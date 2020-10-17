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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;

public class YamlParserTest implements YamlTest {

    @Test
    void testEmptyDocument() throws IOException {
        final ConfigurationNode result = parseString("");
        assertTrue(result.isEmpty());
        assertNull(result.getValue());
    }

    @Test
    void testDuplicateKeysForbidden() throws IOException {
        assertTrue(assertThrows(IOException.class, () -> parseString("{duplicated: 1, duplicated: 2}"))
                .getMessage().contains("Duplicate key"));
    }

}
