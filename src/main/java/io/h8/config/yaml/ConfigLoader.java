package io.h8.config.yaml;

import com.typesafe.config.*;

import java.io.File;
import java.net.URL;
import java.util.Objects;

/**
 * Configurable, instance-based alternative to {@link ConfigFactory}.
 *
 * <p>All settings — YAML factory, parse options, resolve options, and class loader — are fixed at
 * construction time via {@link Builder}. Methods take only the resource argument; there are no
 * options overloads.
 *
 * <pre>{@code
 * ConfigLoader loader = ConfigLoader.builder()
 *         .yamlMaxDepth(64)
 *         .parseOptions(ConfigParseOptions.defaults().setAllowMissing(false))
 *         .classLoader(myLoader)
 *         .build();
 *
 * Config cfg = loader.load();
 * Config cfg = loader.parseFile(new File("app.yaml"));
 * }</pre>
 *
 * <p>Use {@link #DEFAULT} for the standard configuration equivalent to {@link
 * ConfigFactory#load()}.
 */
public final class ConfigLoader {

    /** Default loader: YAML 1.2 core schema, typesafe-config defaults. */
    public static final ConfigLoader DEFAULT = builder().build();

    private final YamlConfigFactory yamlFactory;
    private final ConfigParseOptions parseOptions;
    private final ConfigResolveOptions resolveOptions;
    private final ClassLoader classLoader;

    private ConfigLoader(Builder b) {
        yamlFactory = b.yamlFactory;
        resolveOptions = b.resolveOptions;
        classLoader = b.classLoader;
        ConfigParseOptions opts = b.parseOptions;
        if (classLoader != null) opts = opts.setClassLoader(classLoader);
        if (!(opts.getIncluder() instanceof YamlConfigIncluder))
            opts = opts.prependIncluder(new YamlConfigIncluder(yamlFactory));
        parseOptions = opts;
    }

    /**
     * Returns a new {@link Builder} with all defaults.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    // ── load ─────────────────────────────────────────────────────────────────

    /**
     * Loads the standard application config ({@code application.conf} / system properties /
     * reference configs), with YAML includes resolved.
     *
     * @return resolved {@link Config}
     */
    public Config load() {
        return com.typesafe.config.ConfigFactory.load(parseOptions, resolveOptions);
    }

    /**
     * Loads a named resource basename (e.g. {@code "application"}), with YAML includes resolved.
     * typesafe-config probes {@code .conf}, {@code .json}, and {@code .properties} — not {@code
     * .yaml}. Use {@link #parseResources(String)} to load a YAML file directly.
     *
     * @param basename resource basename without extension
     * @return resolved {@link Config}
     */
    public Config load(String basename) {
        return com.typesafe.config.ConfigFactory.load(basename, parseOptions, resolveOptions);
    }

    // ── parse ─────────────────────────────────────────────────────────────────

    /**
     * Parses a file. Files with a {@code .yaml} or {@code .yml} extension are parsed with the
     * configured {@link YamlConfigFactory}; everything else is parsed as HOCON/JSON.
     *
     * @param file file to parse
     * @return parsed (unresolved) {@link Config}
     */
    public Config parseFile(File file) {
        if (Util.isYaml(file.getName()))
            return YamlConfigIncluder.mergeDocuments(
                            yamlFactory.parseFile(file),
                            ConfigOriginFactory.newFile(file.getPath()))
                    .toConfig();
        return com.typesafe.config.ConfigFactory.parseFile(file, parseOptions);
    }

    /**
     * Parses a URL. URLs whose path ends with {@code .yaml} or {@code .yml} are parsed with the
     * configured {@link YamlConfigFactory}; everything else is parsed as HOCON/JSON.
     *
     * @param url URL to parse
     * @return parsed (unresolved) {@link Config}
     */
    public Config parseURL(URL url) {
        if (Util.isYaml(url))
            return YamlConfigIncluder.mergeDocuments(
                            yamlFactory.parseURL(url), ConfigOriginFactory.newURL(url))
                    .toConfig();
        return com.typesafe.config.ConfigFactory.parseURL(url, parseOptions);
    }

