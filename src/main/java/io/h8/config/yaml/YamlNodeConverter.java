package io.h8.config.yaml;

import com.typesafe.config.*;

import org.snakeyaml.engine.v2.nodes.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Converts a snakeyaml-engine {@link Node} tree into a typesafe-config {@link ConfigValue}.
 *
 * <p>Mapping nodes become {@link ConfigObject}, sequence nodes become {@link ConfigList}, and
 * scalar nodes become the closest Java primitive (according to the YAML 1.2 core schema).
 *
 * <p>When constructed with {@code stringsOnly = true}, all scalar values are kept as raw strings
 * without type coercion. The only exception is an explicit YAML {@code null} tag — those still
 * produce a Java {@code null} so that a literal {@code "null"} string is not mistaken for a missing
 * value.
 */
public final class YamlNodeConverter implements Function<Node, ConfigValue> {
    static final int DEFAULT_MAX_DEPTH = Integer.MAX_VALUE;
    static final YamlNodeConverter DEFAULT = new YamlNodeConverter(DEFAULT_MAX_DEPTH, false);

    private static final ConfigOrigin DEFAULT_ORIGIN = ConfigOriginFactory.newSimple("yaml");

    private final int maxDepth;
    private final boolean stringsOnly;

    /**
     * Creates a converter that rejects YAML documents deeper than {@code maxDepth} levels of
     * nesting. Numeric and boolean scalars are coerced to their Java types.
     *
     * @param maxDepth maximum allowed nesting depth (must be &gt; 0)
     * @throws IllegalArgumentException if {@code maxDepth} is not positive
     */
    public YamlNodeConverter(int maxDepth) {
        this(maxDepth, false);
    }

    /**
     * Creates a converter with an explicit depth limit and an optional strings-only mode.
     *
     * <p>When {@code stringsOnly} is {@code true} all scalars are returned as raw strings; only an
     * explicit YAML {@code null} tag still produces Java {@code null}.
     *
     * @param maxDepth maximum allowed nesting depth (must be &gt; 0)
     * @param stringsOnly {@code true} to suppress numeric/boolean coercion
     * @throws IllegalArgumentException if {@code maxDepth} is not positive
     */
    public YamlNodeConverter(int maxDepth, boolean stringsOnly) {
        if (maxDepth < 1) throw new IllegalArgumentException("maxDepth must be positive");
        this.maxDepth = maxDepth;
        this.stringsOnly = stringsOnly;
    }

    /**
     * Converts a YAML node into a {@link ConfigValue} using a generic {@code "yaml"} origin.
     *
     * @param node the root node of a YAML document
     * @return the corresponding {@link ConfigValue}
     * @throws ConfigException.Parse if the node exceeds the configured depth limit, contains a
     *     mapping with a non-scalar key, or contains a numeric scalar that overflows its Java type
     */
    @Override
    public ConfigValue apply(Node node) {
        return apply(node, DEFAULT_ORIGIN);
    }

    /**
     * Converts a YAML node into a {@link ConfigValue}, reporting errors relative to {@code
     * fileOrigin} (e.g. the file path or URL of the YAML source).
     *
     * @param node the root node of a YAML document
     * @param fileOrigin the origin of the source file, used as the base for error messages
     * @return the corresponding {@link ConfigValue}
     * @throws ConfigException.Parse if the node exceeds the configured depth limit, contains a
     *     mapping with a non-scalar key, or contains a numeric scalar that overflows its Java type
     */
    public ConfigValue apply(Node node, ConfigOrigin fileOrigin) {
        return convert(node, 1, sourceName(fileOrigin));
    }

    private ConfigValue convert(Node node, int depth, String source) {
        if (depth > maxDepth)
            throw new ConfigException.Parse(
                    origin(node, source), "Exceeded maximum YAML document depth: " + maxDepth);

        if (node instanceof MappingNode) return convertObject((MappingNode) node, depth, source);
        if (node instanceof SequenceNode) return convertList((SequenceNode) node, depth, source);
        if (node instanceof ScalarNode) return convertScalar((ScalarNode) node, source);

        throw new ConfigException.Parse(
                origin(node, source), "Unexpected YAML node type: " + node.getClass().getName());
    }

    private ConfigObject convertObject(MappingNode node, int depth, String source) {
        Map<String, ConfigValue> values = new LinkedHashMap<>();
        for (NodeTuple tuple : node.getValue()) {
            Node keyNode = tuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode))
                throw new ConfigException.Parse(
                        origin(keyNode, source), "YAML mapping key must be a scalar");
            String key = ((ScalarNode) keyNode).getValue();
            ConfigValue incoming = convert(tuple.getValueNode(), depth + 1, source);
            values.compute(
                    key,
                    (k, existing) -> existing == null ? incoming : mergeValues(existing, incoming));
        }
        return ConfigValueFactory.fromMap(values, origin(node, source).description());
    }

    private static ConfigValue mergeValues(ConfigValue existing, ConfigValue incoming) {
        if (existing instanceof ConfigObject && incoming instanceof ConfigObject)
            return ((ConfigObject) incoming).withFallback(existing);
        return incoming;
    }

    private ConfigList convertList(SequenceNode node, int depth, String source) {
        List<ConfigValue> values = new ArrayList<>();
        for (Node item : node.getValue()) {
            values.add(convert(item, depth + 1, source));
        }
        return ConfigValueFactory.fromIterable(values, origin(node, source).description());
    }

    private ConfigValue convertScalar(ScalarNode node, String source) {
        try {
            return ConfigValueFactory.fromAnyRef(
                    scalarValue(node), origin(node, source).description());
        } catch (NumberFormatException e) {
            throw new ConfigException.Parse(
                    origin(node, source), "Invalid numeric value '" + node.getValue() + "'", e);
        }
    }

    private Object scalarValue(ScalarNode node) {
        String tag = node.getTag().getValue();
        String value = node.getValue();
        if (stringsOnly) {
            return "tag:yaml.org,2002:null".equals(tag) ? null : value;
        }
        switch (tag) {
            case "tag:yaml.org,2002:null":
                return null;
            case "tag:yaml.org,2002:bool":
                return "true".equalsIgnoreCase(value);
            case "tag:yaml.org,2002:int":
                return parseLong(value);
            case "tag:yaml.org,2002:float":
                return parseYamlFloat(value);
            default:
                return value;
        }
    }

    private static long parseLong(String value) {
        if (value.startsWith("0x") || value.startsWith("0X"))
            return Long.parseLong(value.substring(2), 16);
        if (value.startsWith("0o") || value.startsWith("0O"))
            return Long.parseLong(value.substring(2), 8);
        return Long.parseLong(value);
    }

    private static double parseYamlFloat(String value) {
        switch (value) {
            case ".inf":
                return Double.POSITIVE_INFINITY;
            case "-.inf":
                return Double.NEGATIVE_INFINITY;
            case ".nan":
                return Double.NaN;
            default:
                return Double.parseDouble(value);
        }
    }

    private static String sourceName(ConfigOrigin origin) {
        if (origin.filename() != null) return origin.filename();
        if (origin.url() != null) return origin.url().toString();
        if (origin.resource() != null) return origin.resource();
        return origin.description();
    }

    private static ConfigOrigin origin(Node node, String source) {
        return node.getStartMark()
                .map(
                        mark ->
                                ConfigOriginFactory.newSimple(
                                        source
                                                + ", line "
                                                + (mark.getLine() + 1)
                                                + ", column "
                                                + (mark.getColumn() + 1)))
                .orElse(ConfigOriginFactory.newSimple(source));
    }
}
