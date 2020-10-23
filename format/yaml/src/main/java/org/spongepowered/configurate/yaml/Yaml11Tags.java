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

import org.spongepowered.configurate.ConfigurationNode;

import java.time.ZonedDateTime;
import java.util.regex.Pattern;

/**
 * Standard types defined on the <a href="https://yaml.org/type/">yaml.org
 * tag repository</a>.
 */
public final class Yaml11Tags {

    private Yaml11Tags() {
    }

    private static String yamlOrg(final String specific) {
        return "tag:yaml.org,2002:" + specific;
    }

    /**
     * A binary data tag.
     *
     * @see <a href="https://yaml.org/type/binary.html">tag:yaml.org,2002:binary</a>
     */
    public static final Tag BINARY = Tag.builder()
            .uri(yamlOrg("binary"))
            .nativeType(byte[].class)
            .targetPattern(Pattern.compile("base64 TODO"))
            .build();

    /**
     * A boolean value.
     *
     * @implNote Canonically, these are y|n in YAML 1.1, but because YAML 1.2
     *     will only support true|false, we will treat those as the default
     *     output format.
     * @see <a href="https://yaml.org/type/bool.html">tag:yaml.org,2002:bool</a>
     */
    public static final Tag BOOL = Tag.builder()
            .uri(yamlOrg("bool"))
            .nativeType(Boolean.class)
            .targetPattern(Pattern.compile("y|Y|yes|Yes|YES|n|N|no|No|NO"
                    + "|true|True|TRUE|false|False|FALSE"
                    + "|on|On|ON|off|Off|OFF"))
            .build();

    /**
     * A floating-point number.
     *
     * @see <a href="https://yaml.org/type/float.html">tag:yaml.org,2002:float</a>
     */
    public static final Tag FLOAT = Tag.builder()
            .uri(yamlOrg("float"))
            .nativeType(Double.class)
            .targetPattern(Pattern.compile("[-+]?([0-9][0-9_]*)?\\.[0-9.]*([eE][-+][0-9]+)?" // base 10
                    + "|[-+]?[0-9][0-9_]*(:[0-5]?[0-9])+\\.[0-9]*" // base 60
                    + "|[-+]?\\.(inf|Inf|INF)" // infinity
                    + "|\\.(nan|NaN|NAN)")) // not a number
            .build();

    /**
     * An integer.
     *
     * @see <a href="https://yaml.org/type/int.html">tag:yaml.org,2002:int</a>
     */
    public static final Tag INT = Tag.builder()
            .uri(yamlOrg("int"))
            .nativeType(Long.class)
            .targetPattern(Pattern.compile("[-+]?0b[0-1_]+" // base 2
                    + "|[-+]?0[0-7_]+" // base 8
                    + "|[-+]?(0|[1-9][0-9_]*)" // base 10
                    + "|[-+]?0x[0-9a-fA-F_]+" // base 16
                    + "|[-+]?[1-9][0-9_]*(:[0-5]?[0-9])+")) // base 60
            .build();

    /**
     * A mapping merge.
     *
     * <p>This will not be supported in Configurate until reference-type nodes
     * are fully implemented.</p>
     *
     * @see <a href="https://yaml.org/type/merge.html">tag:yaml.org,2002:merge</a>
     */
    public static final Tag MERGE = Tag.builder()
            .uri(yamlOrg("merge"))
            .nativeType(ConfigurationNode.class)
            .targetPattern(Pattern.compile("<<"))
            .build();

    /**
     * The value {@code null}.
     *
     * <p>Because Configurate has no distinction between a node with a
     * {@code null} value, and a node that does not exist, this tag will most
     * likely never be encountered in an in-memory representation.</p>
     *
     * @see <a href="https://yaml.org/type/null.html">tag:yaml.org,2002:null</a>
     */
    public static final Tag NULL = Tag.builder()
            .uri(yamlOrg("null"))
            .nativeType(Void.class)
            .targetPattern(Pattern.compile("~"
                    + "|null|Null|NULL"
                    + "|$"))
            .build();

    /**
     * Any string.
     *
     * @see <a href="https://yaml.org/type/str.html">tag:yaml.org,2002:str</a>
     */
    public static final Tag STR = Tag.builder()
            .uri(yamlOrg("str"))
            .nativeType(String.class)
            .targetPattern(Pattern.compile(".+")) // empty scalar is NULL
            .build();

    /**
     * A timestamp, containing date, time, and timezone.
     *
     * @see <a href="https://yaml.org/type/timestamp.html">tag:yaml.org,2002:timestamp</a>
     */
    public static final Tag TIMESTAMP = Tag.builder()
            .uri(yamlOrg("timestamp"))
            .nativeType(ZonedDateTime.class)
            .targetPattern(Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}" // YYYY-MM-DD
                    + "|[0-9]{4}" // YYYY
                    + "-[0-9]{1,2}" // month
                    + "-[0-9]{1,2}" // day
                    + "([Tt]|[ \t]+)[0-9]{1,2}" // hour
                    + ":[0-9]{1,2}" // minute
                    + ":[0-9]{2}" // second
                    + "(\\.[0-9]*)?" // fraction
                    + "(([ \t]*)Z|[-+][0-9]{1,2}(:[0-9]{2})?)?")) // time zone
            .build();

}
