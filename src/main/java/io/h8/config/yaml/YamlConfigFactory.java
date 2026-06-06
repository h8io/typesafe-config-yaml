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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Parses YAML sources into lists of {@link ConfigValue} instances.
 *
 * <p>All {@code parse*} methods return {@code List<ConfigValue>}, one element per YAML
 * document in the stream.  An empty stream produces an empty list.
 *
 * <p>Use {@link #DEFAULT} for the standard YAML 1.2 core-schema factory, or construct a
 * custom instance via one of the public constructors to override the {@link LoadSettings}
 * and/or the maximum nesting depth.
 *
 * <p>To obtain a {@link Config} from a single-document mapping file:
 * <pre>{@code
 * Config cfg = ((ConfigObject) YamlConfigFactory.DEFAULT.parseFile(file).get(0)).toConfig();
 * }</pre>
 */
public final class YamlConfigFactory {

    static final LoadSettings DEFAULT_SETTINGS = LoadSettings.builder()
            .setSchema(new CoreSchema())
            .build();
    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * Default factory: YAML 1.2 core schema, no depth limit.
     */
    public static final YamlConfigFactory DEFAULT = new YamlConfigFactory(DEFAULT_SETTINGS);

    private final LoadSettings settings;
    private final YamlNodeToConfigValue converter;

    /**
     * Creates a factory with custom {@link LoadSettings} and no depth limit.
     *
     * @param settings snakeyaml-engine load settings
     */
    public YamlConfigFactory(LoadSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.converter = YamlNodeToConfigValue.DEFAULT;
    }

    /**
     * Creates a factory with default {@link LoadSettings} and a maximum nesting depth.
     *
     * @param maxDepth maximum allowed YAML nesting depth (must be &gt; 0)
     * @throws IllegalArgumentException if {@code maxDepth} is not positive
     */
    public YamlConfigFactory(int maxDepth) {
        this.settings = DEFAULT_SETTINGS;
        this.converter = new YamlNodeToConfigValue(maxDepth);
    }

    /**
     * Creates a factory with custom {@link LoadSettings} and a maximum nesting depth.
     *
     * @param settings snakeyaml-engine load settings
     * @param maxDepth maximum allowed YAML nesting depth (must be &gt; 0)
     * @throws IllegalArgumentException if {@code maxDepth} is not positive
     */
    public YamlConfigFactory(LoadSettings settings, int maxDepth) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.converter = new YamlNodeToConfigValue(maxDepth);
    }

    /**
     * Parses all YAML documents from the given string.
     *
     * @param yaml the YAML text to parse
     * @return one {@link ConfigValue} per document, in stream order; empty if the string
     * contains no documents
     * @throws ConfigException.Parse if the text is not valid YAML
     */
    public List<ConfigValue> parseString(String yaml) {
        return parseAll(new StreamReader(settings, yaml), "<string>");
    }

    /**
     * Parses all YAML documents from the given file using UTF-8 encoding.
     *
     * @param file the file to read
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO    if the file cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseFile(File file) {
        return parseFile(file, DEFAULT_CHARSET);
    }

    /**
     * Parses all YAML documents from the given file.
     *
     * @param file    the file to read
     * @param charset the character encoding to use
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO    if the file cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseFile(File file, Charset charset) {
        ConfigOrigin origin = ConfigOriginFactory.newFile(file.getPath());
        try (InputStream in = Files.newInputStream(file.toPath()); Reader reader = new InputStreamReader(in, charset)) {
            return parseAll(new StreamReader(settings, reader), file.getPath());
        } catch (IOException e) {
            throw new ConfigException.IO(origin, e.getMessage(), e);
        }
    }

    /**
     * Parses all YAML documents from the given URL using UTF-8 encoding.
     *
     * @param url the URL to open
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO    if the URL cannot be opened
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseURL(URL url) {
        ConfigOrigin origin = ConfigOriginFactory.newURL(url);
        try (InputStream in = url.openStream();
             Reader reader = new InputStreamReader(in, DEFAULT_CHARSET)) {
            return parseAll(new StreamReader(settings, reader), url.toString());
        } catch (IOException e) {
            throw new ConfigException.IO(origin, e.getMessage(), e);
        }
    }

    /**
     * Parses all YAML documents from a classpath resource located via the calling
     * thread's context class loader.
     *
     * @param resource the resource path (e.g. {@code "application.yaml"})
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO    if the resource is not found or cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseResources(String resource) {
        return parseURL(requireResource(Thread.currentThread().getContextClassLoader(), resource));
    }

    /**
     * Parses all YAML documents from a classpath resource located via the given class loader.
     *
     * @param loader   the class loader used to locate the resource
     * @param resource the resource path (e.g. {@code "application.yaml"})
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO    if the resource is not found or cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseResources(ClassLoader loader, String resource) {
        return parseURL(requireResource(loader, resource));
    }

    /**
     * Parses all YAML documents from a classpath resource located relative to the given class.
     *
     * @param klass    the class used to locate the resource
     * @param resource the resource path, resolved as by {@link Class#getResource(String)}
     * @return one {@link ConfigValue} per document, in stream order
     * @throws ConfigException.IO    if the resource is not found or cannot be read
     * @throws ConfigException.Parse if the content is not valid YAML
     */
    public List<ConfigValue> parseResources(Class<?> klass, String resource) {
        URL url = klass.getResource(resource);
        if (url == null)
            throw new ConfigException.IO(ConfigOriginFactory.newSimple(resource),
                    "resource not found on classpath: " + resource);
        return parseURL(url);
    }

    private List<ConfigValue> parseAll(StreamReader stream, String originDesc) {
        ConfigOrigin origin = ConfigOriginFactory.newSimple(originDesc);
        try {
            Composer composer = new Composer(settings, new ParserImpl(settings, stream));
            List<ConfigValue> result = new ArrayList<>();
            while (composer.hasNext()) {
                result.add(converter.convert(composer.next()));
            }
            return Collections.unmodifiableList(result);
        } catch (YamlEngineException e) {
            throw new ConfigException.Parse(origin, e.getMessage(), e);
        }
    }

    private static URL requireResource(ClassLoader loader, String resource) {
        URL url = loader.getResource(resource);
        if (url == null)
            throw new ConfigException.IO(ConfigOriginFactory.newSimple(resource),
                    "resource not found on classpath: " + resource);
        return url;
    }
}
