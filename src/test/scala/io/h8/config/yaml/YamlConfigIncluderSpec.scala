package io.h8.config.yaml

import com.typesafe.config.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.List as JList
import scala.jdk.CollectionConverters.*

class YamlConfigIncluderSpec extends AnyFlatSpec with Matchers {

  private val origin = ConfigOriginFactory.newSimple("test")

  private def ctx(allowMissing: Boolean = true): ConfigIncludeContext =
    new ConfigIncludeContext {
      def relativeTo(filename: String): ConfigParseable = null
      def parseOptions(): ConfigParseOptions = ConfigParseOptions.defaults().setAllowMissing(allowMissing)
      def setParseOptions(opts: ConfigParseOptions): ConfigIncludeContext = this
    }

  private def docs(values: ConfigValue*): JList[ConfigValue] = values.toList.asJava

  // ── mergeDocuments ─────────────────────────────────────────────────────────

  "mergeDocuments" should "return empty for an empty list" in {
    YamlConfigIncluder.mergeDocuments(docs(), origin).isEmpty shouldBe true
  }

  it should "skip null documents" in {
    val nullDoc = ConfigValueFactory.fromAnyRef(null)
    YamlConfigIncluder.mergeDocuments(docs(nullDoc), origin).isEmpty shouldBe true
  }

  it should "return the single object document" in {
    val obj = ConfigValueFactory.fromMap(Map("a" -> (1: Integer)).asJava)
    val result = YamlConfigIncluder.mergeDocuments(docs(obj), origin).toConfig
    result.getInt("a") shouldBe 1
  }

  it should "merge two objects with first-wins priority" in {
    val doc1 = ConfigValueFactory.fromMap(Map("a" -> (1: Integer), "b" -> (2: Integer)).asJava)
    val doc2 = ConfigValueFactory.fromMap(Map("a" -> (99: Integer), "c" -> (3: Integer)).asJava)
    val result = YamlConfigIncluder.mergeDocuments(docs(doc1, doc2), origin).toConfig
    result.getInt("a") shouldBe 1 // doc1 wins
    result.getInt("b") shouldBe 2 // only in doc1
    result.getInt("c") shouldBe 3 // only in doc2
  }

  it should "skip nulls between objects" in {
    val doc1 = ConfigValueFactory.fromMap(Map("a" -> (1: Integer)).asJava)
    val nullDoc = ConfigValueFactory.fromAnyRef(null)
    val doc2 = ConfigValueFactory.fromMap(Map("b" -> (2: Integer)).asJava)
    val result = YamlConfigIncluder.mergeDocuments(docs(doc1, nullDoc, doc2), origin).toConfig
    result.getInt("a") shouldBe 1
    result.getInt("b") shouldBe 2
  }

  it should "throw on a list document" in {
    val list = ConfigValueFactory.fromIterable(List[Integer](1, 2).asJava)
    a[ConfigException.Parse] should be thrownBy
      YamlConfigIncluder.mergeDocuments(docs(list), origin)
  }

  it should "throw on a scalar document" in {
    val scalar = ConfigValueFactory.fromAnyRef(42: Integer)
    a[ConfigException.Parse] should be thrownBy
      YamlConfigIncluder.mergeDocuments(docs(scalar), origin)
  }

  it should "throw when a list appears among objects" in {
    val obj = ConfigValueFactory.fromMap(Map("a" -> (1: Integer)).asJava)
    val list = ConfigValueFactory.fromIterable(List[Integer](1).asJava)
    a[ConfigException.Parse] should be thrownBy
      YamlConfigIncluder.mergeDocuments(docs(obj, list), origin)
  }

  // ── includeFile ────────────────────────────────────────────────────────────

  "includeFile" should "parse a YAML file" in {
    val file = writeTemp("x: 10\ny: 20")
    try {
      val result = YamlConfigIncluder.DEFAULT.includeFile(ctx(), file).toConfig
      result.getInt("x") shouldBe 10
      result.getInt("y") shouldBe 20
    } finally file.delete(): Unit
  }

  it should "merge multi-document YAML, first wins" in {
    val file = writeTemp("a: 1\n---\na: 99\nb: 2")
    try {
      val result = YamlConfigIncluder.DEFAULT.includeFile(ctx(), file).toConfig
      result.getInt("a") shouldBe 1
      result.getInt("b") shouldBe 2
    } finally file.delete(): Unit
  }

  it should "return empty for an empty file" in {
    val file = writeTemp("")
    try
      YamlConfigIncluder.DEFAULT.includeFile(ctx(), file).isEmpty shouldBe true
    finally file.delete(): Unit
  }

  it should "return empty for a null-only document" in {
    val file = writeTemp("null")
    try
      YamlConfigIncluder.DEFAULT.includeFile(ctx(), file).isEmpty shouldBe true
    finally file.delete(): Unit
  }

  it should "throw on a list root" in {
    val file = writeTemp("- 1\n- 2")
    try
      a[ConfigException.Parse] should be thrownBy YamlConfigIncluder.DEFAULT.includeFile(ctx(), file)
    finally file.delete(): Unit
  }

