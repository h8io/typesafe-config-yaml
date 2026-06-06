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

class YamlNodeToConfigValueSpec extends AnyFlatSpec with Matchers {
  private val converter = YamlNodeToConfigValue.DEFAULT
  private val settings = LoadSettings.builder().setSchema(new CoreSchema()).build()

  private def compose(text: String): Node = {
    val parser = new ParserImpl(settings, new StreamReader(settings, text))
    val composer = new Composer(settings, parser)
    composer.getSingleNode.orElse(null)
  }

  private def scalar(text: String): Any =
    converter.convert(compose(text)).unwrapped()

  // --- null ---

  "scalar" should "return null for null scalar" in {
    converter.convert(compose("null")).unwrapped() shouldBe null
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

  it should "throw on integer overflow" in {
    a[NumberFormatException] should be thrownBy converter.convert(compose("9999999999999999999999999"))
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
    val list = converter.convert(compose("- 1\n- two\n- true")).asInstanceOf[ConfigList]
    list should have size 3
    list.get(0).unwrapped() shouldEqual 1L
    list.get(1).unwrapped() shouldBe "two"
    list.get(2).unwrapped() shouldEqual true
  }

  // --- mapping ---

  it should "convert mappings to ConfigObject" in {
    val obj = converter.convert(compose("a: 1\nb: hello")).asInstanceOf[ConfigObject]
    obj.get("a").unwrapped() shouldEqual 1L
    obj.get("b").unwrapped() shouldBe "hello"
  }

  it should "convert nested mappings" in {
    val cfg = converter.convert(compose("a:\n  b: 1")).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("a.b") shouldBe 1L
  }

  // --- depth limit ---

  it should "throw on exceeded depth" in {
    val shallow = new YamlNodeToConfigValue(1)
    a[ConfigException.Parse] should be thrownBy
      shallow.convert(compose("a:\n  b: 1"))
  }

  it should "throw IllegalArgumentException for non-positive maxDepth" in {
    an[IllegalArgumentException] should be thrownBy new YamlNodeToConfigValue(0)
    an[IllegalArgumentException] should be thrownBy new YamlNodeToConfigValue(-1)
  }

  // --- duplicate keys ---

  it should "use last value for duplicate scalar keys" in {
    converter.convert(compose("a: 1\na: 2")).asInstanceOf[ConfigObject].get("a").unwrapped() shouldEqual 2L
  }

  it should "merge duplicate object keys" in {
    val cfg = converter.convert(compose("a:\n  x: 1\na:\n  y: 2")).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("a.x") shouldBe 1L
    cfg.getLong("a.y") shouldBe 2L
  }

  it should "use last value for conflicting key inside merged objects" in {
    converter.convert(compose("a:\n  x: 1\na:\n  x: 2")).asInstanceOf[ConfigObject].toConfig.getLong("a.x") shouldBe 2L
  }

  it should "overwrite object with scalar when scalar comes last" in {
    converter.convert(compose("a:\n  x: 1\na: 42")).asInstanceOf[ConfigObject].get("a").unwrapped() shouldEqual 42L
  }

  it should "overwrite scalar with object when object comes last" in {
    converter.convert(compose("a: 42\na:\n  x: 1")).asInstanceOf[ConfigObject].toConfig.getLong("a.x") shouldBe 1L
  }
}
