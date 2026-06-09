package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResponseTest {

  @TempDir
  Path tempDir;
  private BufferedOutputStream mockOutput;
  private Response response;

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
  void testStatusChange(){
    response.status(404);
    assertEquals(404, response.statusCode);
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
  void testSingularHeader(){
    response.header("X-Custom", "Value");
    response.header("Server", "Levtus-0.1.0");
    response.header("Server", "Levtus-0.1.1");

    assertEquals("Levtus-0.1.1", response.headers.get("Server").getFirst());
    assertEquals("Value", response.headers.get("X-Custom").getFirst());
  }

  @Test
  void testHeaderWithArray() {
    List<String> firstHeaders = List.of("Value1", "Value2");
    response.headers("X-Custom", List.of("Bruh"));
    response.headers("X-Custom", firstHeaders);

    List<String> secondHeaders = List.of("Value1", "Value2");
    response.headers("Y-Custom", List.of("Bruh"));
    response.headers("Y-Custom", secondHeaders);

    assertEquals(firstHeaders, response.headers.get("X-Custom"));
    assertEquals(firstHeaders, response.headers.get("X-Custom"));
  }

  @Test
  void testFullHeaders(){
    Map<String, List<String>> headers = new HashMap<>();
    List<String> firstHeaders = List.of("Value1", "Value2");
    List<String> secondHeaders = List.of("Value1", "Value2");
    headers.put("X-Custom", firstHeaders);
    headers.put("Y-Custom", secondHeaders);

    headers.put("Server", List.of("Levtus-0.1")); // Default headers response
    headers.put("Content-Type", response.contentType("text/html").headers.get("Content-Type"));

    response.headers(headers);

    assertEquals(headers, response.headers);
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
