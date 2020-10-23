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

import static io.leangen.geantyref.GenericTypeReflector.erase;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.util.List;
import java.util.Map;

/**
 * A collection of tags that are understood when reading a document.
 */
public final class TagRepository {

    private final List<Tag> tags;
    private final Map<Class<?>, Tag> byErasedType;
    private final Map<String, Tag> byName;

    /**
     * Create a new tag repository.
     *
     * @param tags known tags
     * @return new tag repository
     */
    public static TagRepository of(final List<Tag> tags) {
        return new TagRepository(UnmodifiableCollections.copyOf(tags));
    }

    TagRepository(final List<Tag> tags) {
        this.tags = tags;
        this.byErasedType = UnmodifiableCollections.buildMap(map -> {
            for (final Tag tag : this.tags) {
                map.put(erase(tag.nativeType()), tag);
            }
        });
        this.byName = UnmodifiableCollections.buildMap(map -> {
            for (final Tag tag : this.tags) {
                map.put(tag.uri().toString(), tag);
            }
        });
    }

    /**
     * Determine the implicit tag for a scalar value.
     *
     * @param scalar scalar to test
     * @return the first matching tag
     */
    public @Nullable Tag forInput(final String scalar) {
        for (final Tag tag : this.tags) {
            if (tag.targetPattern().matcher(scalar).matches()) {
                return tag;
            }
        }

        return null;
    }

}
