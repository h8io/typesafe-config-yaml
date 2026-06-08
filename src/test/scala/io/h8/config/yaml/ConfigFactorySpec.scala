package io.h8.config.yaml

import com.typesafe.config.ConfigParseOptions
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{File, StringReader}
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

  it should "accept explicit ConfigParseOptions" in {
    val dir = Files.createTempDirectory("cf-url-opts-test")
    val yaml = dir.resolve("rel.yaml").toFile
    val conf = dir.resolve("main.conf").toFile
    Files.write(yaml.toPath, "yaml-key: from-yaml\n".getBytes(StandardCharsets.UTF_8))
    Files.write(conf.toPath, "include \"rel.yaml\"\n".getBytes(StandardCharsets.UTF_8))
    try {
      val cfg = ConfigFactory.parseURL(conf.toURI.toURL, ConfigParseOptions.defaults())
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

  it should "accept explicit ConfigParseOptions" in {
    val reader = new StringReader("""include "int-a.yaml"""")
    val cfg = ConfigFactory.parseReader(reader, ConfigParseOptions.defaults())
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  it should "parse a YAML file directly when given a .yaml extension" in {
    val file = writeTemp("direct-key: direct-value")
    try {
      val cfg = ConfigFactory.parseFile(file)
      cfg.getString("direct-key") shouldBe "direct-value"
    } finally file.delete(): Unit
  }

  it should "parse a YAML file directly with explicit ConfigParseOptions" in {
    val file = writeTemp("direct-key: direct-value")
    try {
      val cfg = ConfigFactory.parseFile(file, ConfigParseOptions.defaults())
      cfg.getString("direct-key") shouldBe "direct-value"
    } finally file.delete(): Unit
  }

  // ── parseURL (YAML direct) ─────────────────────────────────────────────────

  "ConfigFactory.parseURL" should "parse a YAML URL directly when the path has a .yaml extension" in {
    val file = writeTemp("url-key: url-value")
    try {
      val cfg = ConfigFactory.parseURL(file.toURI.toURL)
      cfg.getString("url-key") shouldBe "url-value"
    } finally file.delete(): Unit
  }

  it should "parse a YAML URL directly with explicit ConfigParseOptions" in {
    val file = writeTemp("url-key: url-value")
    try {
      val cfg = ConfigFactory.parseURL(file.toURI.toURL, ConfigParseOptions.defaults())
      cfg.getString("url-key") shouldBe "url-value"
    } finally file.delete(): Unit
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

  // ── parseFileAnySyntax ─────────────────────────────────────────────────────

  "ConfigFactory.parseFileAnySyntax" should "resolve YAML includes from a .conf file inferred by extension" in {
    val dir = Files.createTempDirectory("cf-anysyntax-test")
    val yaml = dir.resolve("rel.yaml").toFile
    val conf = dir.resolve("main.conf").toFile
    Files.write(yaml.toPath, "yaml-key: from-yaml\n".getBytes(StandardCharsets.UTF_8))
    Files.write(conf.toPath, "include \"rel.yaml\"\nconf-key = from-conf\n".getBytes(StandardCharsets.UTF_8))
    try {
      val cfg = ConfigFactory.parseFileAnySyntax(dir.resolve("main").toFile)
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("conf-key") shouldBe "from-conf"
    } finally {
      yaml.delete()
      conf.delete()
      dir.toFile.delete(): Unit
    }
  }

  it should "accept explicit ConfigParseOptions" in {
    val dir = Files.createTempDirectory("cf-anysyntax-opts-test")
    val yaml = dir.resolve("rel.yaml").toFile
    val conf = dir.resolve("main.conf").toFile
    Files.write(yaml.toPath, "yaml-key: from-yaml\n".getBytes(StandardCharsets.UTF_8))
    Files.write(conf.toPath, "include \"rel.yaml\"\n".getBytes(StandardCharsets.UTF_8))
    try {
      val cfg = ConfigFactory.parseFileAnySyntax(dir.resolve("main").toFile, ConfigParseOptions.defaults())
      cfg.getString("yaml-key") shouldBe "from-yaml"
    } finally {
      yaml.delete()
      conf.delete()
      dir.toFile.delete(): Unit
    }
  }

  // ── parseResources (with options) ──────────────────────────────────────────

  "ConfigFactory.parseResources(String, ConfigParseOptions)" should "resolve YAML includes from a classpath resource" in {
    val cfg = ConfigFactory.parseResources("cf-main.conf", ConfigParseOptions.defaults())
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigFactory.parseResources(Class, String, ConfigParseOptions)" should
    "resolve YAML includes from a classpath resource" in {
      val cfg = ConfigFactory.parseResources(getClass, "/cf-main.conf", ConfigParseOptions.defaults())
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("conf-key") shouldBe "from-cf-main"
    }

  "ConfigFactory.parseResources(ClassLoader, String, ConfigParseOptions)" should
    "resolve YAML includes from a classpath resource" in {
      val cfg = ConfigFactory.parseResources(getClass.getClassLoader, "cf-main.conf", ConfigParseOptions.defaults())
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("conf-key") shouldBe "from-cf-main"
    }

  // ── parseResourcesAnySyntax ────────────────────────────────────────────────

  "ConfigFactory.parseResourcesAnySyntax(String)" should "find a classpath resource by basename" in {
    val cfg = ConfigFactory.parseResourcesAnySyntax("cf-main")
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigFactory.parseResourcesAnySyntax(String, ConfigParseOptions)" should
    "find a classpath resource by basename with options" in {
      val cfg = ConfigFactory.parseResourcesAnySyntax("cf-main", ConfigParseOptions.defaults())
      cfg.getString("conf-key") shouldBe "from-cf-main"
    }

  "ConfigFactory.parseResourcesAnySyntax(Class, String)" should "find a classpath resource by basename" in {
    val cfg = ConfigFactory.parseResourcesAnySyntax(getClass, "/cf-main")
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigFactory.parseResourcesAnySyntax(Class, String, ConfigParseOptions)" should
    "find a classpath resource by basename with options" in {
      val cfg = ConfigFactory.parseResourcesAnySyntax(getClass, "/cf-main", ConfigParseOptions.defaults())
      cfg.getString("conf-key") shouldBe "from-cf-main"
    }

  "ConfigFactory.parseResourcesAnySyntax(ClassLoader, String)" should "find a classpath resource by basename" in {
    val cfg = ConfigFactory.parseResourcesAnySyntax(getClass.getClassLoader, "cf-main")
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigFactory.parseResourcesAnySyntax(ClassLoader, String, ConfigParseOptions)" should
    "find a classpath resource by basename with options" in {
      val cfg = ConfigFactory.parseResourcesAnySyntax(getClass.getClassLoader, "cf-main", ConfigParseOptions.defaults())
      cfg.getString("conf-key") shouldBe "from-cf-main"
    }

  // ── parseApplicationReplacement ────────────────────────────────────────────

  "ConfigFactory.parseApplicationReplacement()" should "return an Optional (empty when no override is configured)" in {
    val result = ConfigFactory.parseApplicationReplacement()
    result should not be null
  }

  "ConfigFactory.parseApplicationReplacement(ClassLoader)" should "return an Optional with the given ClassLoader" in {
    val result = ConfigFactory.parseApplicationReplacement(getClass.getClassLoader)
    result should not be null
  }

  "ConfigFactory.parseApplicationReplacement(ConfigParseOptions)" should "return an Optional with given options" in {
    val result = ConfigFactory.parseApplicationReplacement(ConfigParseOptions.defaults())
    result should not be null
  }

  // ── parseProperties ────────────────────────────────────────────────────────

  "ConfigFactory.parseProperties" should "convert properties to config" in {
    val props = new Properties()
    props.setProperty("a.b", "value")
    val cfg = ConfigFactory.parseProperties(props)
    cfg.getString("a.b") shouldBe "value"
  }

  it should "accept explicit ConfigParseOptions" in {
    val props = new Properties()
    props.setProperty("a.b", "value")
    val cfg = ConfigFactory.parseProperties(props, ConfigParseOptions.defaults())
    cfg.getString("a.b") shouldBe "value"
  }

  // ── parseMap (with origin) ─────────────────────────────────────────────────

  "ConfigFactory.parseMap(Map, String)" should "convert a map to config with origin description" in {
    val cfg = ConfigFactory.parseMap(java.util.Map.of("z", "hello"), "test-origin")
    cfg.getString("z") shouldBe "hello"
    cfg.origin().description() should include("test-origin")
  }

  // ── defaultApplication ─────────────────────────────────────────────────────

  "ConfigFactory.defaultApplication()" should "parse application.conf with YAML includes" in {
    val cfg = ConfigFactory.defaultApplication()
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("app-key") shouldBe "from-application"
  }

  "ConfigFactory.defaultApplication(ClassLoader)" should "parse application.conf with a given ClassLoader" in {
    val cfg = ConfigFactory.defaultApplication(getClass.getClassLoader)
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("app-key") shouldBe "from-application"
  }

  "ConfigFactory.defaultApplication(ConfigParseOptions)" should "parse application.conf with custom options" in {
    val cfg = ConfigFactory.defaultApplication(ConfigParseOptions.defaults())
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("app-key") shouldBe "from-application"
  }

  // ── defaultReference / defaultReferenceUnresolved ──────────────────────────

  "ConfigFactory.defaultReference()" should "return a non-null Config" in {
    ConfigFactory.defaultReference() should not be null
  }

  "ConfigFactory.defaultReference(ClassLoader)" should "return a non-null Config" in {
    ConfigFactory.defaultReference(getClass.getClassLoader) should not be null
  }

  "ConfigFactory.defaultReferenceUnresolved()" should "return a non-null Config" in {
    ConfigFactory.defaultReferenceUnresolved() should not be null
  }

  "ConfigFactory.defaultReferenceUnresolved(ClassLoader)" should "return a non-null Config" in {
    ConfigFactory.defaultReferenceUnresolved(getClass.getClassLoader) should not be null
  }

  // ── defaultOverrides ───────────────────────────────────────────────────────

  "ConfigFactory.defaultOverrides()" should "return a non-null Config" in {
    ConfigFactory.defaultOverrides() should not be null
  }

  "ConfigFactory.defaultOverrides(ClassLoader)" should "return a non-null Config" in {
    ConfigFactory.defaultOverrides(getClass.getClassLoader) should not be null
  }

  // ── invalidateCaches ───────────────────────────────────────────────────────

  "ConfigFactory.invalidateCaches()" should "complete without error" in {
    noException should be thrownBy ConfigFactory.invalidateCaches()
  }

  // ── system / env ───────────────────────────────────────────────────────────

  "ConfigFactory.systemProperties()" should "return a Config containing java.home" in {
    val cfg = ConfigFactory.systemProperties()
    cfg.hasPath("java.home") shouldBe true
  }

  "ConfigFactory.systemEnvironment()" should "return a non-null Config" in {
    ConfigFactory.systemEnvironment() should not be null
  }

  "ConfigFactory.systemEnvironmentOverrides()" should "return a non-null Config" in {
    ConfigFactory.systemEnvironmentOverrides() should not be null
  }

  // ── load ───────────────────────────────────────────────────────────────────

  "ConfigFactory.load()" should "load application.conf with YAML includes" in {
    val cfg = ConfigFactory.load()
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("app-key") shouldBe "from-application"
  }

  it should "probe application.yaml before application.conf" in {
    ConfigFactory.load().getString("yaml-app-key") shouldBe "from-application-yaml"
  }

  "ConfigFactory.load(ClassLoader)" should "load application.conf using a given ClassLoader" in {
    val cfg = ConfigFactory.load(getClass.getClassLoader)
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("app-key") shouldBe "from-application"
  }

  "ConfigFactory.load(ConfigParseOptions)" should "load application.conf with custom parse options" in {
    val cfg = ConfigFactory.load(ConfigParseOptions.defaults().setAllowMissing(false))
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("app-key") shouldBe "from-application"
  }

  it should "be idempotent when YamlConfigIncluder is already present" in {
    val opts = ConfigParseOptions.defaults().prependIncluder(YamlConfigIncluder.DEFAULT)
    val cfg = ConfigFactory.load(opts)
    cfg.getString("yaml-key") shouldBe "from-yaml"
  }

  "ConfigFactory.load(ClassLoader, ConfigParseOptions)" should
    "load application.conf with ClassLoader and parse options" in {
      val cfg = ConfigFactory.load(getClass.getClassLoader, ConfigParseOptions.defaults())
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("app-key") shouldBe "from-application"
    }

  "ConfigFactory.load(ClassLoader, ConfigResolveOptions)" should
    "load application.conf with ClassLoader and resolve options" in {
      val cfg = ConfigFactory.load(getClass.getClassLoader, com.typesafe.config.ConfigResolveOptions.defaults())
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("app-key") shouldBe "from-application"
    }

  "ConfigFactory.load(ConfigParseOptions, ConfigResolveOptions)" should
    "load application.conf with parse and resolve options" in {
      val cfg = ConfigFactory.load(ConfigParseOptions.defaults(), com.typesafe.config.ConfigResolveOptions.defaults())
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("app-key") shouldBe "from-application"
    }

  "ConfigFactory.load(ClassLoader, ConfigParseOptions, ConfigResolveOptions)" should
    "load application.conf with all options" in {
      val cfg = ConfigFactory.load(
        getClass.getClassLoader,
        ConfigParseOptions.defaults(),
        com.typesafe.config.ConfigResolveOptions.defaults()
      )
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("app-key") shouldBe "from-application"
    }

  "ConfigFactory.load(String)" should "load a named resource basename" in {
    val cfg = ConfigFactory.load("cf-main")
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  it should "probe basename.yaml before basename.conf" in {
    val cfg = ConfigFactory.load("app-yaml-priority")
    cfg.getString("source") shouldBe "yaml"
    cfg.getString("yaml-priority-key") shouldBe "only-in-yaml"
    cfg.getString("conf-priority-key") shouldBe "only-in-conf"
  }

  "ConfigFactory.load(ClassLoader, String)" should "load a named resource basename with ClassLoader" in {
    val cfg = ConfigFactory.load(getClass.getClassLoader, "cf-main")
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigFactory.load(String, ConfigParseOptions, ConfigResolveOptions)" should "load a named resource with options" in {
    val cfg = ConfigFactory.load(
      "cf-main",
      ConfigParseOptions.defaults(),
      com.typesafe.config.ConfigResolveOptions.defaults()
    )
    cfg.getString("yaml-key") shouldBe "from-yaml"
    cfg.getString("conf-key") shouldBe "from-cf-main"
  }

  "ConfigFactory.load(ClassLoader, String, ConfigParseOptions, ConfigResolveOptions)" should
    "load a named resource with all options" in {
      val cfg = ConfigFactory.load(
        getClass.getClassLoader,
        "cf-main",
        ConfigParseOptions.defaults(),
        com.typesafe.config.ConfigResolveOptions.defaults()
      )
      cfg.getString("yaml-key") shouldBe "from-yaml"
      cfg.getString("conf-key") shouldBe "from-cf-main"
    }

  "ConfigFactory.load(Config)" should "resolve an already-parsed config" in {
    val parsed = ConfigFactory.parseString("load-key = load-value")
    val cfg = ConfigFactory.load(parsed)
    cfg.getString("load-key") shouldBe "load-value"
  }

  "ConfigFactory.load(ClassLoader, Config)" should "resolve an already-parsed config with a ClassLoader" in {
    val parsed = ConfigFactory.parseString("load-key = load-value")
    val cfg = ConfigFactory.load(getClass.getClassLoader, parsed)
    cfg.getString("load-key") shouldBe "load-value"
  }

  "ConfigFactory.load(Config, ConfigResolveOptions)" should "resolve an already-parsed config with resolve options" in {
    val parsed = ConfigFactory.parseString("load-key = load-value")
    val cfg = ConfigFactory.load(parsed, com.typesafe.config.ConfigResolveOptions.defaults())
    cfg.getString("load-key") shouldBe "load-value"
  }

  "ConfigFactory.load(ClassLoader, Config, ConfigResolveOptions)" should
    "resolve an already-parsed config with all options" in {
      val parsed = ConfigFactory.parseString("load-key = load-value")
      val cfg = ConfigFactory.load(
        getClass.getClassLoader,
        parsed,
        com.typesafe.config.ConfigResolveOptions.defaults()
      )
      cfg.getString("load-key") shouldBe "load-value"
    }

  // ── parseResources (YAML direct) ───────────────────────────────────────────

  "ConfigFactory.parseResources(String) with .yaml" should "parse a YAML classpath resource directly" in {
    val cfg = ConfigFactory.parseResources("test-resource.yaml")
    cfg.getString("resource") shouldBe "ok"
  }

  it should "parse a YAML classpath resource directly with explicit options (null classloader)" in {
    val cfg = ConfigFactory.parseResources("test-resource.yaml", ConfigParseOptions.defaults())
    cfg.getString("resource") shouldBe "ok"
  }

  it should "parse a YAML classpath resource directly with explicit options (non-null classloader)" in {
    val opts = ConfigParseOptions.defaults().setClassLoader(getClass.getClassLoader)
    val cfg = ConfigFactory.parseResources("test-resource.yaml", opts)
    cfg.getString("resource") shouldBe "ok"
  }

  "ConfigFactory.parseResources(Class, String) with .yaml" should "parse a YAML classpath resource directly" in {
    val cfg = ConfigFactory.parseResources(getClass, "/test-resource.yaml")
    cfg.getString("resource") shouldBe "ok"
  }

  it should "parse a YAML classpath resource directly with explicit options" in {
    val cfg = ConfigFactory.parseResources(getClass, "/test-resource.yaml", ConfigParseOptions.defaults())
    cfg.getString("resource") shouldBe "ok"
  }

  "ConfigFactory.parseResources(ClassLoader, String) with .yaml" should "parse a YAML classpath resource directly" in {
    val cfg = ConfigFactory.parseResources(getClass.getClassLoader, "test-resource.yaml")
    cfg.getString("resource") shouldBe "ok"
  }

  it should "parse a YAML classpath resource directly with explicit options" in {
    val cfg = ConfigFactory.parseResources(getClass.getClassLoader, "test-resource.yaml", ConfigParseOptions.defaults())
    cfg.getString("resource") shouldBe "ok"
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private def writeTemp(content: String): File = {
    val f = Files.createTempFile("cf-spec", ".yaml").toFile
    Files.write(f.toPath, content.getBytes(StandardCharsets.UTF_8))
    f
  }
}
