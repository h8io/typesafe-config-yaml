package io.h8.config.yaml

import com.typesafe.config._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.composer.Composer
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.parser.ParserImpl
import org.snakeyaml.engine.v2.scanner.StreamReader

class YamlNodeToConfigValueSpec extends AnyFlatSpec with Matchers {
  private val converter = YamlNodeToConfigValue.DEFAULT
  private val settings = LoadSettings.builder().build()

  private def compose(text: String): Node = {
    val parser = new ParserImpl(settings, new StreamReader(settings, text))
    val composer = new Composer(settings, parser)
    composer.getSingleNode.orElse(null)
  }

  private def convert(text: String): ConfigValue =
    converter.convert(compose(text))

  // --- null ---

  "convert" should "return null for null node" in {
    converter.convert(null).unwrapped() shouldBe null
  }

  it should "return null for null scalar" in {
    convert("null").unwrapped() shouldBe null
  }

  // --- bool ---

  it should "parse true/false as booleans" in {
    convert("true").unwrapped() shouldBe true
    convert("false").unwrapped() shouldBe false
  }

  it should "keep yes/no/on/off as strings (YAML 1.2)" in {
    convert("yes").unwrapped() shouldBe "yes"
    convert("no").unwrapped() shouldBe "no"
    convert("on").unwrapped() shouldBe "on"
    convert("off").unwrapped() shouldBe "off"
  }

  // --- int ---

  it should "parse decimal integers" in {
    convert("42").unwrapped() shouldBe 42L
    convert("-1").unwrapped() shouldBe -1L
  }

  it should "throw on integer overflow" in {
    a[NumberFormatException] should be thrownBy convert("9999999999999999999999999")
  }

  // --- float ---

  it should "parse float scalars" in {
    convert("3.14").unwrapped().asInstanceOf[Double] shouldBe 3.14 +- 1e-10
  }

  it should "parse special float values" in {
    convert(".inf").unwrapped() shouldBe Double.PositiveInfinity
    convert("-.inf").unwrapped() shouldBe Double.NegativeInfinity
    convert(".nan").unwrapped().asInstanceOf[Double].isNaN shouldBe true
  }

  // --- string ---

  it should "keep plain and quoted strings as strings" in {
    convert("hello").unwrapped() shouldBe "hello"
    convert("'42'").unwrapped() shouldBe "42"
  }

  // --- sequence ---

  it should "convert sequences to ConfigList" in {
    val list = convert("- 1\n- two\n- true").asInstanceOf[ConfigList]
    list should have size 3
    list.get(0).unwrapped() shouldBe 1L
    list.get(1).unwrapped() shouldBe "two"
    list.get(2).unwrapped() shouldBe true
  }

  // --- mapping ---

  it should "convert mappings to ConfigObject" in {
    val obj = convert("a: 1\nb: hello").asInstanceOf[ConfigObject]
    obj.get("a").unwrapped() shouldBe 1L
    obj.get("b").unwrapped() shouldBe "hello"
  }

  it should "convert nested mappings" in {
    val cfg = convert("a:\n  b: 1").asInstanceOf[ConfigObject].toConfig
    cfg.getLong("a.b") shouldBe 1L
  }

  // --- depth limit ---

  it should "throw on exceeded depth" in {
    val shallow = new YamlNodeToConfigValue(1)
    a[ConfigException.Parse] should be thrownBy
      shallow.convert(compose("a:\n  b: 1"))
  }

  // --- duplicate keys ---

  it should "use last value for duplicate scalar keys" in {
    convert("a: 1\na: 2").asInstanceOf[ConfigObject].get("a").unwrapped() shouldBe 2L
  }

  it should "merge duplicate object keys" in {
    val cfg = convert("a:\n  x: 1\na:\n  y: 2").asInstanceOf[ConfigObject].toConfig
    cfg.getLong("a.x") shouldBe 1L
    cfg.getLong("a.y") shouldBe 2L
  }

  it should "use last value for conflicting key inside merged objects" in {
    convert("a:\n  x: 1\na:\n  x: 2").asInstanceOf[ConfigObject].toConfig.getLong("a.x") shouldBe 2L
  }

  it should "overwrite object with scalar when scalar comes last" in {
    convert("a:\n  x: 1\na: 42").asInstanceOf[ConfigObject].get("a").unwrapped() shouldBe 42L
  }

  it should "overwrite scalar with object when object comes last" in {
    convert("a: 42\na:\n  x: 1").asInstanceOf[ConfigObject].toConfig.getLong("a.x") shouldBe 1L
  }
}
