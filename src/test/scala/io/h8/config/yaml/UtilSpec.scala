package io.h8.config.yaml

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI

class UtilSpec extends AnyFlatSpec with Matchers {

  // ── isYaml(String) ─────────────────────────────────────────────────────────

  "Util.isYaml(String)" should "return true for .yaml extension" in {
    Util.isYaml("app.yaml") shouldBe true
  }

  it should "return true for .yml extension" in {
    Util.isYaml("app.yml") shouldBe true
  }

  it should "be case-insensitive" in {
    Util.isYaml("app.YAML") shouldBe true
    Util.isYaml("app.YML") shouldBe true
    Util.isYaml("app.Yaml") shouldBe true
  }

  it should "return false for .conf" in {
    Util.isYaml("app.conf") shouldBe false
  }

  it should "return false for .json" in {
    Util.isYaml("app.json") shouldBe false
  }

  it should "return false for no extension" in {
    Util.isYaml("application") shouldBe false
  }

  it should "return false for a name that merely contains yaml" in {
    Util.isYaml("yaml-config.conf") shouldBe false
  }

  // ── isYaml(URL) ────────────────────────────────────────────────────────────

  "Util.isYaml(URL)" should "return true for a .yaml URL" in {
    Util.isYaml(URI.create("file:///etc/app.yaml").toURL) shouldBe true
  }

  it should "return true for a .yml URL" in {
    Util.isYaml(URI.create("file:///etc/app.yml").toURL) shouldBe true
  }

  it should "return false for a .conf URL" in {
    Util.isYaml(URI.create("file:///etc/app.conf").toURL) shouldBe false
  }

  it should "ignore the query string when checking extension" in {
    Util.isYaml(URI.create("file:///etc/app.yaml?v=1").toURL) shouldBe true
    Util.isYaml(URI.create("file:///etc/app.conf?file=app.yaml").toURL) shouldBe false
  }
}
