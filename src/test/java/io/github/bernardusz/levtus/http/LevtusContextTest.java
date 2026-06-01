package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
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
  @Mock private Response mockResponse;
  private LevtusContext context;

  @BeforeEach
  void setUp() {
    context = new LevtusContext(mockRequest, mockResponse);
    lenient().when(mockResponse.status(anyInt())).thenReturn(mockResponse);
    lenient().when(mockResponse.addHeader(anyString(), anyString())).thenReturn(mockResponse);
  }

  @Test
  void testReqAndResGetters() {
    assertEquals(mockRequest, context.req());
    assertEquals(mockResponse, context.res());
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
  void testSingleValueQueriesShortcut(){
    Map<String, List<String>> queries =
      Map.of(
        "tag", List.of("java", "web"),
        "id", List.of("123"));

    Request request = new Request(
      "GET", "/test", Map.of(), queries, new ByteArrayInputStream(new byte[0]), 1024);

    LevtusContext ctx = new LevtusContext(
      request,
      mockResponse
    );

    assertEquals("java", ctx.query("tag"));
    assertEquals("123", ctx.query("id"));
    assertEquals("", ctx.query("nonexistent"));
  }

  @Test
  void testQueriesShortcut() {
    Map<String, List<String>> queries = Map.of("q", List.of("search"));
    when(mockRequest.queries()).thenReturn(queries);
    when(mockRequest.queries("q")).thenReturn(List.of("search"));

    assertEquals(queries, context.queries());
    assertEquals(List.of("search"), context.queries("q"));
    verify(mockRequest).queries();
    verify(mockRequest).queries("q");
  }

  @Test
  void testSingleHeaderValue(){
    Map<String, List<String>> headers =
      Map.of(
        "content-type", List.of("application/json"),
        "x-custom", List.of("value1", "value2"));

    Request request =
      new Request(
        "GET", "/test", headers, Map.of(), new ByteArrayInputStream(new byte[0]), 1024);

    LevtusContext ctx = new LevtusContext(
      request,
      mockResponse
    );

    assertEquals("application/json", ctx.header("Content-Type"));
    assertEquals("value1", ctx.header("X-Custom"));
    assertEquals("", ctx.header("Nonexistent"));
  }

  @Test
  void testHeadersCaseInsensitivity() {
    Map<String, List<String>> headers =
      Map.of(
        "content-type", List.of("application/json"),
        "x-custom", List.of("value1", "value2"));

    Request request =
      new Request("GET", "/", headers, Map.of(), new ByteArrayInputStream(new byte[0]), 1024);

    LevtusContext ctx = new LevtusContext(
      request,
      mockResponse
    );

    assertEquals(List.of("application/json"), ctx.headers("Content-Type"));
    assertEquals(List.of("application/json"), ctx.headers("content-type"));
    assertEquals(List.of("value1", "value2"), ctx.headers("X-CUSTOM"));
    assertEquals("application/json", request.contentType());
    assertEquals(headers, ctx.headers());
  }

  @Test
  void testResponseDelegation() {
    context.send("Hello");
    verify(mockResponse).send("Hello");

    context.send(201, "Created");
    verify(mockResponse).status(201);
    verify(mockResponse).send("Created");

    context.html("<h1>Hi</h1>");
    verify(mockResponse).html("<h1>Hi</h1>");

    context.json("{\"status\":\"ok\"}");
    verify(mockResponse).json("{\"status\":\"ok\"}");

    context.text("Plain");
    verify(mockResponse).text("Plain");

    byte[] binary = new byte[] {1, 2, 3};
    context.sendBinary(binary);
    verify(mockResponse).sendBinary(binary);
  }
}
