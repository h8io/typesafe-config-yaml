package io.h8.config.yaml;

import com.typesafe.config.*;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.*;

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
        List<NodeTuple> merges = new ArrayList<>();
        for (NodeTuple tuple : node.getValue()) {
            Node keyNode = tuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode))
                throw new ConfigException.Parse(origin(keyNode), "YAML mapping key must be a scalar");
            ScalarNode keyScalar = (ScalarNode) keyNode;
            if (Tag.MERGE.equals(keyScalar.getTag())) {
                merges.add(tuple);
            } else {
                ConfigValue incoming = convert(tuple.getValueNode(), depth + 1);
                values.compute(keyScalar.getValue(), (k, existing) -> existing == null ? incoming : mergeValues(existing, incoming));
            }
        }
        for (NodeTuple merge : merges) {
            applyMerge(merge.getValueNode(), depth, values);
        }
        return ConfigValueFactory.fromMap(values, origin(node).description());
    }

    private void applyMerge(Node valueNode, int depth, Map<String, ConfigValue> target) {
        if (valueNode instanceof MappingNode) {
            convert((MappingNode) valueNode, depth).forEach(target::putIfAbsent);
        } else if (valueNode instanceof SequenceNode) {
            for (Node item : ((SequenceNode) valueNode).getValue()) {
                applyMerge(item, depth, target);
            }
        } else {
            throw new ConfigException.Parse(origin(valueNode),
                    "YAML merge value must be a mapping or sequence of mappings");
        }
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
                return isYamlTrue(value);
            case "tag:yaml.org,2002:int":
                return parseYamlInt(value);
            case "tag:yaml.org,2002:float":
                return parseYamlFloat(value);
            default:
                return value;
        }
    }

    private static boolean isYamlTrue(String value) {
        String lc = value.toLowerCase();
        return "true".equals(lc) || "yes".equals(lc) || "on".equals(lc) || "y".equals(lc);
    }

    private static long parseYamlInt(String value) {
        if (value.startsWith("0x") || value.startsWith("0X"))
            return Long.parseLong(value.substring(2), 16);
        if (value.startsWith("0o") || value.startsWith("0O"))
            return Long.parseLong(value.substring(2), 8);
        return Long.parseLong(value);
    }

    private static double parseYamlFloat(String value) {
        switch (value) {
            case ".inf":
            case "+.inf":
                return Double.POSITIVE_INFINITY;
            case "-.inf":
                return Double.NEGATIVE_INFINITY;
            case ".nan":
                return Double.NaN;
            default:
                return Double.parseDouble(value);
        }
    }

    private static ConfigOrigin origin(Node node) {
        if (node == null) return DEFAULT_ORIGIN;
        Mark mark = node.getStartMark();
        if (mark == null) return DEFAULT_ORIGIN;
        return ConfigOriginFactory.newSimple(mark.toString()).withLineNumber(mark.getLine() + 1);
    }
}
