/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spongepowered.configurate.yaml;

import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.CommentedConfigurationNodeIntermediary;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationNodeFactory;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.parser.ParserImpl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class YamlParser extends ParserImpl {

    private final Map<String, ConfigurationNode> aliases = new HashMap<>();

    YamlParser(final ConfigurateScanner reader) {
        super(reader);
    }

    // ignore comments -- currently used to continue accumulating
    // while constructing a mapping key
    private boolean suppressComments;

    private void applyComment(final ConfigurationNode node) {
        if (this.suppressComments) {
            return;
        }
        if (node instanceof CommentedConfigurationNodeIntermediary<?>) {
            ((CommentedConfigurationNodeIntermediary<?>) node).comment(this.scanner().popComments());
        }
    }

    private ConfigurateScanner scanner() {
        return (ConfigurateScanner) this.scanner;
    }

    Event requireEvent(final Event.ID type) throws IOException {
        final Event next = peekEvent();
        if (!next.is(type)) {
            throw new IOException("Expected next event of type" + type + " but was " + next.getEventId());
        }
        return this.getEvent();
    }

    @SuppressWarnings("unchecked")
    <T extends Event> T requireEvent(final Event.ID type, final Class<T> clazz) throws IOException {
        final Event next = peekEvent();
        if (!next.is(type)) {
            throw new IOException("Expected next event of type" + type + " but was " + next.getEventId());
        }
        if (!clazz.isInstance(next)) {
            throw new IOException("Expected event of type " + clazz + " but got a " + next.getClass());
        }

        return (T) this.getEvent();
    }

    public <N extends ConfigurationNode> Stream<N> stream(final ConfigurationNodeFactory<N> factory) throws IOException {
        requireEvent(Event.ID.StreamStart);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<N>() {
            @Override
            public boolean hasNext() {
                return !checkEvent(Event.ID.StreamEnd);
            }

            @Override
            public N next() {
                if (!hasNext()) {
                    throw new IndexOutOfBoundsException();
                }
                try {
                    final N node = factory.createNode();
                    document(node);
                    return node;
                } catch (final IOException e) {
                    throw new RuntimeException(e); // TODO
                }
            }
        }, Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    public void singleDocumentStream(final ConfigurationNode node) throws IOException {
        requireEvent(Event.ID.StreamStart);
        document(node);
        requireEvent(Event.ID.StreamEnd);
    }

    public void document(final ConfigurationNode node) throws IOException {
        requireEvent(Event.ID.DocumentStart);
        this.scanner().setCaptureComments(node instanceof CommentedConfigurationNodeIntermediary<?>);
        try {
            value(node);
        } finally {
            this.aliases.clear();
        }
        requireEvent(Event.ID.DocumentEnd);
    }

    void value(final ConfigurationNode node) throws IOException {
        // We have to capture the comment before we peek ahead
        // peeking ahead will start consuming the next event and its comments
        applyComment(node);

        final Event peeked = peekEvent();
        // extract event metadata
        if (peeked instanceof NodeEvent && !(peeked instanceof AliasEvent)) {
            final String anchor = ((NodeEvent) peeked).getAnchor();
            if (anchor != null) {
                node.hint(YamlConfigurationLoader.ANCHOR_ID, anchor);
                this.aliases.put(anchor, node);
            }
            if (peeked instanceof CollectionStartEvent) {
                node.hint(YamlConfigurationLoader.NODE_STYLE, NodeStyle.fromSnakeYaml(((CollectionStartEvent) peeked).getFlowStyle()));
            }
        }

        // then handle the value
        switch (peeked.getEventId()) {
            case Scalar:
                scalar(node);
                break;
            case MappingStart:
                mapping(node);
                break;
            case SequenceStart:
                sequence(node);
                break;
            case Alias:
                alias(node);
                break;
            default:
                throw new IOException("Unexpected event type " + peekEvent().getEventId());
        }
    }

    void scalar(final ConfigurationNode node) throws IOException {
        final ScalarEvent scalar = requireEvent(Event.ID.Scalar, ScalarEvent.class);
        node.hint(YamlConfigurationLoader.SCALAR_STYLE, ScalarStyle.fromSnakeYaml(scalar.getScalarStyle()));
        node.raw(scalar.getValue()); // TODO:  tags and value types
    }

    void mapping(final ConfigurationNode node) throws IOException {
        requireEvent(Event.ID.MappingStart);

        node.raw(Collections.emptyMap());
        final ConfigurationNode keyHolder = BasicConfigurationNode.root(node.options());
        while (!checkEvent(Event.ID.MappingEnd)) {
            this.suppressComments = true;
            try {
                value(keyHolder);
            } finally {
                this.suppressComments = false;
            }
            final ConfigurationNode child = node.node(keyHolder.raw());
            if (!child.virtual()) { // duplicate keys are forbidden (3.2.1.3)
                throw new IOException("Duplicate key '" + child.key() + "' encountered!");
            }
            value(node.node(child));
        }

        requireEvent(Event.ID.MappingEnd);
    }

    void sequence(final ConfigurationNode node) throws IOException {
        requireEvent(Event.ID.SequenceStart);
        node.raw(Collections.emptyList());

        while (!checkEvent(Event.ID.SequenceEnd)) {
            value(node.appendListNode());
        }

        requireEvent(Event.ID.SequenceEnd);
    }

    void alias(final ConfigurationNode node) throws IOException {
        final AliasEvent event = requireEvent(Event.ID.Alias, AliasEvent.class);
        final ConfigurationNode target = this.aliases.get(event.getAnchor());
        if (target == null) {
            throw new IOException("Unknown anchor '" + event.getAnchor() + "'");
        }
        node.from(target); // TODO: Reference node types
        node.hint(YamlConfigurationLoader.ANCHOR_ID, null); // don't duplicate alias
    }

}
