package io.h8.config.yaml

import com.typesafe.config.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.composer.Composer
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.parser.ParserImpl
import org.snakeyaml.engine.v2.scanner.StreamReader
import org.snakeyaml.engine.v2.schema.CoreSchema

class YamlNodeConverterSpec extends AnyFlatSpec with Matchers {
  private val converter = YamlNodeConverter.DEFAULT
  private val settings = LoadSettings.builder().setSchema(new CoreSchema()).build()

  private def compose(text: String): Node = {
    val parser = new ParserImpl(settings, new StreamReader(settings, text))
    val composer = new Composer(settings, parser)
    composer.getSingleNode.orElse(null)
  }

  private def scalar(text: String): Any =
    converter.apply(compose(text)).unwrapped()

  // --- null ---

  "scalar" should "return null for null scalar" in {
    converter.apply(compose("null")).unwrapped() shouldBe null
  }

  // --- bool ---

  it should "parse true/false as booleans" in {
    scalar("true") shouldBe true
    scalar("false") shouldBe false
  }

  it should "keep yes/no/on/off as strings (YAML 1.2)" in {
    scalar("yes") shouldBe "yes"
    scalar("no") shouldBe "no"
    scalar("on") shouldBe "on"
    scalar("off") shouldBe "off"
  }

  // --- int ---

  it should "parse decimal integers" in {
    scalar("42") shouldBe 42L
    scalar("-1") shouldBe -1L
  }

  it should "parse hexadecimal integers (YAML 1.2 core)" in {
    scalar("0xFF") shouldBe 255L
    scalar("0x1A") shouldBe 26L
  }

  it should "parse octal integers (YAML 1.2 core)" in {
    scalar("0o17") shouldBe 15L
    scalar("0o777") shouldBe 511L
  }

  it should "throw ConfigException.Parse on integer overflow" in {
    a[ConfigException.Parse] should be thrownBy converter.apply(compose("9999999999999999999999999"))
  }

  // ── apply(Node, ConfigOrigin) — file name in errors ────────────────────────

  "YamlNodeConverter.apply(Node, ConfigOrigin)" should "include file name in numeric overflow error" in {
    val origin = ConfigOriginFactory.newFile("my-config.yaml")
    val ex = the[ConfigException.Parse] thrownBy
      converter.apply(compose("9999999999999999999999999"), origin)
    ex.getMessage should include("my-config.yaml")
  }

  it should "include file name in depth-exceeded error" in {
    val origin = ConfigOriginFactory.newFile("my-config.yaml")
    val ex = the[ConfigException.Parse] thrownBy
      new YamlNodeConverter(1).apply(compose("a:\n  b: 1"), origin)
    ex.getMessage should include("my-config.yaml")
  }

  // --- float ---

  it should "parse float scalars" in {
    scalar("3.14").asInstanceOf[Double] shouldBe 3.14 +- 1e-10
  }

  it should "parse special float values" in {
    scalar(".inf") shouldBe Double.PositiveInfinity
    scalar("-.inf") shouldBe Double.NegativeInfinity
    scalar(".nan").asInstanceOf[Double].isNaN shouldBe true
  }

  // --- string ---

  it should "keep plain and quoted strings as strings" in {
    scalar("hello") shouldBe "hello"
    scalar("'42'") shouldBe "42"
  }

  // --- sequence ---

  it should "convert sequences to ConfigList" in {
    val list = converter.apply(compose("- 1\n- two\n- true")).asInstanceOf[ConfigList]
    list should have size 3
    list.get(0).unwrapped() shouldEqual 1L
    list.get(1).unwrapped() shouldBe "two"
    list.get(2).unwrapped() shouldEqual true
  }

  // --- mapping ---

  it should "convert mappings to ConfigObject" in {
    val obj = converter.apply(compose("a: 1\nb: hello")).asInstanceOf[ConfigObject]
    obj.get("a").unwrapped() shouldEqual 1L
    obj.get("b").unwrapped() shouldBe "hello"
  }

  it should "convert nested mappings" in {
    val cfg = converter.apply(compose("a:\n  b: 1")).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("a.b") shouldBe 1L
  }

  // --- depth limit ---

  it should "throw on exceeded depth" in {
    val shallow = new YamlNodeConverter(1)
    a[ConfigException.Parse] should be thrownBy
      shallow.apply(compose("a:\n  b: 1"))
  }

  it should "throw IllegalArgumentException for non-positive maxDepth" in {
    an[IllegalArgumentException] should be thrownBy new YamlNodeConverter(0)
    an[IllegalArgumentException] should be thrownBy new YamlNodeConverter(-1)
  }

  // --- duplicate keys ---

  it should "use last value for duplicate scalar keys" in {
    converter.apply(compose("a: 1\na: 2")).asInstanceOf[ConfigObject].get("a").unwrapped() shouldEqual 2L
  }

  it should "merge duplicate object keys" in {
    val cfg = converter.apply(compose("a:\n  x: 1\na:\n  y: 2")).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("a.x") shouldBe 1L
    cfg.getLong("a.y") shouldBe 2L
  }

  it should "use last value for conflicting key inside merged objects" in {
    converter.apply(compose("a:\n  x: 1\na:\n  x: 2")).asInstanceOf[ConfigObject].toConfig.getLong("a.x") shouldBe 2L
  }

  it should "overwrite object with scalar when scalar comes last" in {
    converter.apply(compose("a:\n  x: 1\na: 42")).asInstanceOf[ConfigObject].get("a").unwrapped() shouldEqual 42L
  }

  it should "overwrite scalar with object when object comes last" in {
    converter.apply(compose("a: 42\na:\n  x: 1")).asInstanceOf[ConfigObject].toConfig.getLong("a.x") shouldBe 1L
  }

  // --- strings-only mode ---

  "YamlNodeConverter in strings-only mode" should "keep integers as strings" in {
    val c = new YamlNodeConverter(1, true)
    c.apply(compose("42")).unwrapped() shouldBe "42"
    c.apply(compose("-1")).unwrapped() shouldBe "-1"
    c.apply(compose("0xFF")).unwrapped() shouldBe "0xFF"
    c.apply(compose("0o17")).unwrapped() shouldBe "0o17"
  }

  it should "keep floats as strings" in {
    val c = new YamlNodeConverter(1, true)
    c.apply(compose("3.14")).unwrapped() shouldBe "3.14"
    c.apply(compose(".inf")).unwrapped() shouldBe ".inf"
    c.apply(compose("-.inf")).unwrapped() shouldBe "-.inf"
    c.apply(compose(".nan")).unwrapped() shouldBe ".nan"
  }

  it should "keep booleans as strings" in {
    val c = new YamlNodeConverter(1, true)
    c.apply(compose("true")).unwrapped() shouldBe "true"
    c.apply(compose("false")).unwrapped() shouldBe "false"
  }

  it should "still return null for explicit null tag" in {
    val c = new YamlNodeConverter(1, true)
    c.apply(compose("null")).unwrapped() shouldBe null
    c.apply(compose("~")).unwrapped() shouldBe null
  }

  it should "keep quoted 'null' string as a string, not null" in {
    val c = new YamlNodeConverter(1, true)
    c.apply(compose("'null'")).unwrapped() shouldBe "null"
  }

  it should "keep plain strings unchanged" in {
    val c = new YamlNodeConverter(1, true)
    c.apply(compose("hello")).unwrapped() shouldBe "hello"
  }

  it should "not throw on oversized integers (no number parsing)" in {
    val c = new YamlNodeConverter(1, true)
    c.apply(compose("9999999999999999999999999")).unwrapped() shouldBe "9999999999999999999999999"
  }
}
