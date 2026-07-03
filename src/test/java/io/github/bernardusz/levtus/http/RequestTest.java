package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import io.github.bernardusz.levtus.exception.developer.BodyAlreadyConsumedException;
import org.junit.jupiter.api.Test;

class RequestTest {

  @Test
  void testBasicGetters() {
    Request request =
        new Request(
            "GET", "/path", Map.of(), Map.of(), new ByteArrayInputStream(new byte[0]), 1024);

    assertEquals("GET", request.method());
    assertEquals("/path", request.path());
  }

  @Test
  void testSingleValueQueries() {
    Map<String, List<String>> queries =
        Map.of(
            "tag", List.of("java", "web"),
            "id", List.of("123"));

    Request request =
        new Request("GET", "/test", Map.of(), queries, new ByteArrayInputStream(new byte[0]), 1024);

    assertEquals("java", request.query("tag"));
    assertEquals("123", request.query("id"));
    assertEquals("", request.query("nonexistent"));
  }

  @Test
  void testMultiValueQueries() {
    Map<String, List<String>> queries =
        Map.of(
            "tag", List.of("java", "web"),
            "id", List.of("123"));

    Request request =
        new Request("GET", "/test", Map.of(), queries, new ByteArrayInputStream(new byte[0]), 1024);

    assertEquals(List.of("java", "web"), request.queries("tag"));
    assertEquals(List.of("123"), request.queries("id"));
    assertEquals(List.of(), request.queries("nonexistent"));
    assertEquals(queries, request.queries());
  }

  @Test
  void testSingleHeaderValue() {
    Map<String, List<String>> headers =
        Map.of(
            "content-type", List.of("application/json"),
            "x-custom", List.of("value1", "value2"));

    Request request =
        new Request("GET", "/test", headers, Map.of(), new ByteArrayInputStream(new byte[0]), 1024);

    assertEquals("application/json", request.header("Content-Type"));
    assertEquals("value1", request.header("X-Custom"));
    assertEquals("", request.header("Nonexistent"));
  }

  @Test
  void testHeadersCaseInsensitivity() {
    Map<String, List<String>> headers =
        Map.of(
            "content-type", List.of("application/json"),
            "x-custom", List.of("value1", "value2"));

    Request request =
        new Request("GET", "/", headers, Map.of(), new ByteArrayInputStream(new byte[0]), 1024);

    assertEquals(List.of("application/json"), request.headers("Content-Type"));
    assertEquals(List.of("application/json"), request.headers("content-type"));
    assertEquals(List.of("value1", "value2"), request.headers("X-CUSTOM"));
    assertEquals("application/json", request.contentType());
    assertEquals(headers, request.headers());
  }

  @Test
  void testContentLengthParsing() {
    Request request =
        new Request(
            "POST",
            "/",
            Map.of("content-length", List.of("512")),
            Map.of(),
            new ByteArrayInputStream(new byte[0]),
            1024);
    assertEquals(512, request.contentLength());

    Request invalidRequest =
        new Request(
            "POST",
            "/",
            Map.of("content-length", List.of("abc")),
            Map.of(),
            new ByteArrayInputStream(new byte[0]),
            1024);
    assertEquals(0, invalidRequest.contentLength());
  }

  @Test
  void testBodyReadingAndCaching() {
    byte[] data = "Hello Levtus".getBytes();
    Request request =
        new Request(
            "POST",
            "/msg",
            Map.of("content-length", List.of(String.valueOf(data.length))),
            Map.of(),
            new ByteArrayInputStream(data),
            1024);

    assertFalse(request.isCached());
    assertArrayEquals(data, request.body());
    assertTrue(request.isCached());
    assertArrayEquals(data, request.body(), "Should return cached data on subsequent calls");
  }

  @Test
  void testBodyReadingWithBodyStream() throws IOException {
    byte[] data = "Hello Levtus".getBytes();
    Request request =
        new Request(
            "POST",
            "/msg",
            Map.of("content-length", List.of(String.valueOf(data.length))),
            Map.of(),
            new ByteArrayInputStream(data),
            1024);

    assertFalse(request.isCached(), "Should not be cached immediately");
    try (InputStream stream = request.bodyStream()) {
      assertNotNull(stream, "Stream should not be null");
      byte[] readData = stream.readAllBytes();
      assertArrayEquals(data, readData, "Stream should contain the original data");
    }
    assertFalse(request.isCached());
  }

  @Test
  void testThrowBodyAlreadyConsumed() throws IOException{
    byte[] data = "Hello Levtus".getBytes();
    Request request =
        new Request(
            "POST",
            "/msg",
            Map.of("content-length", List.of(String.valueOf(data.length))),
            Map.of(),
            new ByteArrayInputStream(data),
            1024);

    assertFalse(request.isCached(), "Should not be cached immediately");
    try (InputStream stream = request.bodyStream()) {
      assertNotNull(stream, "Stream should not be null");
      byte[] readData = stream.readAllBytes();
      assertArrayEquals(data, readData, "Stream should contain the original data");
    }
    assertFalse(request.isCached());

    assertThrows(BodyAlreadyConsumedException.class, () -> request.bodyStream());
    assertThrows(BodyAlreadyConsumedException.class, () -> request.body());
    assertThrows(BodyAlreadyConsumedException.class, () -> request.bodyAsString());
  }

  @Test
  void testBodyAlreadyConsumedCached(){
    byte[] data = "Hello Levtus".getBytes();
    Request request =
        new Request(
            "POST",
            "/msg",
            Map.of("content-length", List.of(String.valueOf(data.length))),
            Map.of(),
            new ByteArrayInputStream(data),
            1024);

    assertFalse(request.isCached());
    assertArrayEquals(data, request.body());
    assertTrue(request.isCached());
    assertArrayEquals(data, request.body(), "Should return cached data on subsequent calls");

    assertThrows(BodyAlreadyConsumedException.class, () -> request.bodyStream());
  }
}
