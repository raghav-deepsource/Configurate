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
import org.yaml.snakeyaml.DumperOptions;

import java.util.EnumMap;
import java.util.Map;

/**
 * Style that can be used to represent a scalar.
 */
public enum ScalarStyle {

    /**
     * A double-quoted string.
     *
     * <p><pre>"hello world"</pre></p>
     */
    DOUBLE_QUOTED(DumperOptions.ScalarStyle.DOUBLE_QUOTED),

    /**
     * A single-quoted string.
     *
     * <p><pre>'hello world'</pre></p>
     */
    SINGLE_QUOTED(DumperOptions.ScalarStyle.SINGLE_QUOTED),
    UNQUOTED(DumperOptions.ScalarStyle.PLAIN),
    FOLDED(DumperOptions.ScalarStyle.FOLDED),
    LITERAL(DumperOptions.ScalarStyle.LITERAL)
    ;

    private static final Map<DumperOptions.ScalarStyle, ScalarStyle> BY_SNAKE = new EnumMap<>(DumperOptions.ScalarStyle.class);
    private final DumperOptions.ScalarStyle snake;

    ScalarStyle(final DumperOptions.ScalarStyle snake) {
        this.snake = snake;
    }

    static DumperOptions.ScalarStyle asSnakeYaml(final @Nullable ScalarStyle style) {
        return style == null ? DumperOptions.ScalarStyle.PLAIN : style.snake;
    }

    static ScalarStyle fromSnakeYaml(final DumperOptions.ScalarStyle style) {
        return BY_SNAKE.getOrDefault(style, UNQUOTED);
    }

    static {
        for (ScalarStyle style : values()) {
            BY_SNAKE.put(style.snake, style);
        }
    }

}
