package io.h8.config.yaml;

import com.typesafe.config.*;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * {@link ConfigIncluder} that handles YAML includes (identified by {@code .yaml} / {@code .yml}
 * extension) and delegates everything else to the chained fallback includer.
 *
 * <p>Multi-document streams are merged with the <em>first</em> document taking priority:
 * <ul>
 *   <li>{@code null} documents are silently skipped.</li>
 *   <li>Mapping documents are merged; earlier documents win on key conflicts.</li>
 *   <li>A sequence or scalar document causes {@link ConfigException.Parse}.</li>
 * </ul>
 * An empty stream or a stream consisting entirely of {@code null} documents produces an empty
 * {@link ConfigObject} (the include contributes nothing).
 *
 * <p>Typical usage — attach to a {@link ConfigParseOptions} before parsing a HOCON file that
 * contains {@code include} directives pointing at YAML resources:
 * <pre>{@code
 * ConfigParseOptions opts = ConfigParseOptions.defaults()
 *         .setIncluder(new YamlConfigIncluder());
 * Config cfg = ConfigFactory.parseFile(configFile, opts);
 * }</pre>
 */
public final class YamlConfigIncluder
        implements ConfigIncluder, ConfigIncluderFile, ConfigIncluderClasspath, ConfigIncluderURL {

    private final ConfigIncluder fallback;

    /** Creates a new includer with no fallback. */
    public YamlConfigIncluder() {
        this(null);
    }

    private YamlConfigIncluder(ConfigIncluder fallback) {
        this.fallback = fallback;
    }

    /**
     * Returns a copy of this includer with {@code fallback} appended to the end of the chain.
     *
     * @param fallback the includer to try when this one does not handle an include
     * @return a new {@link YamlConfigIncluder} instance with the updated fallback chain
     */
    @Override
    public ConfigIncluder withFallback(ConfigIncluder fallback) {
        if (this.fallback == fallback) return this;
        if (this.fallback == null) return new YamlConfigIncluder(fallback);
        return new YamlConfigIncluder(this.fallback.withFallback(fallback));
    }

    /**
     * Includes a resource by name. If the name has a {@code .yaml} or {@code .yml} extension the
     * resource is looked up first on the classpath, then on the file system. Otherwise the call is
     * forwarded to the fallback includer.
     *
     * @param context the include context provided by the parser
     * @param what    the resource name as it appeared after {@code include}
     * @return the included {@link ConfigObject}, never {@code null}
     * @throws ConfigException.IO    if the resource is required and cannot be found
     * @throws ConfigException.Parse if the YAML content is invalid for inclusion
     */
    @Override
    public ConfigObject include(ConfigIncludeContext context, String what) {
        if (!isYaml(what)) return delegateInclude(context, what);

        ClassLoader loader = classLoader(context);
        URL url = loader.getResource(what);
        if (url != null) return parseAndMerge(url, ConfigOriginFactory.newURL(url));

        File file = new File(what);
        if (file.isFile()) return parseAndMerge(file, ConfigOriginFactory.newFile(file.getPath()));

        if (context.parseOptions().getAllowMissing()) return empty();
        throw new ConfigException.IO(ConfigOriginFactory.newSimple(what),
                "YAML resource not found: " + what);
    }

    /**
     * Includes a {@code .yaml} / {@code .yml} file. If the file has a different extension the call
     * is forwarded to the fallback includer.
     *
     * @param context the include context provided by the parser
     * @param file    the file to include
     * @return the included {@link ConfigObject}, never {@code null}
     * @throws ConfigException.IO    if the file is required and does not exist
     * @throws ConfigException.Parse if the YAML content is invalid for inclusion
     */
    @Override
    public ConfigObject includeFile(ConfigIncludeContext context, File file) {
        if (!isYaml(file.getName())) return delegateFile(context, file);
        if (!file.isFile()) {
            if (context.parseOptions().getAllowMissing()) return empty();
            throw new ConfigException.IO(ConfigOriginFactory.newFile(file.getPath()),
                    "YAML file not found: " + file);
        }
        return parseAndMerge(file, ConfigOriginFactory.newFile(file.getPath()));
    }

    /**
     * Includes a classpath resource with a {@code .yaml} / {@code .yml} extension. If the resource
     * has a different extension the call is forwarded to the fallback includer.
     *
     * @param context  the include context provided by the parser
     * @param resource the classpath resource path
     * @return the included {@link ConfigObject}, never {@code null}
     * @throws ConfigException.IO    if the resource is required and cannot be found
     * @throws ConfigException.Parse if the YAML content is invalid for inclusion
     */
    @Override
    public ConfigObject includeResources(ConfigIncludeContext context, String resource) {
        if (!isYaml(resource)) return delegateResources(context, resource);
        ClassLoader loader = classLoader(context);
        URL url = loader.getResource(resource);
        if (url == null) {
            if (context.parseOptions().getAllowMissing()) return empty();
            throw new ConfigException.IO(ConfigOriginFactory.newSimple(resource),
                    "YAML classpath resource not found: " + resource);
        }
        return parseAndMerge(url, ConfigOriginFactory.newURL(url));
    }

    /**
     * Includes a YAML resource from a URL. If the URL path does not have a {@code .yaml} or
     * {@code .yml} extension the call is forwarded to the fallback includer.
     *
     * @param context the include context provided by the parser
     * @param url     the URL to include
     * @return the included {@link ConfigObject}, never {@code null}
     * @throws ConfigException.IO    if the URL cannot be opened
     * @throws ConfigException.Parse if the YAML content is invalid for inclusion
     */
    @Override
    public ConfigObject includeURL(ConfigIncludeContext context, URL url) {
        if (!isYaml(urlPath(url))) return delegateURL(context, url);
        return parseAndMerge(url, ConfigOriginFactory.newURL(url));
    }

    // ── internal ─────────────────────────────────────────────────────────────

    private static ConfigObject parseAndMerge(File file, ConfigOrigin origin) {
        return mergeDocuments(YamlConfigFactory.DEFAULT.parseFile(file), origin);
    }

    private static ConfigObject parseAndMerge(URL url, ConfigOrigin origin) {
        return mergeDocuments(YamlConfigFactory.DEFAULT.parseURL(url), origin);
    }

    static ConfigObject mergeDocuments(List<ConfigValue> docs, ConfigOrigin origin) {
        ConfigObject result = empty();
        for (ConfigValue doc : docs) {
            if (doc.valueType() == ConfigValueType.NULL) continue;
            if (doc.valueType() != ConfigValueType.OBJECT)
                throw new ConfigException.Parse(origin,
                        "YAML include must be a mapping or null, got: " + doc.valueType());
            result = result.withFallback((ConfigObject) doc);
        }
        return result;
    }

    private ConfigObject delegateInclude(ConfigIncludeContext context, String what) {
        return fallback != null ? fallback.include(context, what) : empty();
    }

    private ConfigObject delegateFile(ConfigIncludeContext context, File file) {
        return fallback instanceof ConfigIncluderFile
                ? ((ConfigIncluderFile) fallback).includeFile(context, file)
                : empty();
    }

    private ConfigObject delegateResources(ConfigIncludeContext context, String resource) {
        return fallback instanceof ConfigIncluderClasspath
                ? ((ConfigIncluderClasspath) fallback).includeResources(context, resource)
                : empty();
    }

    private ConfigObject delegateURL(ConfigIncludeContext context, URL url) {
        return fallback instanceof ConfigIncluderURL
                ? ((ConfigIncluderURL) fallback).includeURL(context, url)
                : empty();
    }

    private static boolean isYaml(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml");
    }

    private static String urlPath(URL url) {
        String path = url.getPath();
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

    private static ClassLoader classLoader(ConfigIncludeContext context) {
        ClassLoader loader = context.parseOptions().getClassLoader();
        return loader != null ? loader : Thread.currentThread().getContextClassLoader();
    }

    private static ConfigObject empty() {
        return ConfigFactory.empty().root();
    }
}
