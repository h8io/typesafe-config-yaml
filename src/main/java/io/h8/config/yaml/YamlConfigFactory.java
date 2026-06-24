package io.h8.config.yaml;

import com.typesafe.config.*;

import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.scanner.StreamReader;
import org.snakeyaml.engine.v2.schema.CoreSchema;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Parses YAML sources into lists of {@link ConfigValue} instances.
 *
 * <p>All {@code parse*} methods return {@code List<ConfigValue>}, one element per YAML document in
 * the stream. An empty stream produces an empty list.
 *
 * <p>Use {@link #DEFAULT} for the standard YAML 1.2 core-schema factory, or build a custom instance
 * via {@link #builder()}:
 *
 * <pre>{@code
 * YamlConfigFactory factory = YamlConfigFactory.builder()
 *         .settings(mySettings)
 *         .maxDepth(10)
 *         .stringsOnly(true)
 *         .build();
 * }</pre>
 *
 * <p>To obtain a {@link Config} from a single-document mapping file:
 *
 * <pre>{@code
 * Config cfg = ((ConfigObject) YamlConfigFactory.DEFAULT.parseFile(file).get(0)).toConfig();
 * }</pre>
 */
public final class YamlConfigFactory {

    static final LoadSettings DEFAULT_SETTINGS =
            LoadSettings.builder().setSchema(new CoreSchema()).build();
    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /** Default factory: YAML 1.2 core schema, no depth limit. */
    public static final YamlConfigFactory DEFAULT =
            new YamlConfigFactory(DEFAULT_SETTINGS, YamlNodeConverter.DEFAULT);

    private final LoadSettings settings;
    private final YamlNodeConverter converter;

    private YamlConfigFactory(LoadSettings settings, YamlNodeConverter converter) {
        this.settings = settings;
        this.converter = converter;
    }

    /**
     * Returns a new builder for constructing a {@link YamlConfigFactory}.
     *
     * @return a fresh {@link Builder} with default settings
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link YamlConfigFactory}.
     *
     * <p>All parameters are optional and fall back to the same defaults as {@link #DEFAULT}: YAML
     * 1.2 core schema, no depth limit, numeric/boolean coercion enabled.
     */
    public static final class Builder {
        private LoadSettings settings = DEFAULT_SETTINGS;
        private int maxDepth = YamlNodeConverter.DEFAULT_MAX_DEPTH;
        private boolean stringsOnly = false;

        private Builder() {}

        /**
         * Overrides the snakeyaml-engine {@link LoadSettings}.
         *
         * @param settings load settings (must not enable recursive keys)
         * @return this builder
         */
        public Builder settings(LoadSettings settings) {
            this.settings = settings;
            return this;
        }

        /**
         * Sets the maximum allowed YAML nesting depth.
         *
         * @param maxDepth maximum depth (must be &gt; 0)
         * @return this builder
         */
        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        /**
         * Controls scalar type coercion.
         *
         * @param stringsOnly {@code true} to keep all scalars as raw strings (except explicit
         *     {@code null} tags)
         * @return this builder
         */
        public Builder stringsOnly(boolean stringsOnly) {
            this.stringsOnly = stringsOnly;
            return this;
        }

        /**
         * Builds and returns the configured {@link YamlConfigFactory}.
         *
         * @return a new {@link YamlConfigFactory}
         * @throws IllegalArgumentException if the configured {@link LoadSettings} enables recursive
         *     keys, or if {@code maxDepth} is not positive
         */
        public YamlConfigFactory build() {
            return new YamlConfigFactory(
                    YamlConfigFactory.validated(settings),
                    new YamlNodeConverter(maxDepth, stringsOnly));
        }
    }

    /**
     * Creates a factory with custom {@link LoadSettings} and no depth limit.
     *
     * @param settings snakeyaml-engine load settings
     * @throws IllegalArgumentException if {@code settings} enables recursive keys
     * @deprecated Use {@link #builder()} instead.
     */
    @Deprecated
    public YamlConfigFactory(LoadSettings settings) {
        this(
                validated(settings),
                new YamlNodeConverter(YamlNodeConverter.DEFAULT_MAX_DEPTH, false));
    }

    /**
     * Creates a factory with custom {@link LoadSettings}, no depth limit, and an optional
     * strings-only mode.
     *
     * @param settings snakeyaml-engine load settings
     * @param stringsOnly {@code true} to suppress numeric/boolean coercion for scalar values
     * @throws IllegalArgumentException if {@code settings} enables recursive keys
     * @deprecated Use {@link #builder()} instead.
     */
    @Deprecated
    public YamlConfigFactory(LoadSettings settings, boolean stringsOnly) {
        this(
                validated(settings),
                new YamlNodeConverter(YamlNodeConverter.DEFAULT_MAX_DEPTH, stringsOnly));
    }

    /**
     * Creates a factory with default {@link LoadSettings} and a maximum nesting depth.
     *
     * @param maxDepth maximum allowed YAML nesting depth (must be &gt; 0)
     * @throws IllegalArgumentException if {@code maxDepth} is not positive
     * @deprecated Use {@link #builder()} instead.
     */
    @Deprecated
    public YamlConfigFactory(int maxDepth) {
        this(DEFAULT_SETTINGS, new YamlNodeConverter(maxDepth, false));
    }

    /**
     * Creates a factory with default {@link LoadSettings}, a maximum nesting depth, and an optional
     * strings-only mode.
     *
     * @param maxDepth maximum allowed YAML nesting depth (must be &gt; 0)
     * @param stringsOnly {@code true} to suppress numeric/boolean coercion for scalar values
     * @throws IllegalArgumentException if {@code maxDepth} is not positive
     * @deprecated Use {@link #builder()} instead.
     */
    @Deprecated
    public YamlConfigFactory(int maxDepth, boolean stringsOnly) {
        this(DEFAULT_SETTINGS, new YamlNodeConverter(maxDepth, stringsOnly));
    }

    /**
     * Creates a factory with custom {@link LoadSettings} and a maximum nesting depth.
     *
     * @param settings snakeyaml-engine load settings
     * @param maxDepth maximum allowed YAML nesting depth (must be &gt; 0)
     * @throws IllegalArgumentException if {@code settings} enables recursive keys, or if {@code
     *     maxDepth} is not positive
     * @deprecated Use {@link #builder()} instead.
     */
    @Deprecated
    public YamlConfigFactory(LoadSettings settings, int maxDepth) {
        this(validated(settings), new YamlNodeConverter(maxDepth, false));
    }

    /**
     * Creates a factory with custom {@link LoadSettings}, a maximum nesting depth, and an optional
     * strings-only mode.
     *
     * @param settings snakeyaml-engine load settings
     * @param maxDepth maximum allowed YAML nesting depth (must be &gt; 0)
     * @param stringsOnly {@code true} to suppress numeric/boolean coercion for scalar values
     * @throws IllegalArgumentException if {@code settings} enables recursive keys, or if {@code
     *     maxDepth} is not positive
     * @deprecated Use {@link #builder()} instead.
     */
    @Deprecated
    public YamlConfigFactory(LoadSettings settings, int maxDepth, boolean stringsOnly) {
        this(validated(settings), new YamlNodeConverter(maxDepth, stringsOnly));
    }

    /**
     * Parses all YAML documents from the given string.
     *
     * @param yaml the YAML text to parse
     * @return one {@link ConfigValue} per document, in stream order; empty if the string contains
     *     no documents
     * @throws ConfigException.Parse if the text is not valid YAML
     */
    public List<ConfigValue> parseString(String yaml) {
        return parseAll(
                new StreamReader(settings, yaml), ConfigOriginFactory.newSimple("<string>"));
    }

    /**
     * Parses all YAML documents from the given file using UTF-8 encoding.
     *
     * @param file the file to read
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO if the file cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseFile(File file) {
        return parseFile(file, DEFAULT_CHARSET);
    }

    /**
     * Parses all YAML documents from the given file.
     *
     * @param file the file to read
     * @param charset the character encoding to use
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO if the file cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseFile(File file, Charset charset) {
        ConfigOrigin origin = ConfigOriginFactory.newFile(file.getPath());
        try (InputStream in = Files.newInputStream(file.toPath());
                Reader reader = new InputStreamReader(in, charset)) {
            return parseAll(new StreamReader(settings, reader), origin);
        } catch (IOException e) {
            throw new ConfigException.IO(origin, e.getMessage(), e);
        }
    }

    /**
     * Parses all YAML documents from the given URL using UTF-8 encoding.
     *
     * @param url the URL to open
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO if the URL cannot be opened
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseURL(URL url) {
        ConfigOrigin origin = ConfigOriginFactory.newURL(url);
        try (InputStream in = url.openStream();
                Reader reader = new InputStreamReader(in, DEFAULT_CHARSET)) {
            return parseAll(new StreamReader(settings, reader), origin);
        } catch (IOException e) {
            throw new ConfigException.IO(origin, e.getMessage(), e);
        }
    }

    /**
     * Parses all YAML documents from a classpath resource located via the calling thread's context
     * class loader.
     *
     * @param resource the resource path (e.g. {@code "application.yaml"})
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO if the resource is not found or cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseResources(String resource) {
        return parseURL(requireResource(Thread.currentThread().getContextClassLoader(), resource));
    }

    /**
     * Parses all YAML documents from a classpath resource located via the given class loader.
     *
     * @param loader the class loader used to locate the resource
     * @param resource the resource path (e.g. {@code "application.yaml"})
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO if the resource is not found or cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseResources(ClassLoader loader, String resource) {
        return parseURL(requireResource(loader, resource));
    }

    /**
     * Parses all YAML documents from a classpath resource located relative to the given class.
     *
     * @param klass the class used to locate the resource
     * @param resource the resource path, resolved as by {@link Class#getResource(String)}
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO if the resource is not found or cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseResources(Class<?> klass, String resource) {
        URL url = klass.getResource(resource);
        if (url == null)
            throw new ConfigException.IO(
                    ConfigOriginFactory.newSimple(resource),
                    "resource not found on classpath: " + resource);
        return parseURL(url);
    }

    private static LoadSettings validated(LoadSettings settings) {
        if (settings.getAllowRecursiveKeys())
            throw new IllegalArgumentException("LoadSettings must not enable allowRecursiveKeys");
        return settings;
    }

    private List<ConfigValue> parseAll(StreamReader stream, ConfigOrigin origin) {
        try {
            Composer composer = new Composer(settings, new ParserImpl(settings, stream));
            return StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(composer, Spliterator.ORDERED),
                            false)
                    .map(node -> converter.apply(node, origin))
                    .collect(Collectors.toUnmodifiableList());
        } catch (YamlEngineException e) {
            throw new ConfigException.Parse(origin, e.getMessage(), e);
        }
    }

    private static URL requireResource(ClassLoader loader, String resource) {
        URL url = loader.getResource(resource);
        if (url == null)
            throw new ConfigException.IO(
                    ConfigOriginFactory.newSimple(resource),
                    "resource not found on classpath: " + resource);
        return url;
    }
}
