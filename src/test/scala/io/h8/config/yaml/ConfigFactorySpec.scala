package io.h8.config.yaml

import com.typesafe.config.ConfigParseOptions
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties

class ConfigFactorySpec extends AnyFlatSpec with Matchers {

  // ── parseString ────────────────────────────────────────────────────────────

  "ConfigFactory.parseString" should "resolve YAML includes automatically" in {
    val cfg = ConfigFactory.parseString("""include "int-a.yaml"""")
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  it should "resolve non-YAML includes via the default fallback includer" in {
    val cfg = ConfigFactory.parseString("""include "int-b.json"""")
    cfg.getString("json-key") shouldBe "from-json"
  }

  it should "resolve mixed YAML and non-YAML includes in one string" in {
    val cfg = ConfigFactory.parseString(
      """include "int-a.yaml"
        |include "int-b.json"
        |include "int-d.conf"
        |""".stripMargin)
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("json-key") shouldBe "from-json"
    cfg.getString("hocon-key") shouldBe "from-hocon"
  }

  it should "be idempotent when YamlConfigIncluder is already in options" in {
    val opts = ConfigParseOptions.defaults().prependIncluder(YamlConfigIncluder.DEFAULT)
    val cfg = ConfigFactory.parseString("""include "int-a.yaml"""", opts)
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  it should "accept explicit ConfigParseOptions and still inject the includer" in {
    val opts = ConfigParseOptions.defaults().setAllowMissing(false)
    val cfg = ConfigFactory.parseString("""include "int-a.yaml"""", opts)
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  it should "parse a plain HOCON string without includes" in {
    val cfg = ConfigFactory.parseString("a = 1\nb = hello")
    cfg.getInt("a") shouldBe 1
    cfg.getString("b") shouldBe "hello"
  }

  // ── parseFile ──────────────────────────────────────────────────────────────

  "ConfigFactory.parseFile" should "resolve YAML includes from a .conf file" in {
    val dir = Files.createTempDirectory("cf-file-test")
    val yaml = dir.resolve("rel.yaml").toFile
    val conf = dir.resolve("main.conf").toFile
    Files.write(yaml.toPath, "yaml-key: from-yaml\n".getBytes(StandardCharsets.UTF_8))
    Files.write(conf.toPath, "include \"rel.yaml\"\nconf-key = from-conf\n".getBytes(StandardCharsets.UTF_8))
    try {
      val cfg = ConfigFactory.parseFile(conf)
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("conf-key") shouldBe "from-conf"
    } finally {
      yaml.delete()
      conf.delete()
      dir.toFile.delete(): Unit
    }
  }

  it should "accept explicit ConfigParseOptions" in {
    val dir = Files.createTempDirectory("cf-file-opts-test")
    val yaml = dir.resolve("rel.yaml").toFile
    val conf = dir.resolve("main.conf").toFile
    Files.write(yaml.toPath, "yaml-key: from-yaml\n".getBytes(StandardCharsets.UTF_8))
    Files.write(conf.toPath, "include \"rel.yaml\"\n".getBytes(StandardCharsets.UTF_8))
    try {
      val cfg = ConfigFactory.parseFile(conf, ConfigParseOptions.defaults())
      cfg.getString("yaml-key") shouldBe "from-yaml"
    } finally {
      yaml.delete()
      conf.delete()
      dir.toFile.delete(): Unit
    }
  }

  // ── parseURL ───────────────────────────────────────────────────────────────

  "ConfigFactory.parseURL" should "resolve YAML includes from a URL" in {
    val dir = Files.createTempDirectory("cf-url-test")
    val yaml = dir.resolve("rel.yaml").toFile
    val conf = dir.resolve("main.conf").toFile
    Files.write(yaml.toPath, "yaml-key: from-yaml\n".getBytes(StandardCharsets.UTF_8))
    Files.write(conf.toPath, "include \"rel.yaml\"\n".getBytes(StandardCharsets.UTF_8))
    try {
      val cfg = ConfigFactory.parseURL(conf.toURI.toURL)
      cfg.getString("yaml-key") shouldBe "from-yaml"
    } finally {
      yaml.delete()
      conf.delete()
      dir.toFile.delete(): Unit
    }
  }

  // ── parseReader ────────────────────────────────────────────────────────────

  "ConfigFactory.parseReader" should "resolve YAML includes from a reader" in {
    val reader = new StringReader("""include "int-a.yaml"""")
    val cfg = ConfigFactory.parseReader(reader)
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  // ── parseResources ─────────────────────────────────────────────────────────

  "ConfigFactory.parseResources(String)" should "resolve YAML includes from a classpath resource" in {
    val cfg = ConfigFactory.parseResources("cf-main.conf")
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigFactory.parseResources(Class, String)" should "resolve YAML includes from a classpath resource" in {
    val cfg = ConfigFactory.parseResources(getClass, "/cf-main.conf")
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigFactory.parseResources(ClassLoader, String)" should "resolve YAML includes from a classpath resource" in {
    val cfg = ConfigFactory.parseResources(getClass.getClassLoader, "cf-main.conf")
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  // ── empty ──────────────────────────────────────────────────────────────────

  "ConfigFactory.empty" should "return an empty config" in {
    ConfigFactory.empty().entrySet() shouldBe empty
  }

  it should "return a config with the given origin description" in {
    val cfg = ConfigFactory.empty("test-origin")
    cfg.origin().description() shouldBe "test-origin"
  }

  // ── parseMap ───────────────────────────────────────────────────────────────

  "ConfigFactory.parseMap" should "convert a map to config" in {
    val cfg = ConfigFactory.parseMap(java.util.Map.of("x", Integer.valueOf(42), "y", "hello"))
    cfg.getInt("x") shouldBe 42
    cfg.getString("y") shouldBe "hello"
  }

  // ── parseProperties ────────────────────────────────────────────────────────

  "ConfigFactory.parseProperties" should "convert properties to config" in {
    val props = new Properties()
    props.setProperty("a.b", "value")
    val cfg = ConfigFactory.parseProperties(props)
    cfg.getString("a.b") shouldBe "value"
  }
}
