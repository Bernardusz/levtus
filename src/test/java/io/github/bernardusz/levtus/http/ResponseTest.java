package io.github.bernardusz.levtus.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.bernardusz.levtus.exception.developer.ChunkedTransferException;
import io.github.bernardusz.levtus.exception.developer.FileNotFound;
import io.github.bernardusz.levtus.exception.developer.PathTraversalException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResponseTest {

  @TempDir Path tempDir;
  private OutputStream mockOutput;
  private Response response;

  @BeforeEach
  void setUp() {
    mockOutput = mock(OutputStream.class);
    Response realResponse = new Response(mockOutput, tempDir.toString());

    response = spy(realResponse);
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
    verify(response, atLeastOnce()).render("index.html");
    verify(response, atLeastOnce()).sendFile(tempDir.resolve("index.html"));
  }

  @Test
  void testSendFileSuccess() throws IOException {
    // 1. Create a dummy file with known content
    Path testFile = tempDir.resolve("sample.txt");
    Files.writeString(testFile, "Hello World");

    // 2. Use a real stream to capture raw output easily
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());
    response.status(200); // Set explicit status

    // 3. Act
    response.sendFile(testFile);

    // 4. Assert State
    assertTrue(response.isSent());

    // 5. Assert Output (Verify headers and body content were written)
    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("HTTP/1.1 200"));
    assertTrue(resultOutput.contains("Content-Type: text/plain"));
    assertTrue(resultOutput.contains("Server: Levtus-v0.2"));
    assertTrue(resultOutput.contains("Hello World")); // Verifies writeBody worked
  }

  @Test
  void testSendFile_ShouldReturnEarly_WhenAlreadySent() throws IOException {
    Path testFile = tempDir.resolve("sample.txt");
    Files.writeString(testFile, "Hello");

    // Use a mock stream to strictly count interactions
    BufferedOutputStream mockOutput = mock(BufferedOutputStream.class);
    Response response = new Response(mockOutput, tempDir.toString());

    // First call sets isSent = true and writes data
    response.sendFile(testFile);

    // Clear mock interactions from the first call to isolate the second call
    org.mockito.Mockito.clearInvocations(mockOutput);

    // Act - Second call
    response.sendFile(testFile);

    // Assert - No further bytes should be written or flushed to the stream
    verifyNoInteractions(mockOutput);
  }

  @Test
  void testSendFile_ShouldThrowFileNotFound_WhenFileDoesNotExist() {
    BufferedOutputStream mockOutput = mock(BufferedOutputStream.class);
    Response response = new Response(mockOutput, tempDir.toString());
    Path nonExistentFile = tempDir.resolve("ghost.txt");

    // Assert that your custom exception is thrown
    assertThrows(FileNotFound.class, () -> {
      response.sendFile(nonExistentFile);
    });
  }

  @Test
  void testSendBinary_ShouldSetOctetStreamHeader() throws IOException {
    Path binaryFile = tempDir.resolve("image.bin");
    Files.writeString(binaryFile, "010101");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    // Act
    response.sendBinary(binaryFile);

    // Assert
    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("Content-Type: application/octet-stream"));
  }

  @Test
  void testSendFile_StringPath_ShouldWork() throws IOException {
    Path testFile = tempDir.resolve("sample.txt");
    Files.writeString(testFile, "Hello World");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    // Act - using String path overload
    response.sendFile("sample.txt");

    // Assert
    assertTrue(response.isSent());
    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("HTTP/1.1 200"));
    assertTrue(resultOutput.contains("Hello World"));
  }

  @Test
  void testSendBinary_StringPath_ShouldSetOctetStreamHeader() throws IOException {
    Path binaryFile = tempDir.resolve("image.bin");
    Files.writeString(binaryFile, "010101");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    // Act - using String path overload
    response.sendBinary("image.bin");

    // Assert
    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("Content-Type: application/octet-stream"));
  }

  @Test
  void testRenderNotFound() throws IOException {
    assertThrows(FileNotFound.class, () -> {
      response.render("missing.html");
    });

    assertFalse(response.isSent()); // Making sure the response isn't sent, so devs who catch it can handle it

  }

  @Test
  void testRenderPathTraversalProtection() throws IOException {
    // Create a file outside the static directory
    Path outsideDir = tempDir.getParent();
    Path secretFile = outsideDir.resolve("secret.txt");
    Files.writeString(secretFile, "sensitive data");

    // Try to access it via traversal
    // Should have thrown PathTraversalException
    assertThrows(PathTraversalException.class, () -> {
      response.render("../secret.txt");
    });

    assertFalse(response.isSent()); // Making sure the response isn't sent, so devs who catch it can handle it
  }

  private byte[] containsBytes(String str) {
    return argThat(bytes -> new String(bytes).contains(str));
  }

  @Test
  void testStatusChange() {
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
  void testSingularHeader() {
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
  void testFullHeaders() {
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

  @Test
  void testSendHelperEmpty() throws IOException {
    response.send();
    assertTrue(response.isSent());
    verify(mockOutput).flush();
  }

  @Test
  void testDefaultTransferMode() {
    assertTrue(
        response.transferMode.equals(TransferMode.DEFAULT)
    );

    response.send("Normal send");
    assertTrue(
        response.transferMode.equals(TransferMode.NORMAL)
    );

    assertThrows(
        ChunkedTransferException.class,
        () -> response.stream()
    );
  }

  @Test
  void testChunkedTransferMode() {

    response.stream();
    assertTrue(
        response.isChunked()
    );

    assertFalse(
        response.headers.containsKey("content-length")
    );

    assertTrue(
        response.headers.containsKey("Transfer-Encoding")
    );

    assertTrue(
        () -> {
          List<String> value = response.headers.get("Transfer-Encoding");
          if (value.stream().anyMatch("chunked"::equalsIgnoreCase)){
            return true;
          }
          else return false;
        }
    );
  }

  @Test
  void testSetChunkSize(){
    response.withChunkSize(400);
    assertEquals(400, response.chunkSize());
  }

  @Test
  void testOutputFlushedChunk() throws IOException{
    response.stream();
    verify(mockOutput, atLeastOnce()).flush();
  }

  @Test
  void testSendingChunks() throws IOException {

    // 2. Use a real stream to capture raw output easily
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());
    response.status(200); // Set explicit status

    assertThrows(
        ChunkedTransferException.class,
        () -> response.sendChunk("Hello!")
    );

    assertThrows(
        ChunkedTransferException.class,
        () -> response.sendChunk("Hello!".getBytes(), 0, 6)
    );

    assertThrows(
        ChunkedTransferException.class,
        () -> response.sendChunk("Hello!", 0, 6)
    );

    assertThrows(
        ChunkedTransferException.class,
        () -> response.sendChunk("Hello!".getBytes())
    );

    response.status(200).stream();

    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("HTTP/1.1 200"));
    assertTrue(resultOutput.contains("Content-Type: text/plain"));
    assertTrue(resultOutput.contains("Server: Levtus-v0.2"));

    response
        .sendChunk("Hello!")
        .sendChunk("Hello!!", 0, 7)
        .sendChunk("Hello!!!".getBytes(), 0, 8)
        .sendChunk("Hello!!!!".getBytes());

    String resultOutputSecond = outputStream.toString(StandardCharsets.UTF_8);

    assertTrue(
        resultOutputSecond.contains("Hello!!!!")
    );
  }

  @Test
  void testStreamFile_Path_Success() throws IOException {
    Path testFile = tempDir.resolve("sample.txt");
    Files.writeString(testFile, "Hello World");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    response.stream()
      .streamFile("sample.txt").finishChunkedResponse();

    // Assert
    assertTrue(response.isSent());
    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("HTTP/1.1 200"));
    assertTrue(resultOutput.contains("Hello World"));
  }

  @Test
  void testStreamFile_ShouldThrowFileNotFound_WhenFileDoesNotExist() {
    BufferedOutputStream mockOutput = mock(BufferedOutputStream.class);
    Response response = new Response(mockOutput, tempDir.toString());
    Path nonExistentFile = tempDir.resolve("ghost.txt");

    // Assert that your custom exception is thrown
    assertThrows(FileNotFound.class, () -> {
      response.stream().streamFile(nonExistentFile);
    });
    assertThrows(FileNotFound.class, () -> {
      response.streamFile("ghost.txt");
    });
  }

  @Test
  void testStreamFile_Path_CustomChunkSize() throws IOException {
    Path testFile = tempDir.resolve("sample.txt");
    Files.writeString(testFile, "Hello World");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    response.stream()
        .streamFile("sample.txt", 400)
        .finishChunkedResponse();

    // Assert
    assertTrue(response.isSent());
    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("HTTP/1.1 200"));
    assertTrue(resultOutput.contains("Hello World"));
  }

  @Test
  void testStreamFile_String_PathTraversal() throws IOException {
    // Create a file outside the static directory
    Path outsideDir = tempDir.getParent();
    Path secretFile = outsideDir.resolve("secret.txt");
    Files.writeString(secretFile, "sensitive data");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    // Try to access it via traversal
    assertThrows(PathTraversalException.class, () -> {
      response.stream().streamFile("../secret.txt");
    });
  }

  @Test
  void testStreamFile_RequiresChunkedMode() throws IOException {
    Path testFile = tempDir.resolve("sample.txt");
    Files.writeString(testFile, "Hello World");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    // Should throw ChunkedTransferException when not in chunked mode
    assertThrows(ChunkedTransferException.class, () -> {
      response.streamFile(testFile);
    });
  }

  @Test
  void testStreamFile_Directory() throws IOException {
    Path testDir = tempDir.resolve("testdir");
    Files.createDirectory(testDir);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    // Should throw FileNotFound when path is a directory
    assertThrows(FileNotFound.class, () -> {
      response.stream().streamFile(testDir);
    });
  }

  @Test
  void testStreamFrom_InputStream_Success() throws IOException {
    String testData = "Hello World from InputStream";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    response.stream()
        .streamFrom(inputStream).finishChunkedResponse();

    // Assert
    assertTrue(response.isSent());
    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("HTTP/1.1 200"));
    assertTrue(resultOutput.contains("Hello World from InputStream"));
  }

  @Test
  void testStreamFrom_InputStream_CustomChunkSize() throws IOException {
    String testData = "Hello World from InputStream";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    response.stream()
        .streamFrom(inputStream, 2).finishChunkedResponse();

    // Assert

    assertTrue(response.isSent());
    String resultOutput = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(resultOutput.contains("HTTP/1.1 200"));
    assertTrue(
        resultOutput.contains("He")
    );
    assertTrue(
        resultOutput.contains("ll")
    );
    assertTrue(
        resultOutput.contains("o")
    );
  }

  @Test
  void testStreamFrom_RequiresChunkedMode() throws IOException {
    String testData = "Hello World";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Response response = new Response(outputStream, tempDir.toString());

    // Should throw ChunkedTransferException when not in chunked mode
    assertThrows(ChunkedTransferException.class, () -> {
      response.streamFrom(inputStream);
    });
  }
}
