package io.github.bernardusz.levtus.http;

import io.github.bernardusz.levtus.exception.developer.ChunkedTransferException;
import io.github.bernardusz.levtus.exception.developer.FileNotFound;
import io.github.bernardusz.levtus.exception.developer.LevtusIOException;
import io.github.bernardusz.levtus.exception.developer.PathTraversalException;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents an outgoing HTTP/1.1 response.
 *
 * <p>Provides a fluent API to construct and send data back to the client. It handles headers,
 * status codes, and various payload formats (text, JSON, HTML, binary files).
 */
public class Response {
  private final OutputStream output;

  /** The base directory path from which static files are served. */
  String staticFilesPath;

  int statusCode = 200;
  Map<String, List<String>> headers = new HashMap<>();
  TransferMode transferMode = TransferMode.DEFAULT;
  private boolean isSent = false;
  private int chunkSize = 64 * 1024;

  /**
   * Initializes a new HTTP response bound to a client socket's output stream.
   *
   * @implNote Automatically injects default headers like "Content-Type" and "Server".
   * @param output the buffered output stream connected to the client (must not be null)
   * @param staticFilesPath the directory path for resolving static files (must not be null)
   */
  public Response(OutputStream output, String staticFilesPath) {
    this.output = output;
    this.staticFilesPath = staticFilesPath;
  }

  /**
   * Sets the HTTP response status code.
   *
   * <p>{@code res.status(404).send("Not Found");}
   *
   * @param code the 3-digit HTTP status code (e.g., 200, 404, 500)
   * @return the current Response instance for method chaining
   */
  public Response status(int code) {
    this.statusCode = code;
    return this;
  }

  /**
   * Overrides the "Content-Type" header for the outgoing response.
   *
   * @param type the MIME type string (e.g., "application/json", "text/html") (must not be null)
   * @return The current Response instance for method chaining
   */
  public Response contentType(String type) {
    headers.put("Content-Type", new ArrayList<>(List.of(type)));
    return this;
  }

  /**
   * Sets a header, replacing any existing value(s) for this header name.
   *
   * @param name the header name (must not be null)
   * @param value the header value (must not be null)
   * @return the current Response instance for method chaining
   */
  public Response header(String name, String value) {
    headers.put(name, new ArrayList<>(List.of(value)));
    return this;
  }

  /**
   * Sets a multi-value header, replacing any existing list for this header name.
   *
   * @param name the header name (must not be null)
   * @param values the header values (must not be null)
   * @return the current Response instance for method chaining
   */
  public Response headers(String name, List<String> values) {
    headers.put(name, values);
    return this;
  }

  /**
   * Merges a map of headers into the existing response headers.
   *
   * <p>Overwrites existing keys but preserves unique existing headers.
   *
   * @param headers the full Map of headers
   * @return the current Response instance for method chaining
   */
  public Response headers(Map<String, List<String>> headers) {
    this.headers.putAll(
        headers); // We do this because we want batch-config by user to be authoritative
    return this;
  }

  /**
   * Checks if the response payload has already been written to the socket stream. Once sent,
   * further attempts to modify headers or write to the body will be ignored.
   *
   * @return {@code true} if the response is fully sent, {@code false} otherwise
   */
  public boolean isSent() {
    return isSent;
  }

