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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class CommentTest implements YamlTest {

    @Test
    void testLoadScalarComment() {
        final CommentedConfigurationNode node = parseString(
                "# Hello world\n"
                + "\"i'm a string\""
        );

        assertEquals("Hello world", node.getComment());
        assertEquals("i'm a string", node.getValue());
    }

    @Test
    void testLoadBlockMappingComment() {
        final CommentedConfigurationNode node = parseString(
                "test:\n"
                    + "    # meow\n"
                    + "    cat: purrs\n"
        );

        assertEquals("purrs", node.getNode("test", "cat").getValue());
        assertEquals("meow", node.getNode("test", "cat").getComment());
    }

    @Test
    void testLoadBlockScalarSequenceComment() {
        final CommentedConfigurationNode test = parseString(
                "- first\n"
                    + "# i matter less\n"
                    + "- second\n"
                    + "- third\n"
                    + "# we skipped one\n"
                    + "- fourth\n"
        );

        assertNull(test.getNode(0).getComment());
        assertEquals("i matter less", test.getNode(1).getComment());
        assertEquals("we skipped one", test.getNode(3).getComment());
    }

    @Test
    @Disabled("This doesn't seem to associate comments with the first map entry properly")
    void testLoadScalarCommentsInBlockMapping() {
        final CommentedConfigurationNode test = parseString(
                "blah:\n"
                        + "# beginning sequence\n"
                        + "- # first on map entry\n"
                        + "  test: hello\n"
                        + "  # on second mapping\n"
                        + "  test2: goodbye\n"
        );

        final CommentedConfigurationNode child = test.getNode("blah", 0);
        assertFalse(child.isVirtual());
        assertEquals("beginning sequence", child.getComment());
        assertEquals("first on map entry", child.getNode("test").getComment());
        assertEquals("on second mapping", child.getNode("test2").getComment());
    }

    // flow collections are a bit trickier
    // we can't really do comments on one line, so these all have to have a line per element

    @Test
    void testLoadCommentInFlowMapping() {
        final CommentedConfigurationNode test = parseString(
                "{\n"
                        + "# hello\n"
                        + "test: value,\n"
                        + "uncommented: thing,\n"
                        + "#hi there\n"
                        + "last: bye\n"
                        + "}\n"
        );

        assertEquals("hello", test.getNode("test").getComment());
        assertNull(test.getNode("uncommented").getComment());
        assertEquals("hi there", test.getNode("last").getComment());
    }

    @Test
    void testLoadCommentInFlowSequence() {
        final CommentedConfigurationNode test = parseString(
                "# on list\n"
                        + "[\n"
                        + "  # first\n"
                        + "  'first entry',\n"
                        + "  # second\n"
                        + "  'second entry'\n"
                        + "]"
        );

        assertEquals("on list", test.getComment());
        assertEquals("first", test.getNode(0).getComment());
        assertEquals("second", test.getNode(1).getComment());
    }

    @Test
    void testLoadMixedStructure() {
        final CommentedConfigurationNode test = parseResource(getClass().getResource("/comments-complex.yml"));

    }

    @Test
    void testWriteScalarCommented() {
        final CommentedConfigurationNode node = CommentedConfigurationNode.root()
                .setValue("test")
                .setComment("i have a comment");

        assertEquals(
                "# i have a comment\n"
                + "test\n",
                dump(node));
    }

    @Test
    void testWriteBlockMappingCommented() {

    }

    @Test
    void testWriteBlockSequenceCommented() {

    }

    @Test
    void testWriteFlowMappingCommented() {

    }

    @Test
    void testWriteFlowSequenceCommented() {

    }

}
