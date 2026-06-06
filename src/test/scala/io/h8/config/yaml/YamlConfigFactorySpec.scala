package io.h8.config.yaml

import com.typesafe.config.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class YamlConfigFactorySpec extends AnyFlatSpec with Matchers {

  // ── parseString ────────────────────────────────────────────────────────────

  "parseString" should "parse a single mapping document" in {
    val docs = YamlConfigFactory.DEFAULT.parseString("a: 1\nb: hello")
    docs should have size 1
    val cfg = docs.get(0).asInstanceOf[ConfigObject].toConfig
    cfg.getLong("a") shouldBe 1L
    cfg.getString("b") shouldBe "hello"
  }

  it should "parse a sequence root" in {
    val docs = YamlConfigFactory.DEFAULT.parseString("- 1\n- two")
    docs should have size 1
    val list = docs.get(0).asInstanceOf[ConfigList]
    list.get(0).unwrapped() shouldEqual 1L
    list.get(1).unwrapped() shouldBe "two"
  }

  it should "parse a scalar root" in {
    YamlConfigFactory.DEFAULT.parseString("42").get(0).unwrapped() shouldEqual 42L
    YamlConfigFactory.DEFAULT.parseString("hello").get(0).unwrapped() shouldBe "hello"
  }

  it should "return an empty list for an empty stream" in {
    YamlConfigFactory.DEFAULT.parseString("") should have size 0
  }

  it should "return one null document for a bare document marker" in {
    val docs = YamlConfigFactory.DEFAULT.parseString("---")
    docs should have size 1
    docs.get(0).valueType() shouldBe ConfigValueType.NULL
  }

  it should "return one null-typed document for a null YAML document" in {
    val docs = YamlConfigFactory.DEFAULT.parseString("null")
    docs should have size 1
    docs.get(0).valueType() shouldBe ConfigValueType.NULL
  }

  it should "return multiple documents for a multi-document stream" in {
    val docs = YamlConfigFactory.DEFAULT.parseString("a: 1\n---\nb: 2")
    docs should have size 2
    docs.get(0).asInstanceOf[ConfigObject].toConfig.getLong("a") shouldBe 1L
    docs.get(1).asInstanceOf[ConfigObject].toConfig.getLong("b") shouldBe 2L
  }

  it should "handle heterogeneous documents (mapping then scalar)" in {
    val docs = YamlConfigFactory.DEFAULT.parseString("key: val\n---\n42")
    docs should have size 2
    docs.get(0) shouldBe a[ConfigObject]
    docs.get(1).unwrapped() shouldEqual 42L
  }

  it should "throw ConfigException.Parse on invalid YAML" in {
    a[ConfigException.Parse] should be thrownBy YamlConfigFactory.DEFAULT.parseString("{bad yaml: [}")
  }

  it should "parse nested mappings" in {
    val cfg = YamlConfigFactory.DEFAULT.parseString("server:\n  host: localhost\n  port: 8080")
      .get(0).asInstanceOf[ConfigObject].toConfig
    cfg.getString("server.host") shouldBe "localhost"
    cfg.getInt("server.port") shouldBe 8080
  }

  // ── parseFile ──────────────────────────────────────────────────────────────

  "parseFile" should "parse a YAML file" in {
    val file = writeTemp("a: 42\nb: true")
    try {
      val cfg = YamlConfigFactory.DEFAULT.parseFile(file).get(0).asInstanceOf[ConfigObject].toConfig
      cfg.getLong("a") shouldBe 42L
      cfg.getBoolean("b") shouldBe true
    } finally file.delete(): Unit
  }

  it should "respect an explicit Charset" in {
    val file = writeTempBytes("greeting: héllo".getBytes(StandardCharsets.UTF_8))
    try {
      val cfg = YamlConfigFactory.DEFAULT.parseFile(file, StandardCharsets.UTF_8)
        .get(0).asInstanceOf[ConfigObject].toConfig
      cfg.getString("greeting") shouldBe "héllo"
    } finally file.delete(): Unit
  }

  it should "throw ConfigException.IO for a missing file" in {
    a[ConfigException.IO] should be thrownBy
      YamlConfigFactory.DEFAULT.parseFile(new File("/nonexistent/path/file.yaml"))
  }

  it should "return an empty list for an empty file" in {
    val file = writeTemp("")
    try YamlConfigFactory.DEFAULT.parseFile(file) should have size 0
    finally file.delete(): Unit
  }

  // ── parseURL ───────────────────────────────────────────────────────────────

  "parseURL" should "parse a file URL" in {
    val file = writeTemp("x: 99")
    try {
      val cfg = YamlConfigFactory.DEFAULT.parseURL(file.toURI.toURL)
        .get(0).asInstanceOf[ConfigObject].toConfig
      cfg.getLong("x") shouldBe 99L
    } finally file.delete(): Unit
  }

  // ── parseURL error ─────────────────────────────────────────────────────────

  "parseURL" should "throw ConfigException.IO for an unreadable URL" in {
    a[ConfigException.IO] should be thrownBy
      YamlConfigFactory.DEFAULT.parseURL(java.net.URI.create("file:///nonexistent/no.yaml").toURL)
  }

  // ── parseResources ─────────────────────────────────────────────────────────

  "parseResources(Class, String)" should "load a classpath resource" in {
    val docs = YamlConfigFactory.DEFAULT.parseResources(getClass, "/test-resource.yaml")
    docs.get(0).asInstanceOf[ConfigObject].toConfig.getString("resource") shouldBe "ok"
  }

  it should "throw ConfigException.IO when resource is absent" in {
    a[ConfigException.IO] should be thrownBy
      YamlConfigFactory.DEFAULT.parseResources(getClass, "/no-such-file.yaml")
  }

  "parseResources(ClassLoader, String)" should "load a classpath resource" in {
    val docs = YamlConfigFactory.DEFAULT.parseResources(getClass.getClassLoader, "test-resource.yaml")
    docs.get(0).asInstanceOf[ConfigObject].toConfig.getString("resource") shouldBe "ok"
  }

  it should "throw ConfigException.IO when resource is absent" in {
    a[ConfigException.IO] should be thrownBy
      YamlConfigFactory.DEFAULT.parseResources(getClass.getClassLoader, "no-such-file.yaml")
  }

  "parseResources(String)" should "load a classpath resource via context classloader" in {
    val docs = YamlConfigFactory.DEFAULT.parseResources("test-resource.yaml")
    docs.get(0).asInstanceOf[ConfigObject].toConfig.getString("resource") shouldBe "ok"
  }

  // ── parameterized factory ──────────────────────────────────────────────────

  "YamlConfigFactory(maxDepth)" should "throw on exceeded depth" in {
    a[ConfigException.Parse] should be thrownBy
      new YamlConfigFactory(1).parseString("a:\n  b: 1")
  }

  "YamlConfigFactory(LoadSettings)" should "use custom settings" in {
    import org.snakeyaml.engine.v2.api.LoadSettings
    import org.snakeyaml.engine.v2.schema.CoreSchema
    val settings = LoadSettings.builder().setSchema(new CoreSchema()).build()
    val docs = new YamlConfigFactory(settings).parseString("x: 1")
    docs.get(0).asInstanceOf[ConfigObject].toConfig.getInt("x") shouldBe 1
  }

  "YamlConfigFactory(LoadSettings, maxDepth)" should "respect both parameters" in {
    import org.snakeyaml.engine.v2.api.LoadSettings
    import org.snakeyaml.engine.v2.schema.CoreSchema
    val settings = LoadSettings.builder().setSchema(new CoreSchema()).build()
    a[ConfigException.Parse] should be thrownBy
      new YamlConfigFactory(settings, 1).parseString("a:\n  b: 1")
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private def writeTemp(content: String): File =
    writeTempBytes(content.getBytes(StandardCharsets.UTF_8))

  private def writeTempBytes(bytes: Array[Byte]): File = {
    val f = Files.createTempFile("yaml-factory-test", ".yaml").toFile
    Files.write(f.toPath, bytes)
    f
  }
}
