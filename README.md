# typesafe-config-yaml

A [typesafe-config](https://github.com/lightbend/config) extension that parses YAML sources into `ConfigValue` objects and lets HOCON files include
YAML via `include` directives.

Built on **snakeyaml-engine 3** with the **YAML 1.2 Core Schema**.

## Dependency

```scala
// build.sbt
libraryDependencies += "io.h8" % "typesafe-config-yaml" % "<version>"
```

## Parsing YAML directly — `YamlConfigFactory`

All `parse*` methods return `List<ConfigValue>`, one element per YAML document in the stream.

```java
// ── single-document mapping ──────────────────────────────────────────────────
List<ConfigValue> docs = YamlConfigFactory.DEFAULT.parseFile(new File("app.yaml"));
Config cfg = ((ConfigObject) docs.get(0)).toConfig();
int port = cfg.getInt("server.port");

// ── from a string ────────────────────────────────────────────────────────────
List<ConfigValue> docs = YamlConfigFactory.DEFAULT.parseString("host: localhost\nport: 8080");
Config cfg = ((ConfigObject) docs.get(0)).toConfig();

// ── classpath resource ───────────────────────────────────────────────────────
List<ConfigValue> docs = YamlConfigFactory.DEFAULT.parseResources("application.yaml");

// ── multi-document stream ────────────────────────────────────────────────────
// Each YAML document (separated by ---) becomes a separate list element.
List<ConfigValue> docs = YamlConfigFactory.DEFAULT.parseString("""
        host: localhost
        ---
        host: remote
        """);
// docs.size() == 2
```

YAML root can be any node type, not just a mapping:

```java
// scalar root
long value = (Long) YamlConfigFactory.DEFAULT.parseString("42").get(0).unwrapped();

// sequence root
ConfigList list = (ConfigList) YamlConfigFactory.DEFAULT.parseString("- a\n- b").get(0);
```

### Custom settings

```java
// ── custom depth limit ───────────────────────────────────────────────────────
YamlConfigFactory factory = new YamlConfigFactory(64);

// ── custom LoadSettings (e.g. explicit code-point limit) ─────────────────────
LoadSettings settings = LoadSettings.builder()
        .setSchema(new CoreSchema())
        .setCodePointLimit(1024 * 1024)   // 1 MB
        .build();
YamlConfigFactory factory = new YamlConfigFactory(settings);

// ── both ─────────────────────────────────────────────────────────────────────
YamlConfigFactory factory = new YamlConfigFactory(settings, 64);
```

## Including YAML from HOCON — `YamlConfigIncluder`

`YamlConfigIncluder` implements `ConfigIncluder` / `ConfigIncluderFile` /
`ConfigIncluderClasspath` / `ConfigIncluderURL`. Files with a `.yaml` or
`.yml` extension are handled by the library; everything else is forwarded to
the default HOCON/JSON/properties includer.

### Wiring into the parse pipeline

Use `prependIncluder` so that the YAML includer is tried first and the default
includer becomes its fallback automatically:

```java
ConfigParseOptions opts = ConfigParseOptions.defaults()
        .prependIncluder(YamlConfigIncluder.DEFAULT);

// parse a HOCON file that may include .yaml / .yml files
Config cfg = ConfigFactory.parseFile(new File("application.conf"), opts);

// or from the classpath
Config cfg = ConfigFactory.parseResources("application.conf", opts);
```

### Priority: position of `include` determines precedence

In HOCON, later assignments override earlier ones. The position of an
`include` directive controls its priority relative to the surrounding file.

**YAML as defaults** — include first, then selectively override:

```hocon
# application.conf
include "defaults.yaml"   # provides baseline values

server.port = 9090        # overrides whatever defaults.yaml set for server.port
```

**YAML as overrides** — define inline first, then include to override:

```hocon
# application.conf
server.port = 9090        # default

include "overrides.yaml"  # server.port from the YAML file wins
```

### Multi-format priority chain

All four formats can be layered. The last `include` has the highest priority
among includes; an inline definition after all includes beats them all.

```hocon
# application.conf

include "defaults.yaml"         # layer 1 – YAML defaults
include "extra.json"            # layer 2 – JSON adds/overrides
include "extra.conf"            # layer 3 – HOCON adds/overrides
shared-key = "from-main"        # layer 4 – highest priority
```

```yaml
# defaults.yaml
database.host: localhost
database.port: 5432
shared-key: from-yaml
```

Result: `database.host` and `database.port` come from YAML; `shared-key` is
`"from-main"`.

### Multi-document YAML in an include

When an included YAML file contains multiple documents, they are merged
with the **first document taking priority**:

```yaml
# layered.yaml
database.pool-size: 10    # document 1 — highest priority
---
database.pool-size: 5     # document 2 — lower priority (ignored for this key)
database.timeout: 30s     # document 2 — only source for this key (kept)
```

```hocon
include "layered.yaml"
# database.pool-size = 10
# database.timeout   = 30s
```

`null` documents are silently skipped. A document that is a sequence or
scalar causes `ConfigException.Parse`.

### Custom factory in the includer

Pass a custom `YamlConfigFactory` to apply depth limits or custom
`LoadSettings` to every YAML file that is included:

```java
YamlConfigFactory factory = new YamlConfigFactory(settings, 32);
ConfigParseOptions opts = ConfigParseOptions.defaults()
        .prependIncluder(new YamlConfigIncluder(factory));
```

## YAML 1.2 Core Schema type mapping

| YAML tag  | Java type                                    |
|-----------|----------------------------------------------|
| `!!null`  | `null`                                       |
| `!!bool`  | `Boolean` (`true`/`True`/`TRUE`/`false`/…)   |
| `!!int`   | `Long` (decimal, `0xFF` hex, `0o17` octal)   |
| `!!float` | `Double` (`.inf`, `-.inf`, `.nan` supported) |
| `!!str`   | `String`                                     |
| mapping   | `ConfigObject`                               |
| sequence  | `ConfigList`                                 |

## Limitations

**Custom tags** are not supported. Only the YAML 1.2 Core Schema tags listed
above are converted to their Java counterparts. Any other tag
(`!!timestamp`, `!!binary`, application-specific tags, etc.) is treated as a
plain string.

**Recursive aliases** are rejected. A YAML anchor that refers back to a
parent node (creating a cycle) causes `ConfigException.Parse`. This is the
snakeyaml-engine default and is intentional — cycles cannot be represented in
typesafe-config's immutable value model.

**Relative filesystem paths in `include`** are not resolved. An unqualified
`include "file.yaml"` looks up the resource on the classpath and, if not
found there, treats `file.yaml` as a literal (possibly absolute) filesystem
path. To include a YAML file by path relative to the including `.conf` file
use the qualified form `include file("/absolute/path/to/file.yaml")` or place
the YAML file on the classpath.
