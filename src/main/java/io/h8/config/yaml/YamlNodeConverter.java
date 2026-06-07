package io.h8.config.yaml;

import com.typesafe.config.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.snakeyaml.engine.v2.nodes.*;

/**
 * Converts a snakeyaml-engine {@link Node} tree into a typesafe-config {@link ConfigValue}.
 *
 * <p>Mapping nodes become {@link ConfigObject}, sequence nodes become {@link ConfigList}, and
 * scalar nodes become the closest Java primitive (according to the YAML 1.2 core schema).
 */
public final class YamlNodeConverter implements Function<Node, ConfigValue> {
  static final int DEFAULT_MAX_DEPTH = Integer.MAX_VALUE;
  static final YamlNodeConverter DEFAULT = new YamlNodeConverter(DEFAULT_MAX_DEPTH);

  private static final ConfigOrigin DEFAULT_ORIGIN = ConfigOriginFactory.newSimple("yaml");

  private final int maxDepth;

  /**
   * Creates a converter that rejects YAML documents deeper than {@code maxDepth} levels of nesting.
   *
   * @param maxDepth maximum allowed nesting depth (must be &gt; 0)
   * @throws IllegalArgumentException if {@code maxDepth} is not positive
   */
  public YamlNodeConverter(int maxDepth) {
    if (maxDepth < 1) {
      throw new IllegalArgumentException("maxDepth must be positive");
    }
    this.maxDepth = maxDepth;
  }

  /**
   * Converts a YAML node into a {@link ConfigValue}.
   *
   * @param node the root node of a YAML document
   * @return the corresponding {@link ConfigValue}
   * @throws ConfigException.Parse if the node exceeds the configured depth limit or contains a
   *     mapping with a non-scalar key
   */
  @Override
  public ConfigValue apply(Node node) {
    return convert(node, 1);
  }

  private ConfigValue convert(Node node, int depth) {
    if (depth > maxDepth)
      throw new ConfigException.Parse(
          origin(node), "Exceeded maximum YAML document depth: " + maxDepth);

    if (node instanceof MappingNode) return convertObject((MappingNode) node, depth);
    if (node instanceof SequenceNode) return convertList((SequenceNode) node, depth);
    if (node instanceof ScalarNode) return convertScalar((ScalarNode) node);

    throw new ConfigException.Parse(
        origin(node), "Unexpected YAML node type: " + node.getClass().getName());
  }

  private ConfigObject convertObject(MappingNode node, int depth) {
    Map<String, ConfigValue> values = new LinkedHashMap<>();
    for (NodeTuple tuple : node.getValue()) {
      Node keyNode = tuple.getKeyNode();
      if (!(keyNode instanceof ScalarNode))
        throw new ConfigException.Parse(origin(keyNode), "YAML mapping key must be a scalar");
      String key = ((ScalarNode) keyNode).getValue();
      ConfigValue incoming = convert(tuple.getValueNode(), depth + 1);
      values.compute(
          key, (k, existing) -> existing == null ? incoming : mergeValues(existing, incoming));
    }
    return ConfigValueFactory.fromMap(values, origin(node).description());
  }

  private static ConfigValue mergeValues(ConfigValue existing, ConfigValue incoming) {
    if (existing instanceof ConfigObject && incoming instanceof ConfigObject)
      return ((ConfigObject) incoming).withFallback(existing);
    return incoming;
  }

  private ConfigList convertList(SequenceNode node, int depth) {
    List<ConfigValue> values = new ArrayList<>();
    for (Node item : node.getValue()) {
      values.add(convert(item, depth + 1));
    }
    return ConfigValueFactory.fromIterable(values, origin(node).description());
  }

  private ConfigValue convertScalar(ScalarNode node) {
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

  private static ConfigOrigin origin(Node node) {
    if (node == null) return DEFAULT_ORIGIN;
    return node.getStartMark()
        .map(mark -> DEFAULT_ORIGIN.withLineNumber(mark.getLine() + 1))
        .orElse(DEFAULT_ORIGIN);
  }
}
