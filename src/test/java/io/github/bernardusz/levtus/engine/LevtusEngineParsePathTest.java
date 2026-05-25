package io.github.bernardusz.levtus.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class LevtusEngineParsePathTest {

  private String simulatePathParsing(String rawPath) {
    if (rawPath.startsWith("http")) {
      rawPath = rawPath.substring(rawPath.indexOf("//") + 2);
      rawPath = rawPath.substring(!rawPath.contains("/") ? 0 : rawPath.indexOf("/"));
      if (rawPath.equals("/") || rawPath.isEmpty()) {
        rawPath = "/";
      }
    } else if (!rawPath.startsWith("/") && !rawPath.equals("*")) {
      if (rawPath.contains("/")) {
        rawPath = rawPath.substring(rawPath.indexOf("/"));
      } else {
        rawPath = "/";
      }
    }
    if (!rawPath.equals("*") && !rawPath.contains("/")) {
      rawPath = "/";
    }
    if (rawPath.contains("?")) {
      rawPath = rawPath.substring(0, rawPath.indexOf("?"));
    }
    return rawPath;
  }

  @Test
  void testHostWithoutSchemeWithPath() {
    assertEquals("/kotlin", simulatePathParsing("start.levtus.io/kotlin?tag=awesome"));
  }

  @Test
  void testHostWithoutSchemeNoPath() {
    assertEquals("/", simulatePathParsing("start.levtus.io"));
  }

  @Test
  void testHttpScheme() {
    assertEquals("/kotlin", simulatePathParsing("http://start.levtus.io/kotlin?tag=awesome"));
  }

  @Test
  void testRootPath() {
    assertEquals("/", simulatePathParsing("/"));
  }

  @Test
  void testAsterisk() {
    assertEquals("*", simulatePathParsing("*"));
  }
}