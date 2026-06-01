package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResponseTest {

  private BufferedOutputStream mockOutput;
  private Response response;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    mockOutput = mock(BufferedOutputStream.class);
    response = new Response(mockOutput, tempDir.toString());
  }

  @Test
  void testInitialState() {
    assertFalse(response.isSent());
  }

  @Test
  void testRenderSuccessful() throws IOException {
    Path testFile = tempDir.resolve("index.html");
    Files.writeString(testFile, "<h1>Hello</h1>");

    response.render("index.html");

    assertTrue(response.isSent());
    verify(mockOutput, atLeastOnce()).write(any(byte[].class));
  }

  @Test
  void testRenderNotFound() throws IOException {
    response.render("missing.html");

    assertTrue(response.isSent());
    // Should have sent 404
    verify(mockOutput, atLeastOnce()).write(containsBytes("404 Not Found"));
  }

  @Test
  void testRenderPathTraversalProtection() throws IOException {
    // Create a file outside the static directory
    Path outsideDir = tempDir.getParent();
    Path secretFile = outsideDir.resolve("secret.txt");
    Files.writeString(secretFile, "sensitive data");

    // Try to access it via traversal
    response.render("../secret.txt");

    // Should have sent 403 Forbidden
    verify(mockOutput, atLeastOnce()).write(containsBytes("403 Forbidden"));
  }

  private byte[] containsBytes(String str) {
      return argThat(bytes -> new String(bytes).contains(str));
  }

  @Test
  void testStatusChaining() {
    Response returned = response.status(404);
    assertEquals(response, returned);
  }

  @Test
  void testHeader() {
    Response returned = response.header("X-Custom", "Value");
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
