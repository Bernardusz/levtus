package io.github.bernardusz.levtus.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.bernardusz.levtus.exception.http.BadRequestException;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import io.github.bernardusz.levtus.routing.Router;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpConnectionHandlerTest {

  @Mock private Router mockRouter;
  @Mock private HttpParser mockParser;
  @Mock private Socket mockSocket;

  private HttpConnectionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new HttpConnectionHandler(mockRouter, mockParser);
  }

  @Test
  void testHandleSuccessfulRequest() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    when(mockSocket.getInputStream()).thenReturn(inputStream);
    when(mockSocket.getOutputStream()).thenReturn(outputStream);

    Request mockRequest = mock(Request.class);
    when(mockRequest.isCached()).thenReturn(true);

    when(mockParser.parseRequest(eq(handler), any(InputStream.class), any(Response.class)))
        .thenReturn(mockRequest)
        .thenReturn(null);

    handler.handle(mockSocket);

    verify(mockRouter).handle(any(Request.class), any(Response.class));
    verify(mockSocket).close();
  }

  @Test
  void testHandleSocketTimeout() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    when(mockSocket.getInputStream()).thenReturn(inputStream);
    when(mockSocket.getOutputStream()).thenReturn(outputStream);

    when(mockParser.parseRequest(eq(handler), any(InputStream.class), any(Response.class)))
        .thenThrow(new SocketTimeoutException("Timeout"));

    handler.handle(mockSocket);

    String response = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(response.contains("408 - Request Timeout"));
  }

  @Test
  void testHandleBadRequest() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    when(mockSocket.getInputStream()).thenReturn(inputStream);
    when(mockSocket.getOutputStream()).thenReturn(outputStream);

    when(mockParser.parseRequest(eq(handler), any(InputStream.class), any(Response.class)))
        .thenThrow(new IllegalArgumentException("Malformed URL"));

    handler.handle(mockSocket);

    String response = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(response.contains("400 - Bad Request (Malformed URL)"));
  }

  @Test
  void testHandleLevtusHttpException() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    when(mockSocket.getInputStream()).thenReturn(inputStream);
    when(mockSocket.getOutputStream()).thenReturn(outputStream);

    when(mockParser.parseRequest(eq(handler), any(InputStream.class), any(Response.class)))
        .thenThrow(new BadRequestException("Custom Error"));

    handler.handle(mockSocket);

    String response = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(response.contains("Custom Error"));
  }

  @Test
  void testHandleGenericException() throws Exception {
    InputStream inputStream = new ByteArrayInputStream("".getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    when(mockSocket.getInputStream()).thenReturn(inputStream);
    when(mockSocket.getOutputStream()).thenReturn(outputStream);

    when(mockParser.parseRequest(eq(handler), any(InputStream.class), any(Response.class)))
        .thenThrow(new RuntimeException("Generic Error"));

    assertThrows(RuntimeException.class, () -> handler.handle(mockSocket));
  }

  @Test
  void testSettersAndGetters() {
    handler.setMaxBodySize(100);
    assertEquals(100, handler.getMaxBodySize());

    handler.setMaxHeaderCount(50);
    assertEquals(50, handler.getMaxHeaderCount());

    handler.setMaxHeaderSize(200);
    assertEquals(200, handler.getMaxHeaderSize());

    handler.setMaxLineSize(300);
    assertEquals(300, handler.getMaxLineSize());

    handler.setMaxEmptyLines(5);
    assertEquals(5, handler.getMaxEmptyLines());
  }

  @Test
  void testChunkSizeAttribute() {
    handler.setMaxChunkSize(1024);
    assertEquals(1024, handler.getMaxChunkSize());

    handler.setMaxChunkCount(1000);
    assertEquals(1000, handler.getMaxChunkCount());
  }

  @Test
  void testInitialSocketTimeout() {
    assertEquals(5000, handler.getInitialSocketTimeout()); // Default

    handler.setInitialSocketTimeout(1000);
    assertEquals(1000, handler.getInitialSocketTimeout());
  }

  @Test
  void testProcessingSocketTimeout() {
    assertEquals(20000, handler.getProcessingSocketTimeout()); // Default

    handler.setProcessingSocketTimeout(1000);
    assertEquals(1000, handler.getProcessingSocketTimeout());
  }

}
