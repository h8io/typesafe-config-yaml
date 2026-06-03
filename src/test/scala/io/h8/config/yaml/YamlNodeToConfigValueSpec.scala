package io.h8.config.yaml

import com.typesafe.config._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.yaml.snakeyaml.Yaml

import java.io.StringReader

class YamlNodeToConfigValueSpec extends AnyFlatSpec with Matchers {
  private val converter = YamlNodeToConfigValue.DEFAULT
  private val yaml = new Yaml()

  private def convert(text: String): ConfigValue =
    converter.convert(yaml.compose(new StringReader(text)))

  // --- null ---

  "convert" should "return null for null node" in {
    converter.convert(null).unwrapped() shouldBe null
  }

  it should "return null for null/~ scalars" in {
    convert("null").unwrapped() shouldBe null
    convert("~").unwrapped() shouldBe null
  }

  // --- bool ---

  it should "parse true/false as booleans" in {
    convert("true").unwrapped() shouldBe true
    convert("false").unwrapped() shouldBe false
  }

  it should "parse yes/no/on/off as booleans (YAML 1.1 resolver)" in {
    convert("yes").unwrapped() shouldBe true
    convert("on").unwrapped() shouldBe true
    convert("no").unwrapped() shouldBe false
    convert("off").unwrapped() shouldBe false
  }

  // --- int ---

  it should "parse decimal integers" in {
    convert("42").unwrapped() shouldBe 42L
    convert("-1").unwrapped() shouldBe -1L
  }

  it should "parse hex integers" in {
    convert("0xFF").unwrapped() shouldBe 255L
    convert("0xff").unwrapped() shouldBe 255L
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
    convert("+.inf").unwrapped() shouldBe Double.PositiveInfinity
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
      shallow.convert(yaml.compose(new StringReader("a:\n  b: 1")))
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

  // --- merge key << ---

  it should "expand merge key" in {
    val cfg = convert(
      "base: &base\n  x: 1\n  y: 2\n" +
        "child:\n  <<: *base\n  z: 3\n"
    ).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("child.x") shouldBe 1L
    cfg.getLong("child.y") shouldBe 2L
    cfg.getLong("child.z") shouldBe 3L
  }

  it should "let explicit key override merge key" in {
    val cfg = convert(
      "base: &base\n  x: 1\n" +
        "child:\n  <<: *base\n  x: 99\n"
    ).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("child.x") shouldBe 99L
  }

  it should "let explicit key before << override merge key" in {
    val cfg = convert(
      "base: &base\n  x: 1\n" +
        "child:\n  x: 99\n  <<: *base\n"
    ).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("child.x") shouldBe 99L
  }

  it should "expand sequence merge key" in {
    val cfg = convert(
      "a: &a\n  x: 1\n" +
        "b: &b\n  y: 2\n" +
        "child:\n  <<: [*a, *b]\n  z: 3\n"
    ).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("child.x") shouldBe 1L
    cfg.getLong("child.y") shouldBe 2L
    cfg.getLong("child.z") shouldBe 3L
  }

  it should "give first entry priority in sequence merge key" in {
    val cfg = convert(
      "a: &a\n  x: 1\n" +
        "b: &b\n  x: 2\n" +
        "child:\n  <<: [*a, *b]\n"
    ).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("child.x") shouldBe 1L
  }
}
