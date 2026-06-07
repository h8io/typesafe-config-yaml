package io.h8.config.yaml;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;

import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Drop-in replacement for {@link com.typesafe.config.ConfigFactory} that automatically attaches
 * {@link YamlConfigIncluder#DEFAULT} to all parse and load operations.
 *
 * <p>Replacing {@code import com.typesafe.config.ConfigFactory} with {@code import
 * io.h8.config.yaml.ConfigFactory} is sufficient — no other changes needed.
 *
 * @see com.typesafe.config.ConfigFactory
 */
public final class ConfigFactory {

    private ConfigFactory() {}

    private static final ConfigParseOptions DEFAULT_OPTS =
            ConfigParseOptions.defaults().prependIncluder(YamlConfigIncluder.DEFAULT);

    /** Prepends {@link YamlConfigIncluder#DEFAULT} unless one is already present. */
    private static ConfigParseOptions withIncluder(ConfigParseOptions opts) {
        return opts.getIncluder() instanceof YamlConfigIncluder
                ? opts
                : opts.prependIncluder(YamlConfigIncluder.DEFAULT);
    }

    // ── load ─────────────────────────────────────────────────────────────────

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ConfigParseOptions)} with {@link
     * YamlConfigIncluder#DEFAULT} prepended.
     *
     * @return resolved {@link Config}
     */
    public static Config load() {
        return com.typesafe.config.ConfigFactory.load(DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ClassLoader, ConfigParseOptions)}
     * with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param loader class loader used to find resources
     * @return resolved {@link Config}
     */
    public static Config load(ClassLoader loader) {
        return com.typesafe.config.ConfigFactory.load(loader, DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ConfigParseOptions)} with {@link
     * YamlConfigIncluder#DEFAULT} prepended to {@code parseOptions}.
     *
     * @param parseOptions parse options
     * @return resolved {@link Config}
     */
    public static Config load(ConfigParseOptions parseOptions) {
        return com.typesafe.config.ConfigFactory.load(withIncluder(parseOptions));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ClassLoader, ConfigParseOptions)}
     * with {@link YamlConfigIncluder#DEFAULT} prepended to {@code parseOptions}.
     *
     * @param loader class loader used to find resources
     * @param parseOptions parse options
     * @return resolved {@link Config}
     */
    public static Config load(ClassLoader loader, ConfigParseOptions parseOptions) {
        return com.typesafe.config.ConfigFactory.load(loader, withIncluder(parseOptions));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ClassLoader, ConfigParseOptions,
     * ConfigResolveOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param loader class loader used to find resources
     * @param resolveOptions substitution resolution options
     * @return resolved {@link Config}
     */
    public static Config load(ClassLoader loader, ConfigResolveOptions resolveOptions) {
        return com.typesafe.config.ConfigFactory.load(loader, DEFAULT_OPTS, resolveOptions);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ConfigParseOptions,
     * ConfigResolveOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code
     * parseOptions}.
     *
     * @param parseOptions parse options
     * @param resolveOptions substitution resolution options
     * @return resolved {@link Config}
     */
    public static Config load(
            ConfigParseOptions parseOptions, ConfigResolveOptions resolveOptions) {
        return com.typesafe.config.ConfigFactory.load(withIncluder(parseOptions), resolveOptions);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ClassLoader, ConfigParseOptions,
     * ConfigResolveOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code
     * parseOptions}.
     *
     * @param loader class loader used to find resources
     * @param parseOptions parse options
     * @param resolveOptions substitution resolution options
     * @return resolved {@link Config}
     */
    public static Config load(
            ClassLoader loader,
            ConfigParseOptions parseOptions,
            ConfigResolveOptions resolveOptions) {
        return com.typesafe.config.ConfigFactory.load(
                loader, withIncluder(parseOptions), resolveOptions);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(String, ConfigParseOptions,
     * ConfigResolveOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param resourceBasename resource basename (e.g. {@code application})
     * @return resolved {@link Config}
     */
    public static Config load(String resourceBasename) {
        return com.typesafe.config.ConfigFactory.load(
                resourceBasename, DEFAULT_OPTS, ConfigResolveOptions.defaults());
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ClassLoader, String,
     * ConfigParseOptions, ConfigResolveOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param loader class loader used to find resources
     * @param resourceBasename resource basename (e.g. {@code application})
     * @return resolved {@link Config}
     */
    public static Config load(ClassLoader loader, String resourceBasename) {
        return com.typesafe.config.ConfigFactory.load(
                loader, resourceBasename, DEFAULT_OPTS, ConfigResolveOptions.defaults());
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(String, ConfigParseOptions,
     * ConfigResolveOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code
     * parseOptions}.
     *
     * @param resourceBasename resource basename (e.g. {@code application})
     * @param parseOptions parse options
     * @param resolveOptions substitution resolution options
     * @return resolved {@link Config}
     */
    public static Config load(
            String resourceBasename,
            ConfigParseOptions parseOptions,
            ConfigResolveOptions resolveOptions) {
        return com.typesafe.config.ConfigFactory.load(
                resourceBasename, withIncluder(parseOptions), resolveOptions);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#load(ClassLoader, String,
     * ConfigParseOptions, ConfigResolveOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended
     * to {@code parseOptions}.
     *
     * @param loader class loader used to find resources
     * @param resourceBasename resource basename (e.g. {@code application})
     * @param parseOptions parse options
     * @param resolveOptions substitution resolution options
     * @return resolved {@link Config}
     */
    public static Config load(
            ClassLoader loader,
            String resourceBasename,
            ConfigParseOptions parseOptions,
            ConfigResolveOptions resolveOptions) {
        return com.typesafe.config.ConfigFactory.load(
                loader, resourceBasename, withIncluder(parseOptions), resolveOptions);
    }

    /**
     * Delegates directly to {@link com.typesafe.config.ConfigFactory#load(Config)}. No includer is
     * injected — the config is already parsed.
     *
     * @param config already-parsed config to resolve
     * @return resolved {@link Config}
     */
    public static Config load(Config config) {
        return com.typesafe.config.ConfigFactory.load(config);
    }

    /**
     * Delegates directly to {@link com.typesafe.config.ConfigFactory#load(ClassLoader, Config)}. No
     * includer is injected — the config is already parsed.
     *
     * @param loader class loader used for resolution
     * @param config already-parsed config to resolve
     * @return resolved {@link Config}
     */
    public static Config load(ClassLoader loader, Config config) {
        return com.typesafe.config.ConfigFactory.load(loader, config);
    }

    /**
     * Delegates directly to {@link com.typesafe.config.ConfigFactory#load(Config,
     * ConfigResolveOptions)}. No includer is injected — the config is already parsed.
     *
     * @param config already-parsed config to resolve
     * @param resolveOptions substitution resolution options
     * @return resolved {@link Config}
     */
    public static Config load(Config config, ConfigResolveOptions resolveOptions) {
        return com.typesafe.config.ConfigFactory.load(config, resolveOptions);
    }

    /**
     * Delegates directly to {@link com.typesafe.config.ConfigFactory#load(ClassLoader, Config,
     * ConfigResolveOptions)}. No includer is injected — the config is already parsed.
     *
     * @param loader class loader used for resolution
     * @param config already-parsed config to resolve
     * @param resolveOptions substitution resolution options
     * @return resolved {@link Config}
     */
    public static Config load(
            ClassLoader loader, Config config, ConfigResolveOptions resolveOptions) {
        return com.typesafe.config.ConfigFactory.load(loader, config, resolveOptions);
    }

    // ── defaultApplication ───────────────────────────────────────────────────

    /**
     * Equivalent to {@link
     * com.typesafe.config.ConfigFactory#defaultApplication(ConfigParseOptions)} with {@link
     * YamlConfigIncluder#DEFAULT} prepended.
     *
     * @return the application config
     */
    public static Config defaultApplication() {
        return com.typesafe.config.ConfigFactory.defaultApplication(DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link
     * com.typesafe.config.ConfigFactory#defaultApplication(ConfigParseOptions)} with {@link
     * YamlConfigIncluder#DEFAULT} prepended and {@code loader} set via {@link
     * ConfigParseOptions#setClassLoader(ClassLoader)}.
     *
     * @param loader class loader used to find application resources
     * @return the application config
     */
    public static Config defaultApplication(ClassLoader loader) {
        return com.typesafe.config.ConfigFactory.defaultApplication(
                DEFAULT_OPTS.setClassLoader(loader));
    }

    /**
     * Equivalent to {@link
     * com.typesafe.config.ConfigFactory#defaultApplication(ConfigParseOptions)} with {@link
     * YamlConfigIncluder#DEFAULT} prepended to {@code parseOptions}.
     *
     * @param parseOptions parse options
     * @return the application config
     */
    public static Config defaultApplication(ConfigParseOptions parseOptions) {
        return com.typesafe.config.ConfigFactory.defaultApplication(withIncluder(parseOptions));
    }

    // ── defaultReference / defaultOverrides ──────────────────────────────────

    /**
     * Returns the reference config assembled from all {@code reference.conf} resources on the
     * classpath.
     *
     * @return the reference config
     * @see com.typesafe.config.ConfigFactory#defaultReference()
     */
    public static Config defaultReference() {
        return com.typesafe.config.ConfigFactory.defaultReference();
    }

    /**
     * Returns the reference config assembled from all {@code reference.conf} resources found by
     * {@code loader}.
     *
     * @param loader class loader used to find {@code reference.conf} resources
     * @return the reference config
     * @see com.typesafe.config.ConfigFactory#defaultReference(ClassLoader)
     */
    public static Config defaultReference(ClassLoader loader) {
        return com.typesafe.config.ConfigFactory.defaultReference(loader);
    }

    /**
     * Returns the unresolved reference config assembled from all {@code reference.conf} resources
     * on the classpath.
     *
     * @return the unresolved reference config
     * @see com.typesafe.config.ConfigFactory#defaultReferenceUnresolved()
     */
    public static Config defaultReferenceUnresolved() {
        return com.typesafe.config.ConfigFactory.defaultReferenceUnresolved();
    }

    /**
     * Returns the unresolved reference config assembled from all {@code reference.conf} resources
     * found by {@code loader}.
     *
     * @param loader class loader used to find {@code reference.conf} resources
     * @return the unresolved reference config
     * @see com.typesafe.config.ConfigFactory#defaultReferenceUnresolved(ClassLoader)
     */
    public static Config defaultReferenceUnresolved(ClassLoader loader) {
        return com.typesafe.config.ConfigFactory.defaultReferenceUnresolved(loader);
    }

    /**
     * Returns config from system-property overrides and other highest-priority sources.
     *
     * @return the overrides config
     * @see com.typesafe.config.ConfigFactory#defaultOverrides()
     */
    public static Config defaultOverrides() {
        return com.typesafe.config.ConfigFactory.defaultOverrides();
    }

    /**
     * Returns config from system-property overrides and other highest-priority sources, using
     * {@code loader} for any classpath lookups.
     *
     * @param loader class loader for classpath lookups
     * @return the overrides config
     * @see com.typesafe.config.ConfigFactory#defaultOverrides(ClassLoader)
     */
    public static Config defaultOverrides(ClassLoader loader) {
        return com.typesafe.config.ConfigFactory.defaultOverrides(loader);
    }

    // ── invalidateCaches ─────────────────────────────────────────────────────

    /**
     * Clears all cached configs so the next load re-reads from disk and classpath.
     *
     * @see com.typesafe.config.ConfigFactory#invalidateCaches()
     */
    public static void invalidateCaches() {
        com.typesafe.config.ConfigFactory.invalidateCaches();
    }

    // ── empty ────────────────────────────────────────────────────────────────

    /**
     * Returns an empty config with no origin description.
     *
     * @return an empty {@link Config}
     * @see com.typesafe.config.ConfigFactory#empty()
     */
    public static Config empty() {
        return com.typesafe.config.ConfigFactory.empty();
    }

    /**
     * Returns an empty config with the given origin description.
     *
     * @param originDescription origin description used in error messages
     * @return an empty {@link Config}
     * @see com.typesafe.config.ConfigFactory#empty(String)
     */
    public static Config empty(String originDescription) {
        return com.typesafe.config.ConfigFactory.empty(originDescription);
    }

    // ── system / env ─────────────────────────────────────────────────────────

    /**
     * Returns a config built from Java system properties.
     *
     * @return system-properties config
     * @see com.typesafe.config.ConfigFactory#systemProperties()
     */
    public static Config systemProperties() {
        return com.typesafe.config.ConfigFactory.systemProperties();
    }

    /**
     * Returns a config built from environment variables, suitable for use as overrides.
     *
     * @return environment-variable overrides config
     * @see com.typesafe.config.ConfigFactory#systemEnvironmentOverrides()
     */
    public static Config systemEnvironmentOverrides() {
        return com.typesafe.config.ConfigFactory.systemEnvironmentOverrides();
    }

    /**
     * Returns a config built from environment variables.
     *
     * @return environment-variable config
     * @see com.typesafe.config.ConfigFactory#systemEnvironment()
     */
    public static Config systemEnvironment() {
        return com.typesafe.config.ConfigFactory.systemEnvironment();
    }

    // ── parseProperties ───────────────────────────────────────────────────────

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseProperties(Properties,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param properties Java properties to convert
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseProperties(Properties properties, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseProperties(properties, withIncluder(options));
    }

    /**
     * Converts Java properties into a {@link Config}.
     *
     * @param properties Java properties to convert
     * @return parsed {@link Config}
     * @see com.typesafe.config.ConfigFactory#parseProperties(Properties)
     */
    public static Config parseProperties(Properties properties) {
        return com.typesafe.config.ConfigFactory.parseProperties(properties);
    }

    // ── parseReader ───────────────────────────────────────────────────────────

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseReader(Reader,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param reader reader supplying HOCON or JSON content
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseReader(Reader reader, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseReader(reader, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseReader(Reader,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param reader reader supplying HOCON or JSON content
     * @return parsed {@link Config}
     */
    public static Config parseReader(Reader reader) {
        return com.typesafe.config.ConfigFactory.parseReader(reader, DEFAULT_OPTS);
    }

    // ── parseURL ──────────────────────────────────────────────────────────────

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseURL(URL, ConfigParseOptions)}
     * with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param url URL to parse
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseURL(URL url, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseURL(url, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseURL(URL, ConfigParseOptions)}
     * with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param url URL to parse
     * @return parsed {@link Config}
     */
    public static Config parseURL(URL url) {
        return com.typesafe.config.ConfigFactory.parseURL(url, DEFAULT_OPTS);
    }

    // ── parseFile ─────────────────────────────────────────────────────────────

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseFile(File, ConfigParseOptions)}
     * with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param file file to parse
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseFile(File file, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseFile(file, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseFile(File, ConfigParseOptions)}
     * with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param file file to parse
     * @return parsed {@link Config}
     */
    public static Config parseFile(File file) {
        return com.typesafe.config.ConfigFactory.parseFile(file, DEFAULT_OPTS);
    }

    // ── parseFileAnySyntax ────────────────────────────────────────────────────

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseFileAnySyntax(File,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param file base file path (extension is inferred)
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseFileAnySyntax(File file, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseFileAnySyntax(file, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseFileAnySyntax(File,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param file base file path (extension is inferred)
     * @return parsed {@link Config}
     */
    public static Config parseFileAnySyntax(File file) {
        return com.typesafe.config.ConfigFactory.parseFileAnySyntax(file, DEFAULT_OPTS);
    }

    // ── parseResources ────────────────────────────────────────────────────────

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResources(Class, String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param klass class used for classpath-relative resource lookup
     * @param resource classpath resource path
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseResources(
            Class<?> klass, String resource, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseResources(
                klass, resource, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResources(Class, String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param klass class used for classpath-relative resource lookup
     * @param resource classpath resource path
     * @return parsed {@link Config}
     */
    public static Config parseResources(Class<?> klass, String resource) {
        return com.typesafe.config.ConfigFactory.parseResources(klass, resource, DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResourcesAnySyntax(Class, String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param klass class used for classpath-relative resource lookup
     * @param resource classpath resource path (extension is inferred)
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseResourcesAnySyntax(
            Class<?> klass, String resource, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseResourcesAnySyntax(
                klass, resource, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResourcesAnySyntax(Class, String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param klass class used for classpath-relative resource lookup
     * @param resource classpath resource path (extension is inferred)
     * @return parsed {@link Config}
     */
    public static Config parseResourcesAnySyntax(Class<?> klass, String resource) {
        return com.typesafe.config.ConfigFactory.parseResourcesAnySyntax(
                klass, resource, DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResources(ClassLoader, String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param loader class loader used to find the resource
     * @param resource classpath resource path
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseResources(
            ClassLoader loader, String resource, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseResources(
                loader, resource, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResources(ClassLoader, String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param loader class loader used to find the resource
     * @param resource classpath resource path
     * @return parsed {@link Config}
     */
    public static Config parseResources(ClassLoader loader, String resource) {
        return com.typesafe.config.ConfigFactory.parseResources(loader, resource, DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResourcesAnySyntax(ClassLoader,
     * String, ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code
     * options}.
     *
     * @param loader class loader used to find the resource
     * @param resource classpath resource path (extension is inferred)
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseResourcesAnySyntax(
            ClassLoader loader, String resource, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseResourcesAnySyntax(
                loader, resource, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResourcesAnySyntax(ClassLoader,
     * String, ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param loader class loader used to find the resource
     * @param resource classpath resource path (extension is inferred)
     * @return parsed {@link Config}
     */
    public static Config parseResourcesAnySyntax(ClassLoader loader, String resource) {
        return com.typesafe.config.ConfigFactory.parseResourcesAnySyntax(
                loader, resource, DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResources(String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param resource classpath resource path
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseResources(String resource, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseResources(resource, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResources(String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param resource classpath resource path
     * @return parsed {@link Config}
     */
    public static Config parseResources(String resource) {
        return com.typesafe.config.ConfigFactory.parseResources(resource, DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResourcesAnySyntax(String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param resource classpath resource path (extension is inferred)
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseResourcesAnySyntax(String resource, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseResourcesAnySyntax(
                resource, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseResourcesAnySyntax(String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param resource classpath resource path (extension is inferred)
     * @return parsed {@link Config}
     */
    public static Config parseResourcesAnySyntax(String resource) {
        return com.typesafe.config.ConfigFactory.parseResourcesAnySyntax(resource, DEFAULT_OPTS);
    }

    // ── parseApplicationReplacement ───────────────────────────────────────────

    /**
     * Equivalent to {@link
     * com.typesafe.config.ConfigFactory#parseApplicationReplacement(ConfigParseOptions)} with
     * {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @return optional application replacement config
     */
    public static Optional<Config> parseApplicationReplacement() {
        return com.typesafe.config.ConfigFactory.parseApplicationReplacement(DEFAULT_OPTS);
    }

    /**
     * Equivalent to {@link
     * com.typesafe.config.ConfigFactory#parseApplicationReplacement(ConfigParseOptions)} with
     * {@link YamlConfigIncluder#DEFAULT} prepended and {@code loader} set via {@link
     * ConfigParseOptions#setClassLoader(ClassLoader)}.
     *
     * @param loader class loader used to find application resources
     * @return optional application replacement config
     */
    public static Optional<Config> parseApplicationReplacement(ClassLoader loader) {
        return com.typesafe.config.ConfigFactory.parseApplicationReplacement(
                DEFAULT_OPTS.setClassLoader(loader));
    }

    /**
     * Equivalent to {@link
     * com.typesafe.config.ConfigFactory#parseApplicationReplacement(ConfigParseOptions)} with
     * {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param options parse options
     * @return optional application replacement config
     */
    public static Optional<Config> parseApplicationReplacement(ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseApplicationReplacement(withIncluder(options));
    }

    // ── parseString ───────────────────────────────────────────────────────────

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseString(String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended to {@code options}.
     *
     * @param s HOCON or JSON string to parse
     * @param options parse options
     * @return parsed {@link Config}
     */
    public static Config parseString(String s, ConfigParseOptions options) {
        return com.typesafe.config.ConfigFactory.parseString(s, withIncluder(options));
    }

    /**
     * Equivalent to {@link com.typesafe.config.ConfigFactory#parseString(String,
     * ConfigParseOptions)} with {@link YamlConfigIncluder#DEFAULT} prepended.
     *
     * @param s HOCON or JSON string to parse
     * @return parsed {@link Config}
     */
    public static Config parseString(String s) {
        return com.typesafe.config.ConfigFactory.parseString(s, DEFAULT_OPTS);
    }

    // ── parseMap ──────────────────────────────────────────────────────────────

    /**
     * Converts a string-keyed map into a {@link Config} with the given origin description.
     *
     * @param values map of key-value pairs
     * @param originDescription origin description used in error messages
     * @return parsed {@link Config}
     * @see com.typesafe.config.ConfigFactory#parseMap(Map, String)
     */
    public static Config parseMap(Map<String, ?> values, String originDescription) {
        return com.typesafe.config.ConfigFactory.parseMap(values, originDescription);
    }

    /**
     * Converts a string-keyed map into a {@link Config}.
     *
     * @param values map of key-value pairs
     * @return parsed {@link Config}
     * @see com.typesafe.config.ConfigFactory#parseMap(Map)
     */
    public static Config parseMap(Map<String, ?> values) {
        return com.typesafe.config.ConfigFactory.parseMap(values);
    }
}
