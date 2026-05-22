package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
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
  void testMultiValueQueryParams() {
    Map<String, List<String>> queryParams =
        Map.of(
            "tag", List.of("java", "web"),
            "id", List.of("123"));

    Request request =
        new Request(
            "GET", "/test", Map.of(), queryParams, new ByteArrayInputStream(new byte[0]), 1024);

    assertEquals(List.of("java", "web"), request.query("tag"));
    assertEquals(List.of("123"), request.query("id"));
    assertEquals(List.of(), request.query("nonexistent"));
    assertEquals(queryParams, request.queryParams());
  }

  @Test
  void testHeadersCaseInsensitivity() {
    Map<String, List<String>> headers =
        Map.of(
            "content-type", List.of("application/json"),
            "x-custom", List.of("value1", "value2"));

    Request request =
        new Request("GET", "/", headers, Map.of(), new ByteArrayInputStream(new byte[0]), 1024);

    assertEquals(List.of("application/json"), request.getHeaders("Content-Type"));
    assertEquals(List.of("application/json"), request.getHeaders("content-type"));
    assertEquals(List.of("value1", "value2"), request.getHeaders("X-CUSTOM"));
    assertEquals("application/json", request.contentType());
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
}
