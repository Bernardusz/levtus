package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseTest {

  private BufferedOutputStream mockOutput;
  private Response response;

  @BeforeEach
  void setUp() {
    mockOutput = mock(BufferedOutputStream.class);
    response = new Response(mockOutput, "./public");
  }

  @Test
  void testInitialState() {
    assertFalse(response.isSent());
  }

  @Test
  void testStatusChaining() {
    Response returned = response.status(404);
    assertEquals(response, returned);
  }

  @Test
  void testAddHeader() {
    Response returned = response.addHeader("X-Custom", "Value");
    assertEquals(response, returned);
  }

  @Test
  void testSendPreventsMultipleWrites() throws IOException {
    response.send("First");
    assertTrue(response.isSent());

    response.send("Second");
    // Verify flush was only called once, implying the second send was ignored
    verify(mockOutput, times(1)).flush();
  }

  @Test
  void testJsonHelperSetsContentType() throws IOException {
    response.json("{}");
    assertTrue(response.isSent());
    verify(mockOutput).flush();
  }

  @Test
  void testHtmlHelperSetsContentType() throws IOException {
    response.html("<html></html>");
    assertTrue(response.isSent());
    verify(mockOutput).flush();
  }

  @Test
  void testTextHelperSetsContentType() throws IOException {
    response.text("Plain text");
    assertTrue(response.isSent());
    verify(mockOutput).flush();
  }

  @Test
  void testBinaryHelperSetsContentType() throws IOException {
    response.sendBinary(new byte[] {0, 1});
    assertTrue(response.isSent());
    verify(mockOutput).flush();
  }
}
