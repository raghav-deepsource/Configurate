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

import com.google.auto.value.AutoValue;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * A YAML 1.1/1.2 tag
 *
 * @apiNote Design based on §3.2.1.1 of the YAML 1.1 spec
 */
@AutoValue
public abstract class Tag {

    public static Tag.Builder builder() {
        return new AutoValue_Tag.Builder();
    }

    // TODO: non-scalar tags, look into tagged unions?
    Tag() {}

    /**
     * The canonical tag URI.
     *
     * @return tag uri, with `tag:` schema
     */
    public abstract URI getUri();

    /**
     * The native type that maps to this tag.
     *
     * @return native type for tag
     */
    public abstract Type getNativeType();

    /**
     * Pattern to test scalar values against when resolving this tag.
     *
     * @return match pattern
     * @apiNote See §3.3.2 of YAML 1.1 spec
     */
    public abstract Pattern getTargetPattern();

    /**
     * Whether this tag is a global tag with a full namespace or a local one.
     *
     * @return if this is a global tag
     */
    public final boolean isGlobal() {
        return getUri().getScheme().equals("tag");
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setUri(URI url);

        public final Builder setUri(final String tagUrl) {
            try {
                if (tagUrl.startsWith("!")) {
                    return this.setUri(new URI(tagUrl.substring(1)));
                } else {
                    return this.setUri(new URI(tagUrl));
                }
            } catch (final URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        public abstract Builder setNativeType(Type type);

        public abstract Builder setTargetPattern(Pattern targetPattern);

        public abstract Tag build();

    }

}
