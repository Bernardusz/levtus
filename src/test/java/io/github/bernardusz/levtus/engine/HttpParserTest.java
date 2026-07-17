package io.github.bernardusz.levtus.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import io.github.bernardusz.levtus.exception.http.BadRequestException;
import io.github.bernardusz.levtus.exception.http.HeaderTooLargeException;
import io.github.bernardusz.levtus.exception.http.PayloadTooLargeException;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import io.github.bernardusz.levtus.routing.Router;
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
  private HttpConnectionHandler handler;
  private HttpParser parser;
  private Router router;
  @Mock private Response mockResponse;

  @BeforeEach
  void setUp() {
    parser = new HttpParser();
    router = new Router();
    handler = new HttpConnectionHandler(router, parser);
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
    when(mockInputStream.read()).thenReturn(10, 13, -1);

    String line = parser.readLine(mockInputStream, 10);
    assertEquals("", line); // Empty string for a blank line (no newline - \n)

    String secondLine = parser.readLine(mockInputStream, 10);
    assertNull(secondLine); // null for end of stream

    InputStream mockInputStream2 = Mockito.mock(InputStream.class);
    when(mockInputStream2.read()).thenReturn((int) 'G', 13, 10, -1);

    String line2 = parser.readLine(mockInputStream2, 10);
    assertEquals("G", line2);
  }

  @Test
  void testReadLineTooLarge() throws IOException {
    InputStream mockInputStream = Mockito.mock(InputStream.class);
    // Return 'A' indefinitely to exceed the limit
    when(mockInputStream.read()).thenReturn((int) 'A');

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
  void testParseRequestLineTooLarge() {
    String requestLineKeepAlive = "GET / HTTP/1.1\r\n";
    InputStream stream =
        new ByteArrayInputStream(requestLineKeepAlive.getBytes(StandardCharsets.UTF_8));

    assertThrows(HeaderTooLargeException.class, () -> parser.parseRequestLine(stream, 4, 10));
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
  void testParseRequestLineTooManyEmptyLines() {
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

    assertThrows(BadRequestException.class, () -> parser.parseRequestLine(mockInputStream, 25, 4));
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

    assertThrows(BadRequestException.class, () -> parser.parseMethod(requestLine));
  }

  @Test
  void testParseMethodBadRequest() {
    String badRequestLine = "GET";

    assertThrows(BadRequestException.class, () -> parser.parseMethod(badRequestLine));
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
            20); // Max headers check how many key headers are there. Not the value itself.
    // Note: Create an issue later
    assertEquals(headers, parsedHeaders);
  }

  @Test
  void testParseHeadersHeaderTooLarge() {
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
              20); // Max headers check how many key headers are there. Not the value itself.
        });
    assertThrows(
        HeaderTooLargeException.class,
        () -> {
          parser.parseHeaders(
              mockInputStream,
              8192,
              1); // Max headers check how many key headers are there. Not the value itself.
        });
    // Note: Create an issue later
  }

  @Test
  void testParseHeadersHeaderBadRequestMissingHost() {
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
  void testParseHeadersHeaderImplemented() {
    String requestLine =
        "Host: localhost:8080\r\n"
            + "User-Agent: Mozilla/5.0\r\n"
            + "Transfer-Encoding: Chunked\r\n"
            + "Tag: Wonderful\r\n"
            + "Tag: Java\r\n\r\n";
    InputStream mockInputStream =
        new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));

    assertDoesNotThrow(
        () -> parser.parseHeaders(mockInputStream, 8192, 20));
  }

  @Test
  void testValidateBodySize() {
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));

    assertDoesNotThrow(() -> parser.validateBodySize(headers, 10 * 1024 * 1024, mockResponse));
  }

  @Test
  void testValidateBodySizeBadRequest() {
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim(), "70".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));

    assertThrows(
        BadRequestException.class, () -> parser.validateBodySize(headers, 10 * 1024 * 1024, mockResponse));
  }

  @Test
  void testValidateBodySizePayloadTooLarge() {
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));

    assertThrows(PayloadTooLargeException.class, () -> parser.validateBodySize(headers, 10, mockResponse));
  }

  @Test
  void testValidateBodySizePerRoute(){
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));

    assertThrows(PayloadTooLargeException.class, () -> parser.validateBodySize(headers, 10, mockResponse));
  }

  @Test
  void testValidateBodySize100Continue(){
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));
    headers.put("Expect".toLowerCase().trim(), List.of("100-continue".trim()));

    when(mockResponse.status(anyInt())).thenReturn(mockResponse);

    assertDoesNotThrow(() -> parser.validateBodySize(headers, 20, mockResponse));
    verify(mockResponse).status(100);
    verify(mockResponse).send();
  }

  @Test
  void testValidateBodySize100ContinueNotCalled(){
    Map<String, List<String>> headers = new HashMap<>();

    headers.put("Host".toLowerCase().trim(), List.of("localhost:8080".trim()));
    headers.put("User-Agent".toLowerCase().trim(), List.of("Mozilla/5.0".trim()));
    headers.put("Content-Length".toLowerCase().trim(), List.of("20".trim()));
    headers.put("Tag".toLowerCase().trim(), List.of("Wonderful".trim(), "Java".trim()));
    headers.put("Expect".toLowerCase().trim(), List.of("100-continue".trim()));

    assertThrows(PayloadTooLargeException.class, () -> parser.validateBodySize(headers, 10, mockResponse));
    verify(mockResponse, never()).status(100);
    verify(mockResponse, never()).send();
  }

  @Test
  void testParseRawPath() {
    String requestLine = "GET https://start.levtus.io/kotlin?tag=awesome&tag=java&good HTTP/1.1";

    assertEquals("/kotlin?tag=awesome&tag=java&good", parser.parseRawPath(requestLine));
  }

  @Test
  void testParseRawPathBadRequest() {
    String requestLine = "GET start.levtus.io/kotlin?tag=awesome&tag=java&good HTTP/1.1";
    assertEquals("/kotlin?tag=awesome&tag=java&good", parser.parseRawPath(requestLine));

    String secondRequestLine = "GET localhost:8080 HTTP/1.1";
    assertEquals("/", parser.parseRawPath(secondRequestLine));
  }

  @Test
  void testParseQueryParams() {
    String rawQuery = "tag=awesome+and+nice&tag=java%20kotlin&good";

    Map<String, List<String>> parsedParams = parser.parseQueryParams(rawQuery);

    // 1. Check total size and keys
    assertEquals(2, parsedParams.size());
    assertTrue(parsedParams.containsKey("tag"));
    assertTrue(parsedParams.containsKey("good"));

    // 2. Check individual values explicitly
    assertEquals(List.of("awesome and nice", "java kotlin"), parsedParams.get("tag"));
    assertEquals(List.of(""), parsedParams.get("good"));
  }

  @Test
  void testNormalizePath() {
    assertEquals("/foo/bar", parser.normalizePath("/foo//bar"));
    assertEquals("/foo/bar/", parser.normalizePath("/foo/bar/"));
    assertEquals("/foo/bar", parser.normalizePath("/foo/./bar"));
    assertEquals("/bar", parser.normalizePath("/foo/../bar"));
    assertEquals("/", parser.normalizePath("//"));
  }

  @Test
  void testNormalizePathSecurity() {
    // Backslash conversion
    assertEquals("/foo/bar", parser.normalizePath("\\foo\\bar"));

    // Malicious traversal attempts
    assertThrows(BadRequestException.class, () -> parser.normalizePath("/../etc/passwd"));
    assertThrows(BadRequestException.class, () -> parser.normalizePath("/foo/../../bar"));
  }

  @Test
  void testParseHeadersWithInvalidHeader() {
    String requestLine =
        "Host: localhost:8080\r\n" + "InvalidHeaderLineNoColon\r\n" + "Tag: Java\r\n\r\n";
    InputStream mockInputStream =
        new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));

    assertThrows(BadRequestException.class, () -> parser.parseHeaders(mockInputStream, 8192, 20));
  }

  @Test
  void testParseHeadersTooLarge() throws IOException {
    String headersLine =
        "Host: localhost:8080\r\n"
            + "User-Agent: Mozilla/5.0\r\n"
            + "Tag: Wonderful\r\n"
            + "Tag: Great\r\n"
            + "Tag: JVM\r\n"
            + "Tag: Levtus\r\n"
            + "Tag: Engine\r\n"
            + "Tag: Web\r\n"
            + "Tag: Server\r\n"
            + "Tag: Java\r\n\r\n";
    InputStream mockInputStream =
        new ByteArrayInputStream(headersLine.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        HeaderTooLargeException.class,
        () -> {
          parser.parseHeaders(mockInputStream, 8192, 3);
        }); // This should throw HeaderTooLargeException because the TOTAL header size is too large

    String secondHeaderLine =
        "Host: localhost:8080\r\n"
            + "User-Agent: Mozilla/5.0\r\n"
            + "Content-Length: 123\r\n"
            + "Tag: Java\r\n\r\n";

    InputStream secondMockInputStream =
        new ByteArrayInputStream(secondHeaderLine.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        HeaderTooLargeException.class,
        () -> {
          parser.parseHeaders(secondMockInputStream, 8192, 3);
        }); // This should throw HeaderTooLargeException because the TOTAL header size is too large
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

    Request request = parser.parseRequest(handler, inputStream, mockResponse);

    assertNotNull(request);
    assertEquals("GET", request.method());
    assertEquals("/test/path", request.path());
    assertEquals("localhost:8080", request.headers().get("host").getFirst());
    assertEquals("val", request.queries().get("query").getFirst());
  }

  @Test
  void testParseRequestValidateBodySizePerRoute() throws Exception {
    String fullRequest =
      "POST / HTTP/1.1\r\n"
        + "Host: localhost:8080\r\n"
        + "Content-Length: 2000\r\n"
        + "\r\n"
        + "hello";
    InputStream inputStream =
      new ByteArrayInputStream(fullRequest.getBytes(StandardCharsets.UTF_8));

    router.post("/", ctx -> {});

    assertDoesNotThrow( // default is 8192
      () -> {
        parser.parseRequest(handler, inputStream, mockResponse);
      }
    );
  }

  @Test
  void testParseRequestValidateBodySizePerRouteNotAllowed() throws Exception {
    String fullRequest =
      "POST / HTTP/1.1\r\n"
        + "Host: localhost:8080\r\n"
        + "Content-Length: 2000\r\n"
        + "\r\n"
        + "hello";
    InputStream inputStream =
      new ByteArrayInputStream(fullRequest.getBytes(StandardCharsets.UTF_8));

    router.post("/", ctx -> {}).limit(0);

    assertThrows(PayloadTooLargeException.class, () -> parser.parseRequest(handler, inputStream, mockResponse));
  }

  @Test
  void testParseRequestSecurityVulnerabilities() throws Exception {
    // 1. Encoded traversal: %2e%2e -> ..
    String validEncoded = "GET /foo/%2e%2e/bar HTTP/1.1\r\nHost: localhost\r\n\r\n";
    InputStream is1 = new ByteArrayInputStream(validEncoded.getBytes(StandardCharsets.UTF_8));
    Request req1 = parser.parseRequest(handler, is1, mockResponse);
    assertEquals("/bar", req1.path());

    // 2. Malicious encoded traversal
    String malEncoded = "GET /%2e%2e/etc/passwd HTTP/1.1\r\nHost: localhost\r\n\r\n";
    InputStream is2 = new ByteArrayInputStream(malEncoded.getBytes(StandardCharsets.UTF_8));
    assertThrows(BadRequestException.class, () -> parser.parseRequest(handler, is2, mockResponse));

    // 3. Encoded slash: %2f -> /
    String encodedSlash = "GET /foo%2fbar HTTP/1.1\r\nHost: localhost\r\n\r\n";
    InputStream is3 = new ByteArrayInputStream(encodedSlash.getBytes(StandardCharsets.UTF_8));
    Request req3 = parser.parseRequest(handler, is3, mockResponse);
    assertEquals("/foo/bar", req3.path());

    // 4. Null byte: %00
    String nullByteRequest = "GET /foo%00bar HTTP/1.1\r\nHost: localhost\r\n\r\n";
    InputStream is4 = new ByteArrayInputStream(nullByteRequest.getBytes(StandardCharsets.UTF_8));
    assertThrows(BadRequestException.class, () -> parser.parseRequest(handler, is4, mockResponse));
  }

  @Test
  void testParseHttpProtocol_HTTP_1_1() {
    String requestLine = "GET / HTTP/1.1";
    HttpProtocol protocol = parser.parseHttpProtocol(requestLine);
    assertEquals(HttpProtocol.HTTP_1_1, protocol);
  }

  @Test
  void testParseHttpProtocol_HTTP_1_0() {
    String requestLine = "GET / HTTP/1.0";
    HttpProtocol protocol = parser.parseHttpProtocol(requestLine);
    assertEquals(HttpProtocol.HTTP_1_0, protocol);
  }

  @Test
  void testParseHttpProtocol_InvalidRequestLine() {
    String badRequestLine = "GET";
    assertThrows(BadRequestException.class, () -> parser.parseHttpProtocol(badRequestLine));
  }

  @Test
  void testParseHttpProtocol_UnsupportedVersion() {
    String unsupportedVersion = "GET / HTTP/2.0";
    assertThrows(io.github.bernardusz.levtus.exception.http.LevtusNotImplementedException.class, 
        () -> parser.parseHttpProtocol(unsupportedVersion));
  }

  @Test
  void testParseHttpProtocol_InvalidVersionFormat() {
    String invalidVersion = "GET / HTTP/invalid";
    assertThrows(BadRequestException.class, () -> parser.parseHttpProtocol(invalidVersion));
  }
}
