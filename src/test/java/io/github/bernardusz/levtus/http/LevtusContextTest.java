package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
