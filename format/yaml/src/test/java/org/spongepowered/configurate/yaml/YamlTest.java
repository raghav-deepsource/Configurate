package org.spongepowered.configurate.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.yaml.snakeyaml.reader.StreamReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public interface YamlTest {

    default CommentedConfigurationNode parseString(final String input) {
        final YamlParser parser = new YamlParser(new ConfigurateScanner(new StreamReader(input)));
        final CommentedConfigurationNode result = CommentedConfigurationNode.root();
        try {
            parser.singleDocumentStream(result);
        } catch (final IOException ex) {
            fail(ex);
        }
        return result;
    }

    default CommentedConfigurationNode parseResource(final URL url) {
        assertNotNull(url, "Expected resource is missing");
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                final YamlParser parser = new YamlParser(new ConfigurateScanner(new StreamReader(reader)));
                final CommentedConfigurationNode result = CommentedConfigurationNode.root();
                parser.singleDocumentStream(result);
                return result;
            }
        } catch (final IOException ex) {
            fail(ex);
            throw new AssertionError();
        }
    }

    default String dump(final CommentedConfigurationNode input) {
        return dump(input, null);
    }

    default String dump(final CommentedConfigurationNode input, final @Nullable NodeStyle preferredStyle) {
        final StringWriter writer = new StringWriter();
        try {
            YamlConfigurationLoader.builder()
                    .setSink(() -> new BufferedWriter(writer))
                    .setNodeStyle(preferredStyle)
                    .build().save(input);
        } catch (IOException e) {
            fail(e);
        }
        return writer.toString();
    }


}