  /**
   * Encodes a string into UTF-8 bytes and sends it as the response payload.
   *
   * @param body the raw string payload to send (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void send(String body) throws LevtusIOException {
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Sends an HTML string as the response payload, automatically setting the "Content-Type" to
   * "text/html".
   *
   * @param body the HTML formatted string (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void html(String body) throws LevtusIOException {
    contentType("text/html");
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Sends a plain text string as the response payload, automatically setting the "Content-Type" to
   * "text/plain".
   *
   * @param body the plain text string (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void text(String body) throws LevtusIOException {
    contentType("text/plain");
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Sends a raw byte array as a downloadable payload, automatically setting the "Content-Type" to
   * "application/octet-stream".
   *
   * @param body the byte array representing the file or binary data (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void sendBinary(byte[] body) throws LevtusIOException {
    contentType("application/octet-stream");
    send(body);
  }

  /**
   * Sends a JSON formatted string as the response payload, automatically setting the "Content-Type"
   * to "application/json".
   *
   * @param body the serialized JSON string (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void json(String body) throws LevtusIOException {
    contentType("application/json");
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Streams a file from the server's disk to the client. Safely resolves paths to prevent directory
   * traversal attacks (403 Forbidden). Automatically probes and sets the correct MIME type.
   *
   * <p>{@code res.render("index.html");}
   *
   * @param htmlPath the relative path of the file within the configured static directory (must not
   *     be null)
   * @throws LevtusIOException if an error occurs while accessing or streaming the file
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws PathTraversalException if the file path contains traversal characters
   */
  public void render(String htmlPath) throws LevtusIOException, FileNotFound, PathTraversalException {
    if (isSent) return;
    Path filePath = Path.of(staticFilesPath, htmlPath).normalize();

    Path rootPath = Path.of(staticFilesPath).toAbsolutePath().normalize();
    if (!filePath.toAbsolutePath().startsWith(rootPath)) {
      throw new PathTraversalException("Path traversal detected");
    }

    if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
      try {
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) mimeType = "application/octet-stream";
        this.contentType(mimeType);
        this.status(200);

        sendFile(filePath);
      } catch (IOException e) {
        throw new LevtusIOException(e.getMessage(), e);
      }
    } else {
      throw new FileNotFound("File not found (relative path): " + htmlPath);
    }
  }

  /**
   * Sends an empty response with the current status code without a body.
   *
   * @implNote This method is primarily used for sending 100-Continue response and empty responses.
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void send() {
    if (isSent) return;
    try {
      writeStatus(statusCode);
      writeHeaders();
      output.flush();
      isSent = true;
    } catch (IOException e) {
      throw new LevtusIOException(e.getMessage(), e);
    }
  }

  /**
   * Transmits the final HTTP status line, headers, and body bytes to the client socket.
   *
   * <p>Automatically sets the "Content-Type" and "Server" headers if not already set.
   *
   * @implNote Flushes the stream immediately after writing. Subsequent calls to any send method
   *     will be ignored.
   * @param bodyBytes the complete raw payload (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void send(byte[] bodyBytes) throws LevtusIOException {
    if (isSent) return;
    if (transferMode.equals(TransferMode.CHUNKED)){
      throw new ChunkedTransferException("Not allowed to switch mid response");
    }

    transferMode = TransferMode.NORMAL;
    headers.computeIfAbsent("Content-Type", _ -> new ArrayList<>(List.of("text/plain")));
    headers.computeIfAbsent("Server", _ -> new ArrayList<>(List.of("Levtus-v0.1")));

    try {
      // HTTP Status
      writeStatus(statusCode);
      writeHeaders(bodyBytes.length);
      writeBody(bodyBytes);
      output.flush();
      isSent = true;
    } catch (IOException e) {
      throw new LevtusIOException(e.getMessage(), e);
    }
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer.
   * Uses FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * @param path the file path to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   */
  public void sendFile(Path path) throws LevtusIOException, FileNotFound {
    if (isSent) return;
    if (transferMode.equals(TransferMode.CHUNKED)){
      throw new ChunkedTransferException("Not allowed to switch mid response");
    }

    transferMode = TransferMode.NORMAL;
    headers.computeIfAbsent("Content-Type", _ -> new ArrayList<>(List.of("text/plain")));
    headers.computeIfAbsent("Server", _ -> new ArrayList<>(List.of("Levtus-v0.2")));

    try {
      writeStatus(statusCode);
      writeHeaders();
      writeBody(path);
      output.flush();
      isSent = true;
    } catch (IOException e) {
      throw new LevtusIOException(e.getMessage(), e);
    }
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer.
   * Uses FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>Internally calls {@link #sendFile(Path)}</p>
   *
   * @param path the relative file path to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws PathTraversalException if the path contains traversal characters
   */
  public void sendFile(String path) throws LevtusIOException, FileNotFound, PathTraversalException {
    Path filePath = Path.of(staticFilesPath, path).normalize();
    Path rootPath = Path.of(staticFilesPath).toAbsolutePath().normalize();
    
    if (!filePath.toAbsolutePath().startsWith(rootPath)) {
      throw new PathTraversalException("Path traversal detected");
    }
    
    if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
      throw new FileNotFound("File not found (relative path): " + path);
    }
    
    sendFile(filePath);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer for downloads.
   * Uses FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>A wrapper method that sets the content type to "application/octet-stream"</p>
   * <p>Internally calls {@link #sendFile(Path)}</p>
   *
   * @param path the file path to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   */
  public void sendBinary(Path path) throws LevtusIOException, FileNotFound {
    contentType("application/octet-stream").sendFile(path);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer for downloads.
   * Uses FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>A wrapper method that sets the content type to "application/octet-stream"</p>
   * <p>Resolves the path relative to the configured static files directory and performs
   * security checks to prevent directory traversal attacks.</p>
   * <p>Internally calls {@link #sendBinary(Path)}</p>
   *
   * @param path the relative file path within the static directory
   * @throws LevtusIOException if an I/O error occurs while transferring the file or if the path is invalid
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws PathTraversalException if the path contains traversal characters
   */
  public void sendBinary(String path) throws LevtusIOException, FileNotFound, PathTraversalException {
    Path filePath = Path.of(staticFilesPath, path).normalize();
    Path rootPath = Path.of(staticFilesPath).toAbsolutePath().normalize();
    
    if (!filePath.toAbsolutePath().startsWith(rootPath)) {
      throw new PathTraversalException("Path traversal detected");
    }
    
    if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
      throw new FileNotFound("File not found (relative path): " + path);
    }
    
    sendBinary(filePath);
  }

  /**
   * The method to check the current responding answer, is it {@link TransferMode#NORMAL} or {@link TransferMode#CHUNKED}
   *
   * @return true if the response is in chunked mode, false otherwise
   */
  public boolean isChunked() {
    return transferMode == TransferMode.CHUNKED;
  }

  /**
   * Returns the current default chunk size.
   *
   * @return the current chunk size
   */
  public int chunkSize(){
    return chunkSize;
  }

  /**
   * Set the default chunk size
   *
   * @param chunkSize the size of each chunk being sent
   * @return the current Response object to be chained
   */
  public Response withChunkSize(int chunkSize){
    this.chunkSize = chunkSize;
    return this;
  }

  /**
   * Changes the responding method to {@link TransferMode#CHUNKED}.
   *
   * <p>These following things will happen when you call {@link Response#stream()}:</p>
   * <ul>
   *   <li>The content length header is removed from Response</li>
   *   <li>Will set the transfer encoding header to chunked</li>
   *   <li>Will flush all the headers down the socker first</li>
   * </ul>
   *
   * <p>From now on, all method that internally uses {@link Response#sendFile(Path)} or {@link Response#send(byte[])} will throw an exception of {@link ChunkedTransferException} as you cannot switch in the middle of response</p>
   * <p>All the viable methods of responding with Chunked mode are</p>
   * <ul>
   *   <li>{@link Response#sendChunk(byte[])} to send the chunk directly to the socket (flushed)</li>
   *   <li>{@link Response#sendChunk(String)} to send the chunk directly to the socket (flushed)</li>
   *   <li>{@link Response#streamFile(Path)} to stream a file from disk in chunks (buffered transfer)</li>
   *   <li>{@link Response#streamFrom(InputStream)} to stream from any InputStream in chunks</li>
   *   <li>{@link Response#finishChunkedResponse()} to finish the chunked response</li>
   * </ul>
   *
   * <p>Take as a note, that {@link Response#finishChunkedResponse()} is called in finally block, so it is safe whether you call it or not</p>
   *
   * @return the Response object to be chained
   * @throws ChunkedTransferException if stream is already called or when a bulk sending method has been called earlier
   * @throws LevtusIOException if an unexpected IO error occurs
   */
  public Response stream() throws LevtusIOException, ChunkedTransferException{
    switch (transferMode){
      case TransferMode.NORMAL -> throw new ChunkedTransferException("Not allowed to switch transfer mode mid response");
      case TransferMode.CHUNKED -> throw new ChunkedTransferException("Response is already in CHUNKED streaming");
    }

    this.transferMode = TransferMode.CHUNKED;
    if (this.headers != null) {
      this.headers.remove("Content-Length");
    }
    this.header("Transfer-Encoding", "chunked");

    headers.computeIfAbsent("Content-Type", _ -> new ArrayList<>(List.of("text/plain")));
    headers.computeIfAbsent("Server", _ -> new ArrayList<>(List.of("Levtus-v0.2")));

    try{
      writeStatus(statusCode);
      writeHeaders();
      output.flush();
    }
    catch (IOException e){
      throw new LevtusIOException(e.getMessage(), e);
    }

    return this;
  }

  /**
   * The method to sends the chunk of data to the socket (can be chained and used multiple times).
   *
   * <p>Will send the chunk directly to the socket (flushed). This method can be chained and called multiple times for sending multiple chunks</p>
   * <p>Used to specifically control the offset and length the data is sent</p>
   *
   * @param data the data to be sent to the socker
   * @return the Response object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   */
  public Response sendChunk(byte[] data, int offset, int length) throws LevtusIOException, ChunkedTransferException {
    switch (transferMode){
      case TransferMode.NORMAL -> throw new ChunkedTransferException("Not allowed to switch transfer mode mid response");
      case TransferMode.DEFAULT -> throw new ChunkedTransferException("Forget to change the mode to stream");
      default -> {}
    }

    if (data == null || length == 0) return this;

    try {
      String hexSizeLine = Integer.toHexString(length) + "\r\n";

      output.write(hexSizeLine.getBytes(StandardCharsets.US_ASCII));
      output.write(data, offset, length);
      output.write("\r\n".getBytes(StandardCharsets.US_ASCII));

      output.flush();
    }
    catch (IOException e){
      throw new LevtusIOException(e.getMessage(), e);
    }

    return this;
  }


  /**
   * The method to sends the chunk of data to the socket (can be chained and used multiple times).
   *
   * <p>Will send the chunk directly to the socket (flushed). This method can be chained and called multiple times for sending multiple chunks</p>
   *
   * @param data the data to be sent to the socker
   * @return the Response object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   */
  public Response sendChunk(byte[] data) throws LevtusIOException, ChunkedTransferException {
    switch (transferMode){
      case TransferMode.NORMAL -> throw new ChunkedTransferException("Not allowed to switch transfer mode mid response");
      case TransferMode.DEFAULT -> throw new ChunkedTransferException("Forget to change the mode to stream");
      default -> {}
    }

    if (data == null || data.length == 0) return this;

    try {
      String hexSizeLine = Integer.toHexString(data.length) + "\r\n";

      output.write(hexSizeLine.getBytes(StandardCharsets.US_ASCII));
      output.write(data);
      output.write("\r\n".getBytes(StandardCharsets.US_ASCII));

      output.flush();
    }
    catch (IOException e){
      throw new LevtusIOException(e.getMessage(), e);
    }

    return this;
  }

  /**
   * The method to sends the chunk of data in the form of String to the socket (can be chained and used multiple times).
   *
   * <p>Will send the chunk directly to the socket (flushed). This method can be chained and called multiple times for sending multiple chunks</p>
   * <p>A helper method, internally calling {@link Response#sendChunk(byte[])}</p>
   * <p>Used to specifically control the offset and length the data is sent</p>
   *
   * @param data the data to be sent to the socker
   * @param length the length of the data
   * @param offset the offset of the data
   * @return the Response object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   */
  public Response sendChunk(String data, int offset, int length) throws LevtusIOException, ChunkedTransferException {
    return sendChunk(data.getBytes(), offset, length);
  }

  /**
   * The method to sends the chunk of data in the form of String to the socket (can be chained and used multiple times).
   *
   * <p>Will send the chunk directly to the socket (flushed). This method can be chained and called multiple times for sending multiple chunks</p>
   * <p>A helper method, internally calling {@link Response#sendChunk(byte[])}</p>
   *
   * @param data the data to be sent to the socker
   * @return the Response object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   */
  public Response sendChunk(String data) throws LevtusIOException, ChunkedTransferException {
    return sendChunk(data.getBytes());
  }

  /**
   * Streams a file from disk to the socket in chunked mode.
   *
   * <p>Uses buffered transfer (not zero-copy) with configurable chunk size. The file is read into
   * a buffer and sent as chunks with proper chunk headers. This is different from {@link #sendFile(Path)}
   * which uses zero-copy NIO transfer and cannot be used in chunked mode.</p>
   *
   * <p>Requires {@link #stream()} to be called first to enable chunked transfer mode.</p>
   *
   * @param path the absolute path of the file being sent
   * @param chunkSize the size of each chunk in bytes
   * @return the Response object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   */
  public Response streamFile(Path path, int chunkSize) throws FileNotFound, LevtusIOException, ChunkedTransferException{
    if (!Files.exists(path) || Files.isDirectory(path)) {
      throw new FileNotFound("File not found (absolute path): " + path);
    }

    try (InputStream is = Files.newInputStream(path)) {
      byte[] buffer = new byte[chunkSize];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) > 0) {
        sendChunk(buffer, 0, bytesRead);
      }
    }
    catch (IOException e){
      throw new LevtusIOException(e.getMessage(), e);
    }
    return this;
  }

  /**
   * Streams a file from disk to the socket in chunked mode using the default chunk size.
   *
   * <p>Uses buffered transfer (not zero-copy) with the default chunk size configured via
   * {@link #withChunkSize(int)}. The file is read into a buffer and sent as chunks with proper
   * chunk headers. This is different from {@link #sendFile(Path)} which uses zero-copy NIO transfer
   * and cannot be used in chunked mode.</p>
   *
   * <p>Requires {@link #stream()} to be called first to enable chunked transfer mode.</p>
   *
   * @param path the absolute path of the file being sent
   * @return the Response object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   */
  public Response streamFile(Path path) throws FileNotFound, LevtusIOException, ChunkedTransferException {
    return streamFile(path, chunkSize);
  }


  /**
   * Streams a file from disk to the socket in chunked mode with configurable chunk size.
   *
   * <p>Uses buffered transfer (not zero-copy) with configurable chunk size. The file path is resolved
   * relative to the configured static files directory. Path traversal attacks are prevented by
   * validating the resolved path stays within the static directory.</p>
   *
   * <p>Requires {@link #stream()} to be called first to enable chunked transfer mode.</p>
   *
   * @param path the relative path of the file within the static directory
   * @param chunkSize the size of each chunk in bytes
   * @return the Response object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   * @throws PathTraversalException if the path contains traversal characters
   */
  public Response streamFile(String path, int chunkSize) throws LevtusIOException, ChunkedTransferException, PathTraversalException, FileNotFound {
    Path filePath = Path.of(staticFilesPath, path).normalize();
    Path rootPath = Path.of(staticFilesPath).toAbsolutePath().normalize();

    if (!filePath.toAbsolutePath().startsWith(rootPath)) {
      throw new PathTraversalException("Path traversal detected");
    }

    if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
      throw new FileNotFound("File not found (relative path): " + path);
    }

    return streamFile(filePath, chunkSize);
  }

  /**
   * Streams a file from disk to the socket in chunked mode using the default chunk size.
   *
   * <p>Uses buffered transfer (not zero-copy) with the default chunk size configured via
   * {@link #withChunkSize(int)}. The file path is resolved relative to the configured static files
   * directory. Path traversal attacks are prevented by validating the resolved path stays within
   * the static directory.</p>
   *
   * <p>Requires {@link #stream()} to be called first to enable chunked transfer mode.</p>
   *
   * @param path the relative path of the file within the static directory
   * @return the Response object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   * @throws PathTraversalException if the path contains traversal characters
   */
  public Response streamFile(String path) throws LevtusIOException, ChunkedTransferException, PathTraversalException, FileNotFound {
    return streamFile(path, chunkSize);
  }

  /**
   * Streams data from an InputStream to the socket in chunked mode with configurable chunk size.
   *
   * <p>Uses buffered transfer (not zero-copy) with configurable chunk size. The InputStream is read
   * into a buffer and sent as chunks with proper chunk headers. This is useful for streaming data
   * from databases, external APIs, or proxying request bodies.</p>
   *
   * <p>Requires {@link #stream()} to be called first to enable chunked transfer mode.</p>
   *
   * @param is the input stream to read data from
   * @param chunkSize the size of each chunk in bytes
   * @return the Response object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   */
  public Response streamFrom(InputStream is, int chunkSize) throws LevtusIOException, ChunkedTransferException {
    try {
      byte[] buffer = new byte[chunkSize];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) > 0) {
        sendChunk(buffer, 0, bytesRead);
      }
    }
    catch (IOException e){
      throw new LevtusIOException(e.getMessage(), e);
    }
    return this;
  }

  /**
   * Streams data from an InputStream to the socket in chunked mode using the default chunk size.
   *
   * <p>Uses buffered transfer (not zero-copy) with the default chunk size configured via
   * {@link #withChunkSize(int)}. The InputStream is read into a buffer and sent as chunks with
   * proper chunk headers. This is useful for streaming data from databases, external APIs, or
   * proxying request bodies.</p>
   *
   * <p>Requires {@link #stream()} to be called first to enable chunked transfer mode.</p>
   *
   * @param is the input stream to read data from
   * @return the Response object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk sending method
   */
  public Response streamFrom(InputStream is) throws LevtusIOException, ChunkedTransferException {
    return streamFrom(is, chunkSize);
  }

  /**
   * The method to send the final CRLF to end the current Response.
   *
   * <p>Internally called by the HttpConnectionHandler, it is optional to call this method or leave it as is</p>
   *
   * @throws LevtusIOException if an unexpected IO error occurs
   */
  public void finishChunkedResponse() throws LevtusIOException {
    if (transferMode != TransferMode.CHUNKED || isSent()){
      return;
    }

    try {
      output.write("0\r\n\r\n".getBytes());
      output.flush();
      isSent = true;
    }
    catch (IOException e){
      throw new LevtusIOException(e.getMessage(), e);
    }
  }

  private void writeHeaders() throws IOException {
    for (var entry : headers.entrySet()) {
      for (var headerValue : entry.getValue()) {
        output.write(
            (purifyHeader(entry.getKey()) + ": " + purifyHeader(headerValue) + "\r\n").getBytes());
      }
    }
    output.write("\r\n".getBytes());
  }
  private void writeHeaders(long contentLength) throws IOException {
    headers.put("Content-Length", new ArrayList<>(List.of(String.valueOf(contentLength))));
    for (var entry : headers.entrySet()) {
      for (var headerValue : entry.getValue()) {
        output.write(
            (purifyHeader(entry.getKey()) + ": " + purifyHeader(headerValue) + "\r\n").getBytes());
      }
    }
    output.write("\r\n".getBytes());
  }

  private void writeBody(byte[] bodyBytes) throws IOException {
    output.write(bodyBytes);
  }

  private void writeBody(Path path) throws LevtusIOException, FileNotFound {
    if (!Files.exists(path) || Files.isDirectory(path)) {
      throw new FileNotFound("File not found (absolute path): " + path);
    }
    
    try(FileChannel fileChannel = FileChannel.open(path)) {

      WritableByteChannel targetChannel = Channels.newChannel(output);

      long position = 0;
      long size = fileChannel.size();

      while (position < size) {
        long transferred = fileChannel.transferTo(position, size - position, targetChannel);
        if (transferred <= 0){
          break;
        }
        position += transferred;
      }
    }
    catch (IOException e){
      throw new LevtusIOException(e.getMessage(), e);
    }
  }

  private void writeStatus(int statusCode) throws IOException {
    output.write(("HTTP/1.1 " + statusCode + " " + getStatusText(statusCode) + "\r\n").getBytes());
  }

  // A method to clean a header from \r and \n
  private String purifyHeader(String header) {
    if (!header.contains("\r") && !header.contains("\n") && !header.contains("\0")) return header;
    return header.replace("\r", "").replace("\n", "").replace("\0", "");
  }

  private String getStatusText(int code) {
    return switch (code) {
      case 100 -> "Continue";
      case 200 -> "OK";
      case 201 -> "Created";
      case 204 -> "No Content";
      case 400 -> "Bad Request";
      case 401 -> "Unauthorized";
      case 403 -> "Forbidden";
      case 404 -> "Not Found";
      case 405 -> "Method Not Allowed";
      case 500 -> "Internal Server Error";
      case 501 -> "Not Implemented";
      case 503 -> "Service Unavailable";
      default -> "Unknown";
    };
  }
}
