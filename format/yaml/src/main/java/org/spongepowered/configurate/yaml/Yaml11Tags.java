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

    // https://yaml.org/type/binary.html
    public static final Tag BINARY = Tag.builder()
            .setUri(yamlOrg("binary"))
            .setNativeType(byte[].class)
            .setTargetPattern(Pattern.compile("base64 TODO"))
            .build();

    // https://yaml.org/type/bool.html
    // Canonically these are y|n in YAML 1.1, but because YAML 1.2 moves to
    // true|false only, we'll just use those
    public static final Tag BOOL = Tag.builder()
            .setUri(yamlOrg("bool"))
            .setNativeType(Boolean.class)
            .setTargetPattern(Pattern.compile("y|Y|yes|Yes|YES|n|N|no|No|NO"
                    + "|true|True|TRUE|false|False|FALSE"
                    + "|on|On|ON|off|Off|OFF"))
            .build();

    // https://yaml.org/type/float.html
    public static final Tag FLOAT = Tag.builder()
            .setUri(yamlOrg("float"))
            .setNativeType(Double.class)
            .setTargetPattern(Pattern.compile("[-+]?([0-9][0-9_]*)?\\.[0-9.]*([eE][-+][0-9]+)?" // base 10
                    + "|[-+]?[0-9][0-9_]*(:[0-5]?[0-9])+\\.[0-9]*" // base 60
                    + "|[-+]?\\.(inf|Inf|INF)" // infinity
                    + "|\\.(nan|NaN|NAN)")) // not a number
            .build();

    // https://yaml.org/type/int.html
    public static final Tag INT = Tag.builder()
            .setUri(yamlOrg("int"))
            .setNativeType(Long.class)
            .setTargetPattern(Pattern.compile("[-+]?0b[0-1_]+" // base 2
                    + "|[-+]?0[0-7_]+" // base 8
                    + "|[-+]?(0|[1-9][0-9_]*)" // base 10
                    + "|[-+]?0x[0-9a-fA-F_]+" // base 16
                    + "|[-+]?[1-9][0-9_]*(:[0-5]?[0-9])+")) // base 60
            .build();

    // https://yaml.org/type/merge.html
    public static final Tag MERGE = Tag.builder()
            .setUri(yamlOrg("merge"))
            .setNativeType(ConfigurationNode.class)
            .setTargetPattern(Pattern.compile("<<"))
            .build();

    // https://yaml.org/type/null.html
    public static final Tag NULL = Tag.builder()
            .setUri(yamlOrg("null"))
            .setNativeType(Void.class)
            .setTargetPattern(Pattern.compile("~"
                    + "|null|Null|NULL"
                    + "|$"))
            .build();

    // https://yaml.org/type/str.html
    public static final Tag STR = Tag.builder()
            .setUri(yamlOrg("str"))
            .setNativeType(String.class)
            .setTargetPattern(Pattern.compile(".+")) // empty scalar is NULL
            .build();

    // https://yaml.org/type/timestamp.html
    public static final Tag TIMESTAMP = Tag.builder()
            .setUri(yamlOrg("timestamp"))
            .setNativeType(ZonedDateTime.class)
            .setTargetPattern(Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}" // YYYY-MM-DD
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