    /**
     * Parses a classpath resource. Resources with a {@code .yaml} or {@code .yml} extension are
     * parsed with the configured {@link YamlConfigFactory} using the configured class loader;
     * everything else is parsed as HOCON/JSON.
     *
     * @param resource classpath resource path
     * @return parsed (unresolved) {@link Config}
     */
    public Config parseResources(String resource) {
        if (Util.isYaml(resource))
            return YamlConfigIncluder.mergeDocuments(
                            yamlFactory.parseResources(effectiveLoader(), resource),
                            ConfigOriginFactory.newSimple(resource))
                    .toConfig();
        return com.typesafe.config.ConfigFactory.parseResources(resource, parseOptions);
    }

    /**
     * Parses a classpath resource located relative to {@code klass}. Resources with a {@code .yaml}
     * or {@code .yml} extension are parsed with the configured {@link YamlConfigFactory};
     * everything else is parsed as HOCON/JSON.
     *
     * @param klass class used for resource lookup
     * @param resource classpath resource path
     * @return parsed (unresolved) {@link Config}
     */
    public Config parseResources(Class<?> klass, String resource) {
        if (Util.isYaml(resource))
            return YamlConfigIncluder.mergeDocuments(
                            yamlFactory.parseResources(klass, resource),
                            ConfigOriginFactory.newSimple(resource))
                    .toConfig();
        return com.typesafe.config.ConfigFactory.parseResources(klass, resource, parseOptions);
    }

    /**
     * Parses a HOCON string. {@code include} directives pointing at {@code .yaml} / {@code .yml}
     * files are resolved with the configured {@link YamlConfigFactory}.
     *
     * @param s HOCON string
     * @return parsed (unresolved) {@link Config}
     */
    public Config parseString(String s) {
        return com.typesafe.config.ConfigFactory.parseString(s, parseOptions);
    }

    private ClassLoader effectiveLoader() {
        return classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    /** Builder for {@link ConfigLoader}. All fields have sensible defaults. */
    public static final class Builder {

        private YamlConfigFactory yamlFactory = YamlConfigFactory.DEFAULT;
        private ConfigParseOptions parseOptions = ConfigParseOptions.defaults();
        private ConfigResolveOptions resolveOptions = ConfigResolveOptions.defaults();
        private ClassLoader classLoader = null;

        private Builder() {}

        /**
         * Sets the YAML factory used for direct YAML parsing and {@code include} resolution.
         *
         * @param factory the factory to use; must not be {@code null}
         * @return this builder
         */
        public Builder yamlFactory(YamlConfigFactory factory) {
            yamlFactory = Objects.requireNonNull(factory, "yamlFactory");
            return this;
        }

        /**
         * Convenience shortcut: creates a {@link YamlConfigFactory} with the given maximum nesting
         * depth and uses it as the YAML factory.
         *
         * @param maxDepth maximum allowed YAML nesting depth (must be &gt; 0)
         * @return this builder
         * @throws IllegalArgumentException if {@code maxDepth} is not positive
         */
        public Builder yamlMaxDepth(int maxDepth) {
            yamlFactory = new YamlConfigFactory(maxDepth);
            return this;
        }

        /**
         * Sets the base {@link ConfigParseOptions} used for HOCON/JSON parsing and {@code include}
         * resolution. A {@link YamlConfigIncluder} will be prepended automatically unless one is
         * already present.
         *
         * @param opts parse options; must not be {@code null}
         * @return this builder
         */
        public Builder parseOptions(ConfigParseOptions opts) {
            parseOptions = Objects.requireNonNull(opts, "parseOptions");
            return this;
        }

        /**
         * Sets the {@link ConfigResolveOptions} used when resolving substitutions.
         *
         * @param opts resolve options; must not be {@code null}
         * @return this builder
         */
        public Builder resolveOptions(ConfigResolveOptions opts) {
            resolveOptions = Objects.requireNonNull(opts, "resolveOptions");
            return this;
        }

        /**
         * Sets the class loader used for classpath resource lookups. When not set, the calling
         * thread's context class loader is used at the time of each call.
         *
         * @param loader the class loader to use, or {@code null} to use the context class loader
         * @return this builder
         */
        public Builder classLoader(ClassLoader loader) {
            classLoader = loader;
            return this;
        }

        /**
         * Builds the {@link ConfigLoader}.
         *
         * @return a new {@link ConfigLoader}
         */
        public ConfigLoader build() {
            return new ConfigLoader(this);
        }
    }
}
