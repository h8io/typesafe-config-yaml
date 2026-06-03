package io.h8.config.yaml;

import com.typesafe.config.*;
import org.snakeyaml.engine.v2.nodes.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class YamlNodeToConfigValue {
    static final int DEFAULT_MAX_DEPTH = 256;
    static final YamlNodeToConfigValue DEFAULT = new YamlNodeToConfigValue(DEFAULT_MAX_DEPTH);

    private static final ConfigOrigin DEFAULT_ORIGIN = ConfigOriginFactory.newSimple("yaml");

    private final int maxDepth;

    public YamlNodeToConfigValue(int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be positive");
        }
        this.maxDepth = maxDepth;
    }

    ConfigValue convert(Node node) {
        if (node == null) {
            return ConfigValueFactory.fromAnyRef(null, DEFAULT_ORIGIN.description());
        }
        return convert(node, 1);
    }

    private ConfigValue convert(Node node, int depth) {
        if (depth > maxDepth)
            throw new ConfigException.Parse(origin(node), "Exceeded maximum YAML document depth: " + maxDepth);

        if (node instanceof MappingNode) return convert((MappingNode) node, depth);
        if (node instanceof SequenceNode) return convert((SequenceNode) node, depth);
        if (node instanceof ScalarNode) return convert((ScalarNode) node);

        throw new ConfigException.Parse(origin(node), "Unexpected YAML node type: " + node.getClass().getName());
    }

    private ConfigObject convert(MappingNode node, int depth) {
        Map<String, ConfigValue> values = new LinkedHashMap<>();
        for (NodeTuple tuple : node.getValue()) {
            Node keyNode = tuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode))
                throw new ConfigException.Parse(origin(keyNode), "YAML mapping key must be a scalar");
            String key = ((ScalarNode) keyNode).getValue();
            ConfigValue incoming = convert(tuple.getValueNode(), depth + 1);
            values.compute(key, (k, existing) -> existing == null ? incoming : mergeValues(existing, incoming));
        }
        return ConfigValueFactory.fromMap(values, origin(node).description());
    }

    private static ConfigValue mergeValues(ConfigValue existing, ConfigValue incoming) {
        if (existing instanceof ConfigObject && incoming instanceof ConfigObject)
            return ((ConfigObject) incoming).withFallback(existing);
        return incoming;
    }

    private ConfigList convert(SequenceNode node, int depth) {
        List<ConfigValue> values = new ArrayList<>();
        for (Node item : node.getValue()) {
            values.add(convert(item, depth + 1));
        }
        return ConfigValueFactory.fromIterable(values, origin(node).description());
    }

    private ConfigValue convert(ScalarNode node) {
        return ConfigValueFactory.fromAnyRef(scalarValue(node), origin(node).description());
    }

    private static Object scalarValue(ScalarNode node) {
        String tag = node.getTag().getValue();
        String value = node.getValue();
        switch (tag) {
            case "tag:yaml.org,2002:null":
                return null;
            case "tag:yaml.org,2002:bool":
                return "true".equalsIgnoreCase(value);
            case "tag:yaml.org,2002:int":
                return Long.parseLong(value);
            case "tag:yaml.org,2002:float":
                return parseYamlFloat(value);
            default:
                return value;
        }
    }

    private static double parseYamlFloat(String value) {
        switch (value) {
            case ".inf":  return Double.POSITIVE_INFINITY;
            case "-.inf": return Double.NEGATIVE_INFINITY;
            case ".nan":  return Double.NaN;
            default:      return Double.parseDouble(value);
        }
    }

    private static ConfigOrigin origin(Node node) {
        if (node == null) return DEFAULT_ORIGIN;
        return node.getStartMark()
                .map(mark -> DEFAULT_ORIGIN.withLineNumber(mark.getLine() + 1))
                .orElse(DEFAULT_ORIGIN);
    }
}