  it should "return empty for missing file when allowMissing=true" in {
    YamlConfigIncluder.DEFAULT
      .includeFile(ctx(allowMissing = true), new File("/no/such/file.yaml"))
      .isEmpty shouldBe true
  }

  it should "throw for missing file when allowMissing=false" in {
    a[ConfigException.IO] should be thrownBy
      YamlConfigIncluder.DEFAULT.includeFile(ctx(allowMissing = false), new File("/no/such/file.yaml"))
  }

  it should "delegate non-YAML files to fallback" in {
    val called = new java.util.concurrent.atomic.AtomicBoolean(false)
    val fb = new ConfigIncluder with ConfigIncluderFile {
      def withFallback(fb: ConfigIncluder): ConfigIncluder = this
      def include(context: ConfigIncludeContext, what: String): ConfigObject = ConfigFactory.empty().root()
      def includeFile(context: ConfigIncludeContext, f: File): ConfigObject = {
        called.set(true)
        ConfigFactory.empty().root()
      }
    }
    val file = new File("config.conf")
    YamlConfigIncluder.DEFAULT.withFallback(fb).asInstanceOf[ConfigIncluderFile].includeFile(ctx(), file)
    called.get() shouldBe true
  }

  // ── includeResources ───────────────────────────────────────────────────────

  "includeResources" should "load a classpath YAML resource" in {
    val result = YamlConfigIncluder.DEFAULT
      .includeResources(ctx(), "test-resource.yaml").toConfig
    result.getString("resource") shouldBe "ok"
  }

  it should "return empty for missing resource when allowMissing=true" in {
    YamlConfigIncluder.DEFAULT
      .includeResources(ctx(allowMissing = true), "no-such.yaml")
      .isEmpty shouldBe true
  }

  it should "throw for missing resource when allowMissing=false" in {
    a[ConfigException.IO] should be thrownBy
      YamlConfigIncluder.DEFAULT.includeResources(ctx(allowMissing = false), "no-such.yaml")
  }

  it should "delegate non-YAML resources to fallback" in {
    val called = new java.util.concurrent.atomic.AtomicBoolean(false)
    val fb = fallbackClasspathIncluder(called)
    YamlConfigIncluder.DEFAULT.withFallback(fb).asInstanceOf[ConfigIncluderClasspath].includeResources(
      ctx(), "something.conf")
    called.get() shouldBe true
  }

  // ── includeURL ─────────────────────────────────────────────────────────────

  "includeURL" should "parse a YAML file URL" in {
    val file = writeTemp("key: value")
    try {
      val result = YamlConfigIncluder.DEFAULT
        .includeURL(ctx(), file.toURI.toURL).toConfig
      result.getString("key") shouldBe "value"
    } finally file.delete(): Unit
  }

  it should "delegate non-YAML URLs to fallback" in {
    val called = new java.util.concurrent.atomic.AtomicBoolean(false)
    val fb = fallbackURLIncluder(called)
    val url = java.net.URI.create("file:///some/path/config.conf").toURL
    YamlConfigIncluder.DEFAULT.withFallback(fb).asInstanceOf[ConfigIncluderURL].includeURL(ctx(), url)
    called.get() shouldBe true
  }

  it should "accept .yml extension" in {
    val file = writeTemp("v: 1").toPath
    val ymlFile = file.resolveSibling(file.getFileName.toString.replace(".yaml", ".yml")).toFile
    Files.copy(file.toFile.toPath, ymlFile.toPath)
    try
      YamlConfigIncluder.DEFAULT.includeFile(ctx(), ymlFile).toConfig.getInt("v") shouldBe 1
    finally { ymlFile.delete(): Unit; file.toFile.delete(): Unit }
  }

  it should "return empty when non-ConfigIncluderFile fallback is set" in {
    val fb = fallbackIncluder(new java.util.concurrent.atomic.AtomicBoolean())
    YamlConfigIncluder.DEFAULT.withFallback(fb)
      .asInstanceOf[ConfigIncluderFile].includeFile(ctx(), new File("config.conf"))
      .isEmpty shouldBe true
  }

  it should "return empty when non-ConfigIncluderClasspath fallback is set" in {
    val fb = fallbackIncluder(new java.util.concurrent.atomic.AtomicBoolean())
    YamlConfigIncluder.DEFAULT.withFallback(fb)
      .asInstanceOf[ConfigIncluderClasspath].includeResources(ctx(), "config.conf")
      .isEmpty shouldBe true
  }

  it should "return empty when non-ConfigIncluderURL fallback is set" in {
    val fb = fallbackIncluder(new java.util.concurrent.atomic.AtomicBoolean())
    val url = java.net.URI.create("file:///some/path/config.conf").toURL
    YamlConfigIncluder.DEFAULT.withFallback(fb)
      .asInstanceOf[ConfigIncluderURL].includeURL(ctx(), url)
      .isEmpty shouldBe true
  }

  // ── include (generic) ──────────────────────────────────────────────────────

