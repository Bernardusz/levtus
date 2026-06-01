package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LevtusContextTest {

  @Mock private Request mockRequest;
  private Response realResponse;
  private LevtusContext context;

  private ByteArrayOutputStream responseBuffer; // Captures the written data

  @BeforeEach
  void setUp() {
    mockRequest = mock(Request.class);

    responseBuffer = new ByteArrayOutputStream();
    realResponse = new Response(new BufferedOutputStream(responseBuffer), "./public");

    context = new LevtusContext(mockRequest, realResponse);
  }

  @Test
  void testReqAndResGetters() {
    assertEquals(mockRequest, context.req());
    assertEquals(realResponse, context.res());
  }

  @Test
  void testPathParamShortcut() {
    Map<String, String> pathParams = Map.of("userId", "42");
    context.setPathParams(pathParams);

    assertEquals("42", context.param("userId"));
    assertEquals("", context.param("missing"));
    assertEquals(pathParams, context.params());
  }

  @Test
  void testEmptyParamsReturnEmptyMap() {
    assertNotNull(context.params());
    assertTrue(context.params().isEmpty());
  }

  @Test
  void testStatusChange() {
    context.status(404);
    assertEquals(404, context.res.statusCode);
  }

  @Test
  void testHeader() {
    context.header("userId", "42");
    assertEquals(context.res.headers.get("userId"), List.of("42"));
  }

  @Test
  void testSingularHeader() {
    // 2. Act
    context.header("X-Custom", "Value");
    context.header("Server", "Levtus-0.1.0");
    context.header("Server", "Levtus-0.1.1"); // This should overwrite the previous one

    // 3. Assert
    assertEquals("Value", context.res.headers.get("X-Custom").get(0));
    assertEquals("Levtus-0.1.1", context.res.headers.get("Server").get(0));
  }

  @Test
  void testHeaderWithArray() {
    // Arrange
    List<String> firstHeaders = List.of("Value1", "Value2");
    List<String> secondHeaders = List.of("Value3", "Value4");

    // Act
    context.headers("X-Custom", List.of("Bruh")); // This gets overwritten
    context.headers("X-Custom", firstHeaders);

    context.headers("Y-Custom", List.of("Bruh")); // This gets overwritten
    context.headers("Y-Custom", secondHeaders);

    assertEquals(firstHeaders, context.res.headers.get("X-Custom"));
    assertEquals(secondHeaders, context.res.headers.get("Y-Custom"));
  }

  @Test
  void testFullHeaders() {
    Map<String, List<String>> expectedHeaders = new HashMap<>();
    expectedHeaders.put("X-Custom", List.of("Value1", "Value2"));
    expectedHeaders.put("Y-Custom", List.of("Value1", "Value2"));
    expectedHeaders.put("Server", List.of("Levtus-0.1"));
    expectedHeaders.put(
        "Content-Type", List.of("text/html")); // Just put the expected array directly

    context.res.contentType("text/html");
    context.headers(expectedHeaders);

    assertEquals(expectedHeaders, context.res.headers);
  }

  @Test
  void testQueryParamShortcut() {
    Map<String, List<String>> queryParams = Map.of("q", List.of("search"));
    when(mockRequest.queryParams()).thenReturn(queryParams);
    when(mockRequest.query("q")).thenReturn(List.of("search"));

    assertEquals(queryParams, context.queryParams());
    assertEquals(List.of("search"), context.query("q"));
    verify(mockRequest).queryParams();
    verify(mockRequest).query("q");
  }

  @Test
  void testSendString() {
    context.contentType("text/plain");
    context.send("Hello");

    String rawResponse = responseBuffer.toString(StandardCharsets.UTF_8);

    assertTrue(rawResponse.endsWith("Hello"));
    assertTrue(rawResponse.contains("Content-Type: text/plain"));
  }

  @Test
  void testSendStatusAndString() {
    context.send(201, "Created");

    String rawResponse = responseBuffer.toString(StandardCharsets.UTF_8);

    assertTrue(rawResponse.endsWith("Created"));
    assertEquals(201, context.res.statusCode);
  }

  @Test
  void testHtmlDelegation() {
    context.html("<h1>Hi</h1>");

    String rawResponse = responseBuffer.toString(StandardCharsets.UTF_8);

    assertTrue(rawResponse.endsWith("<h1>Hi</h1>"));
    assertTrue(rawResponse.contains("Content-Type: text/html"));
  }

  @Test
  void testJsonDelegation() {
    context.json("{\"status\":\"ok\"}");

    String rawResponse = responseBuffer.toString(StandardCharsets.UTF_8);

    assertTrue(rawResponse.endsWith("{\"status\":\"ok\"}"));
    assertTrue(rawResponse.contains("application/json"));
  }

  @Test
  void testBinaryDelegation() {
    byte[] binary = new byte[] {1, 2, 3};

    context.sendBinary(binary);

    byte[] fullResponse = responseBuffer.toByteArray();

    int headerLength = fullResponse.length - binary.length;
    byte[] lastBytes =
        java.util.Arrays.copyOfRange(fullResponse, headerLength, fullResponse.length);

    assertArrayEquals(
        binary, lastBytes, "The raw HTTP response body did not match the expected binary payload.");

    String rawText = responseBuffer.toString(StandardCharsets.UTF_8);
    assertTrue(rawText.contains("Content-Length: 3"));
  }
}