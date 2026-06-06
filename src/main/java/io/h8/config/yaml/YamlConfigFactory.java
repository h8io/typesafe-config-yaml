package io.h8.config.yaml;

import com.typesafe.config.*;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.scanner.StreamReader;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for creating {@link ConfigValue} instances from YAML sources.
 *
 * <p>All {@code parse*} methods return {@code List<ConfigValue>}, one element per YAML
 * document in the stream.  An empty stream produces an empty list.
 *
 * <p>To obtain a {@link Config} from a single-document mapping file:
 * <pre>{@code
 * Config cfg = ((ConfigObject) YamlConfigFactory.parseFile(file).get(0)).toConfig();
 * }</pre>
 */
public final class YamlConfigFactory {

    private static final LoadSettings SETTINGS = LoadSettings.builder().build();
    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private YamlConfigFactory() {
    }

    public static List<ConfigValue> parseString(String yaml) {
        return parseAll(new StreamReader(SETTINGS, yaml), "<string>");
    }

    public static List<ConfigValue> parseFile(File file) {
        return parseFile(file, DEFAULT_CHARSET);
    }

    public static List<ConfigValue> parseFile(File file, Charset charset) {
        ConfigOrigin origin = ConfigOriginFactory.newFile(file.getPath());
        try (InputStream in = Files.newInputStream(file.toPath());
             Reader reader = new InputStreamReader(in, charset)) {
            return parseAll(new StreamReader(SETTINGS, reader), file.getPath());
        } catch (IOException e) {
            throw new ConfigException.IO(origin, e.getMessage(), e);
        }
    }

    public static List<ConfigValue> parseURL(URL url) {
        ConfigOrigin origin = ConfigOriginFactory.newURL(url);
        try (InputStream in = url.openStream();
             Reader reader = new InputStreamReader(in, DEFAULT_CHARSET)) {
            return parseAll(new StreamReader(SETTINGS, reader), url.toString());
        } catch (IOException e) {
            throw new ConfigException.IO(origin, e.getMessage(), e);
        }
    }

    public static List<ConfigValue> parseResources(String resource) {
        return parseURL(requireResource(Thread.currentThread().getContextClassLoader(), resource));
    }

    public static List<ConfigValue> parseResources(ClassLoader loader, String resource) {
        return parseURL(requireResource(loader, resource));
    }

    public static List<ConfigValue> parseResources(Class<?> klass, String resource) {
        URL url = klass.getResource(resource);
        if (url == null)
            throw new ConfigException.IO(ConfigOriginFactory.newSimple(resource),
                    "resource not found on classpath: " + resource);
        return parseURL(url);
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private static List<ConfigValue> parseAll(StreamReader stream, String originDesc) {
        ConfigOrigin origin = ConfigOriginFactory.newSimple(originDesc);
        try {
            Composer composer = new Composer(SETTINGS, new ParserImpl(SETTINGS, stream));
            List<ConfigValue> result = new ArrayList<>();
            while (composer.hasNext()) {
                result.add(YamlNodeToConfigValue.DEFAULT.convert(composer.next()));
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
