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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.RepresentationHint;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.CommentHandler;
import org.spongepowered.configurate.loader.CommentHandlers;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.util.UnmodifiableCollections;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.BufferedReader;
import java.io.Writer;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

/**
 * A loader for YAML-formatted configurations, using the SnakeYAML library for
 * parsing and generation.
 *
 */
public final class YamlConfigurationLoader extends AbstractConfigurationLoader<CommentedConfigurationNode> {

    /**
     * The identifier for a YAML anchor that can be used to refer to the node
     * this hint is set on.
     */
    public static final RepresentationHint<String> ANCHOR_ID = RepresentationHint.of("anchor-id", String.class);

    /**
     * The YAML scalar style this node should attempt to use.
     *
     * <p>If the chosen scalar style would produce syntactically invalid YAML, a
     * valid one will replace it.</p>
     */
    public static final RepresentationHint<ScalarStyle> SCALAR_STYLE = RepresentationHint.of("scalar-style", ScalarStyle.class);

    /**
     * The YAML node style to use for collection nodes. A {@code null} value
     * will instruct the emitter to fall back to the
     * {@link Builder#nodeStyle()} setting.
     */
    public static final RepresentationHint<NodeStyle> NODE_STYLE = RepresentationHint.of("node-style", NodeStyle.class);

    /**
     * YAML native types from <a href="https://yaml.org/type/">YAML 1.1 Global tags</a>.
     *
     * <p>using SnakeYaml representation: https://bitbucket.org/asomov/snakeyaml/wiki/Documentation#markdown-header-yaml-tags-and-java-types
     */
    private static final Set<Class<?>> NATIVE_TYPES = UnmodifiableCollections.toSet(
            Boolean.class, Integer.class, Long.class, BigInteger.class, Double.class, // numeric
            byte[].class, String.class, Date.class, java.sql.Date.class, Timestamp.class); // complex types

    /**
     * Creates a new {@link YamlConfigurationLoader} builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link YamlConfigurationLoader}.
     */
    public static final class Builder extends AbstractConfigurationLoader.Builder<Builder, YamlConfigurationLoader> {
        private final DumperOptions options = new DumperOptions();
        private @Nullable NodeStyle style;

        Builder() {
            indent(4);
            defaultOptions(o -> o.nativeTypes(NATIVE_TYPES));
        }

        /**
         * Sets the level of indentation the resultant loader should use.
         *
         * @param indent the indent level
         * @return this builder (for chaining)
         */
        public Builder indent(final int indent) {
            this.options.setIndent(indent);
            return this;
        }

        /**
         * Gets the level of indentation to be used by the resultant loader.
         *
         * @return the indent level
         */
        public int indent() {
            return this.options.getIndent();
        }

        /**
         * Sets the node style the built loader should use.
         *
         * <dl><dt>Flow</dt>
         * <dd>the compact, json-like representation.<br>
         * Example: <code>
         *     {value: [list, of, elements], another: value}
         * </code></dd>
         *
         * <dt>Block</dt>
         * <dd>expanded, traditional YAML<br>
         * Example: <code>
         *     value:
         *     - list
         *     - of
         *     - elements
         *     another: value
         * </code></dd>
         * </dl>
         *
         * <p>A {@code null} value will tell the loader to pick a value
         * automatically based on the contents of each non-scalar node.</p>
         *
         * @param style the node style to use
         * @return this builder (for chaining)
         */
        public Builder nodeStyle(final @Nullable NodeStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Gets the node style to be used by the resultant loader.
         *
         * @return the node style
         */
        public @Nullable NodeStyle nodeStyle() {
            return this.style;
        }

        @Override
        public YamlConfigurationLoader build() {
            return new YamlConfigurationLoader(this);
        }
    }

    private final ThreadLocal<Constructor> constructor;
    private final DumperOptions options;
    private final YamlVisitor visitor;
    private final LoaderOptions loader;
    private final Resolver resolver;

    private YamlConfigurationLoader(final Builder builder) {
        super(builder, new CommentHandler[] {CommentHandlers.HASH});
        final DumperOptions opts = builder.options;
        opts.setDefaultFlowStyle(NodeStyle.asSnakeYaml(builder.style));
        this.options = opts;
        this.loader = new LoaderOptions();
        this.resolver = new Resolver();
        this.visitor = new YamlVisitor(this.resolver, this.options);
        this.constructor = ThreadLocal.withInitial(Constructor::new);
    }

    @Override
    protected void loadInternal(final CommentedConfigurationNode node, final BufferedReader reader) throws ParsingException {
        // Match the superclass implementation, except we substitute our own scanner implementation
        final StreamReader stream = new StreamReader(reader);
        final YamlParser parser = new YamlParser(new ConfigurateScanner(stream));
        parser.singleDocumentStream(node);
    }

    @Override
    protected void saveInternal(final ConfigurationNode node, final Writer writer) throws ConfigurateException {
        final Emitter emitter = new Emitter(writer, this.options);
        final YamlVisitor.State state = new YamlVisitor.State(emitter);
        node.visit(this.visitor, state);
    }

    @Override
    public CommentedConfigurationNode createNode(final ConfigurationOptions options) {
        return CommentedConfigurationNode.root(options);
    }

}
