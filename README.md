[![GitHub release](https://img.shields.io/github/v/release/h8io/typesafe-config-yaml)](https://github.com/h8io/typesafe-config-yaml/releases/latest)

# typesafe-config-yaml

Typesafe Config is the de facto configuration standard in many Scala/JVM projects, but it does not parse YAML.
This library adds YAML 1.2 support without replacing the existing HOCON workflow.

## Quick start

**1. Add the dependency** (built on snakeyaml-engine 3, YAML 1.2 Core Schema):

```scala
// build.sbt — replace with the latest version if needed
libraryDependencies += "io.h8" % "typesafe-config-yaml" % "1.1.0"
```

**2. Replace the import** — nothing else changes:

```java
// before
import com.typesafe.config.ConfigFactory;

// after
import io.h8.config.yaml.ConfigFactory;
```

That's it. `ConfigFactory.load()` now probes `application.yaml` first, YAML files parse by extension,
and `include "file.yaml"` works inside HOCON files.

## Drop-in replacement — `io.h8.config.yaml.ConfigFactory`

`io.h8.config.yaml.ConfigFactory` is a static façade with the same API as `com.typesafe.config.ConfigFactory` that:

- probes `.yaml` / `.yml` resources **before** `.conf` / `.json` / `.properties` in `load()` and `load(String)`,
- detects `.yaml` / `.yml` by extension in `parse*(File/URL/resource)` and parses them directly,
- prepends `YamlConfigIncluder.DEFAULT` everywhere else so HOCON `include` directives resolve YAML files.

```java
// probes application.yaml first, then application.conf; YAML includes resolve automatically
Config cfg = ConfigFactory.load();

// explicit resource basename — probes .yaml / .yml first, then .conf / .json / .properties
Config cfg = ConfigFactory.load("application");

// parse a YAML file directly — detected by .yaml / .yml extension
Config cfg = ConfigFactory.parseFile(new File("app.yaml"));
Config cfg = ConfigFactory.parseResources("config.yaml");
Config cfg = ConfigFactory.parseURL(url);                    // works if URL path ends in .yaml

// parse a HOCON file that includes YAML
Config cfg = ConfigFactory.parseFile(new File("app.conf"));
Config cfg = ConfigFactory.parseString("include \"extra.yaml\"\nkey = value");
```

### Custom `ConfigParseOptions`

When you pass a `ConfigParseOptions` that already has a `YamlConfigIncluder` at the front of its includer chain, the
façade detects this and does **not** prepend a second one — so calling it with manually-wired options is safe and
idempotent:

```java
ConfigParseOptions opts = ConfigParseOptions.defaults()
        .prependIncluder(new YamlConfigIncluder(customFactory));

Config cfg = ConfigFactory.load(opts);   // customFactory is used, no double-wrapping
```

### Scope

`io.h8.config.yaml.ConfigFactory` covers every overload of `load`, `parse*`, `defaultApplication`,
`defaultReference`, `defaultOverrides`, `empty`, `invalidateCaches`, `systemProperties`, and `systemEnvironment`. The
only methods that do **not** inject an includer are the `load(Config, …)` family — those accept an already-parsed
config and only perform substitution resolution.

## Configurable instance — `ConfigLoader`

`ConfigLoader` is an instance-based alternative to the static `ConfigFactory` facade. All settings
are fixed at construction time via a builder, so call sites need no parameters beyond the resource
itself.

```java
ConfigLoader loader = ConfigLoader.builder()
        .yamlMaxDepth(64)                                                    // or .yamlFactory(...)
        .parseOptions(ConfigParseOptions.defaults().setAllowMissing(false))
        .resolveOptions(ConfigResolveOptions.noSystem())
        .classLoader(myLoader)
        .build();

// load — sys-props override first; otherwise .yaml / .yml take priority over .conf / .json / .properties
Config cfg = loader.load();              // application.yaml > application.conf > reference + system props
Config cfg = loader.load("basename");    // basename.yaml > basename.conf / .json / .properties

// parse — .yaml / .yml detected by extension, everything else treated as HOCON/JSON
Config cfg = loader.parseFile(new File("app.yaml"));
Config cfg = loader.parseResources("config.yaml");
Config cfg = loader.parseResources(MyApp.class, "/config.yaml");
Config cfg = loader.parseURL(url);
Config cfg = loader.parseString("include \"extra.yaml\"\nkey = value");
```

Use `ConfigLoader.DEFAULT` when no custom settings are needed — it is equivalent to
`ConfigFactory.load()` with all defaults.

### Builder options

| Method | Default | Description |
|---|---|---|
| `yamlFactory(YamlConfigFactory)` | `YamlConfigFactory.DEFAULT` | Custom YAML parser (settings, schema) |
| `yamlMaxDepth(int)` | — | Shortcut: creates a factory with the given depth limit |
| `parseOptions(ConfigParseOptions)` | `ConfigParseOptions.defaults()` | Base HOCON parse options; `YamlConfigIncluder` is prepended automatically |
| `resolveOptions(ConfigResolveOptions)` | `ConfigResolveOptions.defaults()` | Substitution resolution options |
| `classLoader(ClassLoader)` | context class loader | Class loader for classpath resource lookup |

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

`YamlConfigIncluder` implements `ConfigIncluder` / `ConfigIncluderFile` / `ConfigIncluderClasspath` /
`ConfigIncluderURL`. Files with a `.yaml` or `.yml` extension are handled by the library; everything else is forwarded
to the default HOCON/JSON/properties includer.

### Wiring into the parse pipeline

Use `prependIncluder` so that the YAML includer is tried first and the default  includer becomes its fallback
automatically:

```java
ConfigParseOptions opts = ConfigParseOptions.defaults()
        .prependIncluder(YamlConfigIncluder.DEFAULT);

// parse a HOCON file that may include .yaml / .yml files
Config cfg = ConfigFactory.parseFile(new File("application.conf"), opts);

// or from the classpath
Config cfg = ConfigFactory.parseResources("application.conf", opts);
```

### Priority: position of `include` determines precedence

In HOCON, later assignments override earlier ones. The position of an `include` directive controls its priority relative
to the surrounding file.

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

All four formats can be layered. The last `include` has the highest priority among includes; an inline definition after
all includes beats them all.

```hocon
# application.conf

include "defaults.yaml"         # layer 1 – YAML defaults
include "extra.json"            # layer 2 – JSON adds/overrides
include "extra.conf"            # layer 3 – HOCON adds/overrides
shared-key = "from-main"        # layer 4 – highest priority
```

```yaml
# defaults.yaml
database:
  host: localhost
  port: 5432
shared-key: from-yaml
```

Result: `database.host` and `database.port` come from YAML; `shared-key` is `"from-main"`.

> **YAML vs HOCON keys:** In HOCON, `server.port = 8080` is a path — it creates a nested object `{server: {port: 8080}}`.
> In YAML, `server.port: 8080` is a plain string key with a literal dot — no nesting is created, and
> `cfg.getString("server.port")` will throw `ConfigException.Missing`. Always use YAML indentation to produce nested
> objects that typesafe-config paths can reach.

### Multi-document YAML in an include

When an included YAML file contains multiple documents, they are merged with the **first document taking priority**:

```yaml
# layered.yaml
database:
  pool-size: 10    # document 1 — highest priority
---
database:
  pool-size: 5     # document 2 — lower priority (ignored for this key)
  timeout: 30s     # document 2 — only source for this key (kept)
```

```hocon
include "layered.yaml"
# database.pool-size = 10
# database.timeout   = 30s
```

A document whose root is `null` (e.g. an empty `---` separator) is silently skipped.
`null` values inside a document are kept as-is, mapped to `null` in the resulting `ConfigObject`.
A document whose root is a sequence or scalar causes `ConfigException.Parse`.

### Custom factory in the includer

Pass a custom `YamlConfigFactory` to apply depth limits or custom `LoadSettings` to every YAML file that is included:

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

**Custom tags** are not supported. Only the YAML 1.2 Core Schema tags listed above are converted
to their Java counterparts. Any other tag (`!!timestamp`, `!!binary`, application-specific tags, etc.) is treated as a
plain string.

**Recursive aliases** are rejected. A YAML anchor that refers back to a parent node (creating a cycle) causes
`ConfigException.Parse`. This is the snakeyaml-engine default and is intentional — cycles cannot be represented in
typesafe-config's immutable value model. As a special case, `LoadSettings` with `allowRecursiveKeys` enabled is
rejected at construction time with `IllegalArgumentException`.

**Relative paths in `include`** are resolved in this order:

1. Relative to the including file's location (via `ConfigIncludeContext.relativeTo`). Works when the host `.conf` is 
   parsed from a filesystem file or a classpath resource.
2. Classpath lookup by name.
3. Absolute or CWD-relative filesystem path.

When parsing from `ConfigFactory.parseString(...)` there is no file origin, so step 1 is skipped,
and only classpath / absolute paths are tried.
