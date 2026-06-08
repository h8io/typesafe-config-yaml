package io.h8.config.yaml

import com.typesafe.config.{ConfigException, ConfigParseOptions, ConfigResolveOptions}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class ConfigLoaderSpec extends AnyFlatSpec with Matchers {

  // ── DEFAULT / builder defaults ─────────────────────────────────────────────

  "ConfigLoader.DEFAULT" should "load application.conf with YAML includes" in {
    val cfg = ConfigLoader.DEFAULT.load()
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("app-key") shouldBe "from-application"
  }

  it should "probe application.yaml before application.conf" in {
    val cfg = ConfigLoader.DEFAULT.load()
    cfg.getString("yaml-app-key") shouldBe "from-application-yaml"
  }

  "ConfigLoader.builder().build()" should "behave identically to DEFAULT" in {
    val cfg = ConfigLoader.builder().build().load()
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  // ── load(String) ──────────────────────────────────────────────────────────

  "ConfigLoader.load(String)" should "load a named resource basename" in {
    val cfg = ConfigLoader.DEFAULT.load("cf-main")
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  it should "probe basename.yaml before basename.conf" in {
    val cfg = ConfigLoader.DEFAULT.load("app-yaml-priority")
    cfg.getString("source") shouldBe "yaml"
    cfg.getString("yaml-priority-key") shouldBe "only-in-yaml"
    cfg.getString("conf-priority-key") shouldBe "only-in-conf"
  }

  // ── parseFile ─────────────────────────────────────────────────────────────

  "ConfigLoader.parseFile" should "parse a YAML file directly" in {
    val file = writeTemp("direct-key: direct-value")
    try
      ConfigLoader.DEFAULT.parseFile(file).getString("direct-key") shouldBe "direct-value"
    finally file.delete(): Unit
  }

  it should "parse a .conf file with YAML includes" in {
    val dir = Files.createTempDirectory("cl-file-test")
    val yaml = dir.resolve("rel.yaml").toFile
    val conf = dir.resolve("main.conf").toFile
    Files.write(yaml.toPath, "yaml-key: from-yaml\n".getBytes(StandardCharsets.UTF_8))
    Files.write(conf.toPath, "include \"rel.yaml\"\nconf-key = from-conf\n".getBytes(StandardCharsets.UTF_8))
    try {
      val cfg = ConfigLoader.DEFAULT.parseFile(conf)
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("conf-key") shouldBe "from-conf"
    } finally { yaml.delete(); conf.delete(); dir.toFile.delete(): Unit }
  }

  // ── parseURL ──────────────────────────────────────────────────────────────

  "ConfigLoader.parseURL" should "parse a YAML URL directly" in {
    val file = writeTemp("url-key: url-value")
    try
      ConfigLoader.DEFAULT.parseURL(file.toURI.toURL).getString("url-key") shouldBe "url-value"
    finally file.delete(): Unit
  }

  // ── parseResources ────────────────────────────────────────────────────────

  "ConfigLoader.parseResources(String)" should "parse a YAML classpath resource directly" in {
    ConfigLoader.DEFAULT.parseResources("test-resource.yaml").getString("resource") shouldBe "ok"
  }

  it should "parse a .conf classpath resource with YAML includes" in {
    val cfg = ConfigLoader.DEFAULT.parseResources("cf-main.conf")
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigLoader.parseResources(Class, String)" should "parse a YAML classpath resource directly" in {
    ConfigLoader.DEFAULT.parseResources(getClass, "/test-resource.yaml")
      .getString("resource") shouldBe "ok"
  }

  // ── parseString ───────────────────────────────────────────────────────────

  "ConfigLoader.parseString" should "resolve YAML includes" in {
    val cfg = ConfigLoader.DEFAULT.parseString("""include "int-a.yaml"""")
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  // ── builder: yamlMaxDepth ─────────────────────────────────────────────────

  "ConfigLoader.builder().yamlMaxDepth" should "enforce a nesting depth limit on YAML files" in {
    val file = writeTemp("a:\n  b: 1")
    try
      a[ConfigException.Parse] should be thrownBy
        ConfigLoader.builder().yamlMaxDepth(1).build().parseFile(file)
    finally file.delete(): Unit
  }

  // ── builder: classLoader ──────────────────────────────────────────────────

  "ConfigLoader.builder().classLoader" should "use the given class loader for resource lookup" in {
    val loader = ConfigLoader.builder().classLoader(getClass.getClassLoader).build()
    loader.parseResources("test-resource.yaml").getString("resource") shouldBe "ok"
  }

  // ── builder: parseOptions ─────────────────────────────────────────────────

  "ConfigLoader.builder().parseOptions" should "apply custom parse options" in {
    val loader = ConfigLoader.builder()
      .parseOptions(ConfigParseOptions.defaults().setAllowMissing(false))
      .build()
    loader.parseResources("test-resource.yaml").getString("resource") shouldBe "ok"
  }

  it should "not double-wrap when options already carry a YamlConfigIncluder" in {
    val opts = ConfigParseOptions.defaults().prependIncluder(YamlConfigIncluder.DEFAULT)
    val loader = ConfigLoader.builder().parseOptions(opts).build()
    val cfg = loader.parseString("""include "int-a.yaml"""")
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  // ── builder: resolveOptions ───────────────────────────────────────────────

  "ConfigLoader.builder().resolveOptions" should "pass resolve options through to load" in {
    val loader = ConfigLoader.builder()
      .resolveOptions(ConfigResolveOptions.defaults())
      .build()
    loader.load().getString("app-key") shouldBe "from-application"
  }

  // ── system property overrides ─────────────────────────────────────────────

  "ConfigLoader.load" should "honour config.resource system property" in {
    System.setProperty("config.resource", "test-resource.yaml")
    try ConfigLoader.DEFAULT.load().getString("resource") shouldBe "ok"
    finally System.clearProperty("config.resource"): Unit
  }

  it should "honour config.file system property" in {
    val f = writeTemp("file-sys-key: from-sys-file")
    System.setProperty("config.file", f.getAbsolutePath)
    try ConfigLoader.DEFAULT.load().getString("file-sys-key") shouldBe "from-sys-file"
    finally { System.clearProperty("config.file"); f.delete(): Unit }
  }

  it should "honour config.url system property" in {
    val f = writeTemp("url-sys-key: from-sys-url")
    System.setProperty("config.url", f.toURI.toURL.toString)
    try ConfigLoader.DEFAULT.load().getString("url-sys-key") shouldBe "from-sys-url"
    finally { System.clearProperty("config.url"); f.delete(): Unit }
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private def writeTemp(content: String): File = {
    val f = Files.createTempFile("cl-spec", ".yaml").toFile
    Files.write(f.toPath, content.getBytes(StandardCharsets.UTF_8))
    f
  }
}