  "include" should "load a YAML classpath resource by name" in {
    val result = YamlConfigIncluder.DEFAULT
      .include(ctx(), "test-resource.yaml").toConfig
    result.getString("resource") shouldBe "ok"
  }

  it should "load a YAML file from the file system" in {
    val file = writeTemp("fs: true")
    try {
      val result = YamlConfigIncluder.DEFAULT.include(ctx(), file.getAbsolutePath).toConfig
      result.getBoolean("fs") shouldBe true
    } finally file.delete(): Unit
  }

  it should "return empty for missing YAML when allowMissing=true" in {
    YamlConfigIncluder.DEFAULT
      .include(ctx(allowMissing = true), "/no/such/file.yaml")
      .isEmpty shouldBe true
  }

  it should "throw for missing YAML when allowMissing=false" in {
    a[ConfigException.IO] should be thrownBy
      YamlConfigIncluder.DEFAULT.include(ctx(allowMissing = false), "/no/such/file.yaml")
  }

  it should "delegate non-YAML names to fallback" in {
    val called = new java.util.concurrent.atomic.AtomicBoolean(false)
    val fb = fallbackIncluder(called)
    YamlConfigIncluder.DEFAULT.withFallback(fb).include(ctx(), "application.conf")
    called.get() shouldBe true
  }

  it should "return empty for non-YAML names when no fallback is set" in {
    val includer = new YamlConfigIncluder(YamlConfigFactory.DEFAULT)
    includer.include(ctx(), "application.conf").isEmpty shouldBe true
  }

  it should "use context classloader when parseOptions returns null loader" in {
    val ctxNullLoader = new ConfigIncludeContext {
      def relativeTo(f: String): ConfigParseable = null
      def parseOptions(): ConfigParseOptions =
        ConfigParseOptions.defaults().setClassLoader(null).setAllowMissing(true)
      def setParseOptions(o: ConfigParseOptions): ConfigIncludeContext = this
    }
    // Should not throw — falls back to context classloader
    YamlConfigIncluder.DEFAULT.include(ctxNullLoader, "test-resource.yaml").toConfig
      .getString("resource") shouldBe "ok"
  }

  // ── withFallback ───────────────────────────────────────────────────────────

  "withFallback" should "return same instance when fallback is unchanged" in {
    val fb = fallbackIncluder(new java.util.concurrent.atomic.AtomicBoolean())
    val includer = YamlConfigIncluder.DEFAULT.withFallback(fb)
    includer.withFallback(fb) should be theSameInstanceAs includer
  }

  it should "chain fallbacks" in {
    val first = fallbackIncluder(new java.util.concurrent.atomic.AtomicBoolean())
    val second = fallbackIncluder(new java.util.concurrent.atomic.AtomicBoolean())
    val includer = YamlConfigIncluder.DEFAULT.withFallback(first).withFallback(second)
    includer should not be theSameInstanceAs(YamlConfigIncluder.DEFAULT)
  }

  // ── custom factory ────────────────────────────────────────────────────────

  "YamlConfigIncluder(YamlConfigFactory)" should "use the provided factory's depth limit" in {
    val shallow = new YamlConfigIncluder(YamlConfigFactory.builder().maxDepth(1).build())
    val file = writeTemp("a:\n  b: 1")
    try
      a[ConfigException.Parse] should be thrownBy shallow.includeFile(ctx(), file)
    finally file.delete(): Unit
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private def writeTemp(content: String): File = {
    val f = Files.createTempFile("yaml-includer-test", ".yaml").toFile
    Files.write(f.toPath, content.getBytes(StandardCharsets.UTF_8))
    f
  }

  private def fallbackIncluder(called: java.util.concurrent.atomic.AtomicBoolean): ConfigIncluder =
    new ConfigIncluder {
      def withFallback(fb: ConfigIncluder): ConfigIncluder = this
      def include(context: ConfigIncludeContext, what: String): ConfigObject = {
        called.set(true)
        ConfigFactory.empty().root()
      }
    }

  private def fallbackClasspathIncluder(called: java.util.concurrent.atomic.AtomicBoolean): ConfigIncluder =
    new ConfigIncluder with ConfigIncluderClasspath {
      def withFallback(fb: ConfigIncluder): ConfigIncluder = this
      def include(context: ConfigIncludeContext, what: String): ConfigObject = ConfigFactory.empty().root()
      def includeResources(context: ConfigIncludeContext, resource: String): ConfigObject = {
        called.set(true)
        ConfigFactory.empty().root()
      }
    }

  private def fallbackURLIncluder(called: java.util.concurrent.atomic.AtomicBoolean): ConfigIncluder =
    new ConfigIncluder with ConfigIncluderURL {
      def withFallback(fb: ConfigIncluder): ConfigIncluder = this
      def include(context: ConfigIncludeContext, what: String): ConfigObject = ConfigFactory.empty().root()
      def includeURL(context: ConfigIncludeContext, url: java.net.URL): ConfigObject = {
        called.set(true)
        ConfigFactory.empty().root()
      }
    }
}
