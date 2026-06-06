package io.h8.config.yaml

import com.typesafe.config.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class YamlConfigIntegrationSpec extends AnyFlatSpec with Matchers {

  // prependIncluder wires YamlConfigIncluder as the first in chain, with the
  // default HOCON/JSON/properties includer automatically set as its fallback.
  private def opts: ConfigParseOptions =
    ConfigParseOptions.defaults().prependIncluder(YamlConfigIncluder.DEFAULT)

  private def parse(hocon: String): Config =
    ConfigFactory.parseString(hocon, opts)

  // ── YAML priority levels ───────────────────────────────────────────────────

  "YAML include" should "act as defaults when included first" in {
    // shared-key defined in YAML, then overridden inline → inline wins
    val cfg = parse(
      """include "int-a.yaml"
        |shared-key = "from-main"
        |""".stripMargin)

    cfg.getString("yaml-key") shouldBe "from-yaml" // only in YAML, kept
    cfg.getString("shared-key") shouldBe "from-main" // inline overrides YAML
  }

  it should "act as overrides when included last" in {
    // shared-key defined inline first, then YAML included → YAML wins
    val cfg = parse(
      """shared-key = "from-main"
        |main-only-key = "from-main"
        |include "int-a.yaml"
        |""".stripMargin)

    cfg.getString("yaml-key") shouldBe "from-yaml" // from YAML
    cfg.getString("shared-key") shouldBe "from-yaml" // YAML overrides inline
    cfg.getString("main-only-key") shouldBe "from-main" // not in YAML, unchanged
  }

  // ── multi-format: each source contributes its own key ─────────────────────

  "multi-format include" should "load each format's unique key" in {
    val cfg = parse(
      """include "int-a.yaml"
        |include "int-b.json"
        |include "int-c.properties"
        |include "int-d.conf"
        |""".stripMargin)

    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("json-key") shouldBe "from-json"
    cfg.getString("props-key") shouldBe "from-props"
    cfg.getString("hocon-key") shouldBe "from-hocon"
  }

  // ── priority determined by include position ────────────────────────────────

  it should "apply last-include-wins priority for shared-key" in {
    // All four files define shared-key; last included file wins.
    val cfg = parse(
      """include "int-a.yaml"
        |include "int-b.json"
        |include "int-c.properties"
        |include "int-d.conf"
        |""".stripMargin)

    cfg.getString("shared-key") shouldBe "from-hocon" // int-d.conf last
  }

  it should "let inline value beat all includes when defined last" in {
    val cfg = parse(
      """include "int-a.yaml"
        |include "int-b.json"
        |include "int-c.properties"
        |include "int-d.conf"
        |shared-key = "from-main"
        |""".stripMargin)

    cfg.getString("shared-key") shouldBe "from-main"
  }

  it should "let inline value be overridden by includes when defined first" in {
    val cfg = parse(
      """shared-key = "from-main"
        |include "int-a.yaml"
        |include "int-b.json"
        |include "int-c.properties"
        |include "int-d.conf"
        |""".stripMargin)

    cfg.getString("shared-key") shouldBe "from-hocon" // last include wins
  }

  it should "respect arbitrary include ordering for priority" in {
    // Reverse order: HOCON first, YAML last → YAML wins for shared-key
    val cfg = parse(
      """include "int-d.conf"
        |include "int-c.properties"
        |include "int-b.json"
        |include "int-a.yaml"
        |""".stripMargin)

    cfg.getString("shared-key") shouldBe "from-yaml" // int-a.yaml last
    // All unique keys still present regardless of order
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("json-key") shouldBe "from-json"
    cfg.getString("props-key") shouldBe "from-props"
    cfg.getString("hocon-key") shouldBe "from-hocon"
  }

  it should "resolve YAML path relative to the including .conf file" in {
    val dir = java.nio.file.Files.createTempDirectory("yaml-rel-test")
    val yaml = dir.resolve("rel.yaml").toFile
    val conf = dir.resolve("main.conf").toFile
    java.nio.file.Files.write(yaml.toPath, "rel-key: from-relative-yaml\n".getBytes)
    java.nio.file.Files.write(conf.toPath,
      "include \"rel.yaml\"\nconf-key = from-conf\n".getBytes)
    try {
      val cfg = ConfigFactory.parseFile(conf, opts)
      cfg.getString("rel-key") shouldBe "from-relative-yaml"
      cfg.getString("conf-key") shouldBe "from-conf"
    } finally {
      yaml.delete(): Unit
      conf.delete(): Unit
      dir.toFile.delete(): Unit
    }
  }

  it should "sandwich YAML between lower and higher priority includes" in {
    // JSON (lowest) → YAML (middle) → HOCON (highest)
    val cfg = parse(
      """include "int-b.json"
        |include "int-a.yaml"
        |include "int-d.conf"
        |""".stripMargin)

    cfg.getString("shared-key") shouldBe "from-hocon" // HOCON wins
    cfg.getString("yaml-key") shouldBe "from-yaml" // YAML unique key present
    cfg.getString("json-key") shouldBe "from-json" // JSON unique key present
    cfg.getString("hocon-key") shouldBe "from-hocon" // HOCON unique key present
  }
}
