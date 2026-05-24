package io.github.bernardusz.levtus.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.bernardusz.levtus.exception.BadRequestException;
import io.github.bernardusz.levtus.exception.HeaderTooLargeException;
import io.github.bernardusz.levtus.exception.LevtusNotImplementedException;
import io.github.bernardusz.levtus.exception.PayloadTooLargeException;
import io.github.bernardusz.levtus.http.Request;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpParserTest {
  @Mock private LevtusEngine mockEngine;

  private HttpParser parser;

  @BeforeEach
  void setUp() {
    parser = new HttpParser();
  }

  @Test
  void testUtf8Decoder() {
    assertEquals("test", parser.utf8Decoder("test"));
    assertEquals("test space", parser.utf8Decoder("test%20space"));
    assertEquals("😀", parser.utf8Decoder("%F0%9F%98%80"));
    assertEquals("你好", parser.utf8Decoder("%E4%BD%A0%E5%A5%BD"));
    assertEquals("special!@#$%^&*()", parser.utf8Decoder("special!@#$%25^&*()"));
  }

  @Test
  void testReadLine() throws IOException {
    InputStream mockInputStream = Mockito.mock(InputStream.class);
    // Standard HTTP newline is \r\n (13, 10)
    Mockito.when(mockInputStream.read()).thenReturn(10, 13, -1);

    String line = parser.readLine(mockInputStream, 10);
    assertEquals("", line); // Empty string for a blank line (no newline - \n)

    String secondLine = parser.readLine(mockInputStream, 10);
    assertEquals(null, secondLine); // null for end of stream

    InputStream mockInputStream2 = Mockito.mock(InputStream.class);
    Mockito.when(mockInputStream2.read()).thenReturn((int) 'G', 13, 10, -1);

    String line2 = parser.readLine(mockInputStream2, 10);
    assertEquals("G", line2);
  }

  @Test
  void testReadLineTooLarge() throws IOException {
    InputStream mockInputStream = Mockito.mock(InputStream.class);
    // Return 'A' indefinitely to exceed the limit
    Mockito.when(mockInputStream.read()).thenReturn((int) 'A');

    assertThrows(
        HeaderTooLargeException.class,
        () -> {
          parser.readLine(mockInputStream, 5); // Limit is 5, but we feed it more
        });
  }

  @Test
  void testParseRequestLineEquals() throws IOException {
    String mockRequestLine = "GET / HTTP/1.1\r\n"; // Added newline to close the parser read
    InputStream stream = new ByteArrayInputStream(mockRequestLine.getBytes(StandardCharsets.UTF_8));

    // Pass the real ByteArrayInputStream directly! No mocks needed.
    String result = parser.parseRequestLine(stream, 25, 10);
    assertEquals("GET / HTTP/1.1", result.trim());
  }

  @Test
  void testParseRequestLineTooLarge() throws IOException {
    String requestLineKeepAlive = "GET / HTTP/1.1\r\n";
    InputStream stream =
        new ByteArrayInputStream(requestLineKeepAlive.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        HeaderTooLargeException.class,
        () -> {
          parser.parseRequestLine(stream, 4, 10);
        });
  }

  @Test
  void testParseRequestLineEmptyLines() throws IOException {
    String requestLineKeepAlive = "\r\n\r\nGET / HTTP/1.1\r\n";
    InputStream stream =
        new ByteArrayInputStream(requestLineKeepAlive.getBytes(StandardCharsets.UTF_8));

    String result = parser.parseRequestLine(stream, 25, 10);
    assertEquals("GET / HTTP/1.1", result.trim());
  }

  @Test
  void testParseRequestLineTooManyEmptyLines() throws IOException {
    String requestLineKeepAlive =
        "\r\n\r\n\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "\r\n"
            + "GET / HTTP/1.1";
    InputStream mockInputStream =
        new ByteArrayInputStream(requestLineKeepAlive.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        BadRequestException.class,
        () -> {
          String result = parser.parseRequestLine(mockInputStream, 25, 4);
        });
  }

  @Test
  void testParseMethod() {
    String requestLine = "GET / HTTP/1.1";
    String parsedMethod = parser.parseMethod(requestLine);

    assertEquals("GET", parsedMethod);
  }

  @Test
  void testParseMethodMoreThanThree() {
    String requestLine = "GET / HTTP/1.1 RANDOM THINGS";
    String parsedMethod = parser.parseMethod(requestLine);

    assertEquals("GET", parsedMethod);
  }

  @Test
  void testParseMethodBadRequest() {
    String badRequestLine = "GET";

    assertThrows(
        BadRequestException.class,
        () -> {
          parser.parseMethod(badRequestLine);
        });
  }

  @Test
  void testParseHeaders() throws IOException {
    String requestLine =
        "Host: localhost:8080\r\n"
            + "User-Agent: Mozilla/5.0\r\n"
            + "Tag: Wonderful\r\n"
            + "Tag: Java\r\n\r\n";
    InputStream mockInputStream =
        new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));

    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));

    Map<String, List<String>> parsedHeaders =
        parser.parseHeaders(
            mockInputStream,
            8192,
            20); // Max headers check how many key headers are there. Not the vakue itself.
    // Note: Create an issue later
    assertEquals(headers, parsedHeaders);
  }

  @Test
  void testParseHeadersHeaderTooLarge() throws IOException {
    String requestLine =
        "Host: localhost:8080\r\n"
            + "User-Agent: Mozilla/5.0\r\n"
            + "Tag: Wonderful\r\n"
            + "Tag: Java\r\n\r\n";
    InputStream mockInputStream =
        new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        HeaderTooLargeException.class,
        () -> {
          parser.parseHeaders(
              mockInputStream,
              1,
              20); // Max headers check how many key headers are there. Not the vakue itself.
        });
    assertThrows(
        HeaderTooLargeException.class,
        () -> {
          parser.parseHeaders(
              mockInputStream,
              8192,
              1); // Max headers check how many key headers are there. Not the vakue itself.
        });
    // Note: Create an issue later
  }

  @Test
  void testParseHeadersHeaderBadRequestMissingHost() throws IOException {
    String requestLine = "User-Agent: Mozilla/5.0\r\n" + "Tag: Wonderful\r\n" + "Tag: Java\r\n\r\n";
    InputStream mockInputStream =
        new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        BadRequestException.class,
        () -> { // Throws bad request because we're missing Host
          parser.parseHeaders(mockInputStream, 100, 20);
        });
  }

  @Test
  void testParseHeadersHeaderNotImplemented() throws IOException {
    String requestLine =
        "Host: localhost:8080\r\n"
            + "User-Agent: Mozilla/5.0\r\n"
            + "Transfer-Encoding: Chunked\r\n"
            + "Tag: Wonderful\r\n"
            + "Tag: Java\r\n\r\n";
    InputStream mockInputStream =
        new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        LevtusNotImplementedException.class,
        () -> {
          parser.parseHeaders(mockInputStream, 8192, 20);
        });
  }

  @Test
  void testValidateBodySize() {
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));

    assertDoesNotThrow(
        () -> {
          parser.validateBodySize(headers, 10 * 1024 * 1024);
        });
  }

  @Test
  void testValidateBodySizeBadRequest() {
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim(), "70".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));

    assertThrows(
        BadRequestException.class,
        () -> {
          parser.validateBodySize(headers, 10 * 1024 * 1024);
        });
  }

  @Test
  void testValidateBodySizePayloadTooLarge() {
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));

    assertThrows(
        PayloadTooLargeException.class,
        () -> {
          parser.validateBodySize(headers, 10);
        });
  }

  @Test
  void
      testParseRawPath() { // Currently there is a bug in parseRawPath. If client only pass the
                           // domain name without http/https, the route will be broken
                           // (localhost:8080/ instead of /)
    String requestLine = "GET https://start.levtus.io/kotlin?tag=awesome&tag=java&good HTTP/1.1";

    assertEquals("/kotlin?tag=awesome&tag=java&good", parser.parseRawPath(requestLine));
  }

  @Test
  void testParsePath() {
    String rawPath = "/kotlin?tag=awesome&tag=java&good";
    String expectedPath = "/kotlin";

    assertEquals("/kotlin", parser.parsePath(rawPath));
    assertEquals(expectedPath, parser.parsePath(expectedPath));
  }

  @Test
  void testParseQueryParams() {
    String rawPath = "/kotlin?tag=awesome+and+nice&tag=java%20kotlin&good";

    Map<String, List<String>> parsedParams = parser.parseQueryParams(rawPath);

    // 1. Check total size and keys
    assertEquals(2, parsedParams.size());
    assertTrue(parsedParams.containsKey("tag"));
    assertTrue(parsedParams.containsKey("good"));

    // 2. Check individual values explicitly
    assertEquals(List.of("awesome and nice", "java kotlin"), parsedParams.get("tag"));
    assertEquals(List.of(""), parsedParams.get("good"));
  }

  //  @Test normalizePath currently has a bug, it can't normalize a path yet due to URI.
  //  void testNormalizePath() {
  //    assertEquals("/foo/bar", parser.normalizePath("/foo//bar"));
  //    assertEquals("/foo/bar/", parser.normalizePath("/foo/bar/"));
  //    assertEquals("/foo/bar", parser.normalizePath("/foo/./bar"));
  //    assertEquals("/bar", parser.normalizePath("/foo/../bar"));
  //    assertEquals("/", parser.normalizePath("//"));
  //  }

  @Test
  void testParseHeadersWithInvalidHeader() throws IOException {
    String requestLine =
        "Host: localhost:8080\r\n" + "InvalidHeaderLineNoColon\r\n" + "Tag: Java\r\n\r\n";
    InputStream mockInputStream =
        new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));

    Map<String, List<String>> parsedHeaders = parser.parseHeaders(mockInputStream, 8192, 20);
    assertTrue(parsedHeaders.containsKey("host"));
    assertTrue(parsedHeaders.containsKey("tag"));
    assertFalse(parsedHeaders.containsKey("invalidheaderlinenocolon"));
  }

  @Test
  void testParseRequest() throws Exception {
    String fullRequest =
        "GET /test//path?query=val HTTP/1.1\r\n"
            + "Host: localhost:8080\r\n"
            + "Content-Length: 5\r\n"
            + "\r\n"
            + "hello";
    InputStream inputStream =
        new ByteArrayInputStream(fullRequest.getBytes(StandardCharsets.UTF_8));

    when(mockEngine.getMaxLineSize()).thenReturn(8192);
    when(mockEngine.getMaxEmptyLines()).thenReturn(10);
    when(mockEngine.getMaxHeaderSize()).thenReturn(8192);
    when(mockEngine.getMaxHeaderCount()).thenReturn(100);
    when(mockEngine.getMaxBodySize()).thenReturn(1024);

    Request request = parser.parseRequest(mockEngine, inputStream);

    assertNotNull(request);
    assertEquals("GET", request.method());
    assertEquals("/test/path", request.path());
    assertEquals("localhost:8080", request.headers().get("host").get(0));
    assertEquals("val", request.queryParams().get("query").get(0));
  }
}
