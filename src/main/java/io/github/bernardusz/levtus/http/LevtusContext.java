package io.github.bernardusz.levtus.http;

import io.github.bernardusz.levtus.exception.developer.*;
import io.github.bernardusz.levtus.exception.http.PayloadTooLargeException;
import io.github.bernardusz.levtus.io.LevtusInputStream;
import io.github.bernardusz.levtus.io.StreamConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The LevtusContext that wraps HTTP Requests and Output Stream as {@link Request} and {@link
 * Response}.
 *
 * <p>Responsible for:
 *
 * <ul>
 *   <li>Wrapping HTTP Requests and Output Stream
 *   <li>Handling and providing access to Path Parameters
 *   <li>Handling Query Parameters
 *   <li>Handling Request Body
 *   <li>Handling Response transmission
 * </ul>
 *
 * @author Bernardusz
 * @version 0.1.1
 */
public class LevtusContext {
  /** The Request object that represents an incoming HTTP/1.1 request. */
  final Request req;

  /** The fully instantiated Response object that represents an outgoing HTTP/1.1 response. */
  final Response res;

  /**
   * The path parameters extracted from the URI based on the route pattern (wildcards). For example,
   * in a route "/users/{id}", the value of "{id}" is stored here.
   */
  final Map<String, String> pathParams;

  /**
   * Instantiates a new LevtusContext, setting the Request, Response, and Path Parameters.
   *
   * @param req the incoming request
   * @param res the outgoing response
   * @param pathParams the extracted path parameters
   */
  public LevtusContext(Request req, Response res, Map<String, String> pathParams) {
    this.req = req;
    this.res = res;
    this.pathParams = pathParams != null ? Map.copyOf(pathParams) : Map.of();
  }

  /**
   * Return the Request object.
   *
   * @return the Request object
   */
  public Request req() {
    return req;
  }

  /**
   * Return the Response object.
   *
   * @return the response
   */
  public Response res() {
    return res;
  }

  /**
   * Retrieves the body stream of the request. Enforces the configured maximum body size limits to
   * prevent memory exhaustion (OOM) attacks.
   *
   * <p>{@code LevtusInputStream bodyStream = ctx.bodyStream();} {@code InputStream bodyStream =
   * ctx.bodyStream();}
   *
   * @return the body stream from the socket {@link LevtusInputStream}
   * @throws BodyAlreadyConsumedException if the body has already been consumed
   * @throws LevtusIOException if an I/O error occurs while reading the socket stream
   * @throws PayloadTooLargeException if the 'Content-Length' or actual stream data exceeds {@code
   *     maxBodySize}
   */
  public LevtusInputStream bodyStream()
      throws BodyAlreadyConsumedException, PayloadTooLargeException, LevtusIOException {
    return req.bodyStream();
  }

  /**
   * Consumes the request body as a continuous stream via a functional interface.
   *
   * <p>This method provides a safe, resource-managed way to process large incoming payloads
   * (e.g., file uploads) piece by piece without buffering the entire body into memory. The underlying
   * socket stream is automatically managed and safely closed using a try-with-resources block
   * once the consumer completes execution.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * ctx.bodyStream(stream -> {
   * byte[] buffer = new byte[8192];
   * int read;
   * while ((read = stream.read(buffer)) != -1) {
   * out.write(buffer, 0, read);
   * }
   * });
   * }</pre>
   *
   * @param consumer the functional stream handler that will process the raw stream bytes
   * @throws LevtusIOException if an underlying socket error occurs or if user logic throws an I/O error
   * @throws PayloadTooLargeException if the streamed body exceeds the configured server limits
   * @throws BodyAlreadyConsumedException if the request body has already been read by an alternate method
   */
  public void bodyStream(StreamConsumer consumer)
      throws LevtusIOException, PayloadTooLargeException, BodyAlreadyConsumedException {
    try (LevtusInputStream lis = this.bodyStream()) {
      consumer.consume(lis);
    } catch (IOException e) {
      // If the developer's lambda logic threw a raw checked IOException
      throw new LevtusIOException("An I/O error occurred while reading the socket stream", e);
    } catch (BodyAlreadyConsumedException | PayloadTooLargeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new LevtusIOException("Error processing body stream", e);
    }
  }

  /**
   * Lazily reads the incoming payload from the raw stream into a byte array and caches it. Enforces
   * the configured maximum body size limits to prevent memory exhaustion (OOM) attacks.
   *
   * <p>{@code byte[] body = ctx.body();}
   *
   * @return the raw byte array of the request body
   * @throws PayloadTooLargeException if the 'Content-Length' or actual stream data exceeds {@code
   *     maxBodySize}
   * @throws LevtusIOException if an I/O error occurs while reading the socket stream
   * @throws BodyAlreadyConsumedException if the body has already been consumed
   */
  public byte[] body()
      throws PayloadTooLargeException, BodyAlreadyConsumedException, LevtusIOException {
    return req.body();
  }

  /**
   * Retrieves the body of the request in form of String. Enforces the configured maximum body size
   * limits to prevent memory exhaustion (OOM) attacks.
   *
   * <p>{@code String body = ctx.bodyAsString();}
   *
   * @return the body of the request as a String
   * @throws PayloadTooLargeException if the 'Content-Length' or actual stream data exceeds {@code
   *     maxBodySize}
   * @throws BodyAlreadyConsumedException if the body has already been consumed
   * @throws LevtusIOException if an I/O error occurs while reading the socket stream
   */
  public String bodyAsString()
      throws PayloadTooLargeException, BodyAlreadyConsumedException, LevtusIOException {
    return req.bodyAsString();
  }

  /**
   * Retrieves the value of a path parameter (wildcard) by its name.
   *
   * <p>{@code String userId = ctx.param("id");}
   *
   * @param name the name of the path parameter (defined in the route pattern)
   * @return the parameter value, or an empty string if not found
   */
  public String param(String name) {
    return pathParams.getOrDefault(name, "");
  }

  /**
   * Retrieves all the value of path parameters (wildcard).
   *
   * <p>{@code HashMap<String, String> params = ctx.params();}
   *
   * @return the map
   */
  public Map<String, String> params() {
    return pathParams != null ? pathParams : Map.of();
  }

  /**
   * Retrieves the value - a String - of a specific query parameter by its key.
   *
   * <p>Return the first value inside the list of values for the query parameter.
   *
   * <p>{@code String id = ctx.query("id");}
   *
   * @param key The query key
   * @return The first value for the header, or an empty string if the key does not exist
   */
  public String query(String key) {
    return req.query(key);
  }

  /**
   * Retrieves the value - a List of String - of a specific query parameter by its key.
   *
   * <p>{@code ArrayList<String> tag = ctx.queries("tag");}
   *
   * @param name the name of the query parameter
   * @return the query parameter value, or an empty string if not found
   */
  public List<String> queries(String name) {
    return req.queries(name);
  }

  /**
   * Retrieves the complete map of parsed URL query parameters.
   *
   * <p>{@code HashMap<String, List<String>> queryParams = ctx.queries();}
   *
   * @return a map of query parameters
   */
  public Map<String, List<String>> queries() {
    return req.queries();
  }

  /**
   * Retrieves the first value associated with a specific HTTP header. Header name resolution is
   * case-insensitive.
   *
   * <p>{@code String accepts = ctx.header("Accept");}
   *
   * @param name the target header name (must not be null)
   * @return a list of header values, or an empty list if the header is not present
   */
  public String header(String name) {
    return req.header(name);
  }

  /**
   * Retrieves all values associated with a specific HTTP header. Header name resolution is
   * case-insensitive.
   *
   * <p>{@code List<String> accepts = ctx.headers("Accept");}
   *
   * @param name the target header name (must not be null)
   * @return a list of header values, or an empty list if the header is not present
   */
  public List<String> headers(String name) {
    return req.headers(name);
  }

  /**
   * Retrieves the complete map of HTTP headers associated with this request.
   *
   * <p>{@code HashMap<String, List<String>> headers = ctx.headers();}
   *
   * @return an unmodifiable map of headers
   */
  public Map<String, List<String>> headers() {
    return req.headers();
  }

  /**
   * Sets the HTTP response status code
   *
   * @param code the 3-digit HTTP status code
   * @return The current LevtusContext instance for method chaining
   */
  public LevtusContext status(int code) {
    res.status(code);
    return this;
  }

  /**
   * Overrides the "Content-Type" header for the outgoing response.
   *
   * @param type the MIME type string (e.g., "application/json", "text/html") (must not be null)
   * @return The current LevtusContext instance for method chaining
   */
  public LevtusContext contentType(String type) {
    res.contentType(type);
    return this;
  }

  /**
   * Sets a header, replacing any existing value(s) for this header name.
   *
   * @param name the header name (must not be null)
   * @param value the header value (must not be null)
   * @return The current LevtusContext instance for method chaining
   */
  public LevtusContext header(String name, String value) {
    res.header(name, value);
    return this;
  }

  /**
   * Sets a multi-value header, replacing any existing list for this header name.
   *
   * @param name the header name (must not be null)
   * @param values the header values (must not be null)
   * @return The current LevtusContext instance for method chaining
   */
  public LevtusContext headers(String name, List<String> values) {
    res.headers(name, values);
    return this;
  }

  /**
   * Merges a map of headers into the existing response headers.
   *
   * <p>Overwrites existing keys but preserves unique existing headers.
   *
   * @param headers the full Map of headers
   * @return The current LevtusContext instance for method chaining
   */
  public LevtusContext headers(Map<String, List<String>> headers) {
    res.headers(headers);
    return this;
  }

  /**
   * Send a plain String as data (text/plain) through the Response.
   *
   * @param data the string data to send
   * @throws LevtusIOException if an I/O error occurs while sending the data
   */
  public void send(String data) throws LevtusIOException {
    res.send(data);
  }

  /**
   * Send a plain String as data (text/plain) through the Response with a custom status code.
   *
   * @param code the HTTP status code
   * @param data the string data to send
   * @throws LevtusIOException if an I/O error occurs while sending the data
   */
  public void send(int code, String data) throws LevtusIOException {
    res.status(code).send(data);
  }

  /**
   * Send a plain String as data through the Response with a custom status code and content type.
   *
   * @param code the HTTP status code
   * @param contentType the MIME type of the content (e.g., "application/json")
   * @param data the string data to send
   * @throws LevtusIOException if an I/O error occurs while sending the data
   */
  public void send(int code, String contentType, String data) throws LevtusIOException {
    res.status(code).contentType(contentType);
    res.send(data);
  }

  /**
   * Sends an HTML string as the response payload, automatically setting the "Content-Type" to
   * "text/html".
   *
   * @param html the HTML formatted string (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void html(String html) throws LevtusIOException {
    res.html(html);
  }

  /**
   * Sends a plain text string as the response payload, automatically setting the "Content-Type" to
   * "text/plain".
   *
   * @param text the plain text string (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void text(String text) throws LevtusIOException {
    res.text(text);
  }

  /**
   * Sends a raw byte array as a downloadable payload, automatically setting the "Content-Type" to
   * "application/octet-stream".
   *
   * @param body the byte array representing the file or binary data (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void sendBinary(byte[] body) throws LevtusIOException {
    res.sendBinary(body);
  }

  /**
   * Sends a JSON formatted string as the response payload, automatically setting the "Content-Type"
   * to "application/json".
   *
   * @param json the serialized JSON string (must not be null)
   * @throws LevtusIOException if an I/O error occurs while writing to the socket stream
   */
  public void json(String json) throws LevtusIOException {
    res.json(json);
  }

  /**
   * Streams a file from the server's disk to the client. Safely resolves paths to prevent directory
   * traversal attacks (403 Forbidden). Automatically probes and sets the correct MIME type.
   *
   * <p>{@code ctx.render("index.html");}
   *
   * @param htmlPath the relative path of the file within the configured static directory (must not
   *     be null)
   * @throws LevtusIOException if an error occurs while accessing or streaming the file
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws PathTraversalException if the path contains traversal characters
   */
  public void render(String htmlPath)
      throws LevtusIOException, FileNotFound, PathTraversalException {
    res.render(htmlPath);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer. Uses
   * FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * @param path the absolute file path to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   */
  public void sendFile(Path path) throws LevtusIOException, FileNotFound {
    res.sendFile(path);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer. Uses
   * FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>Resolves the path relative to the configured static files directory and performs security
   * checks to prevent directory traversal attacks.
   *
   * @param path the relative file path within the static directory
   * @throws LevtusIOException if an I/O error occurs while transferring the file or if the path is
   *     invalid
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws PathTraversalException if the path contains traversal characters
   */
  public void sendFile(String path) throws LevtusIOException, FileNotFound, PathTraversalException {
    res.sendFile(path);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer for downloads. Uses
   * FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>A wrapper method that sets the content type to "application/octet-stream"
   *
   * @param path the absolute file path to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   */
  public void sendBinary(Path path) throws LevtusIOException, FileNotFound {
    res.sendBinary(path);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer for downloads. Uses
   * FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>A wrapper method that sets the content type to "application/octet-stream"
   *
   * <p>Resolves the path relative to the configured static files directory and performs security
   * checks to prevent directory traversal attacks.
   *
   * @param path the relative file path within the static directory
   * @throws LevtusIOException if an I/O error occurs while transferring the file or if the path is
   *     invalid
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws PathTraversalException if the path contains traversal characters
   */
  public void sendBinary(String path)
      throws LevtusIOException, FileNotFound, PathTraversalException {
    res.sendBinary(path);
  }

  /**
   * Returns the current default chunk size.
   *
   * @return the current chunk size
   */
  public int chunkSize() {
    return res.chunkSize();
  }

  /**
   * Set the default chunk size
   *
   * @param chunkSize the size of each chunk being sent
   * @return the current LevtusContext object to be chained
   */
  public LevtusContext withChunkSize(int chunkSize) {
    res.withChunkSize(chunkSize);
    return this;
  }

  /**
   * Changes the responding method to {@link TransferMode#CHUNKED}.
   *
   * <p>These following things will happen when you call {@link LevtusContext#stream()}:
   *
   * <ul>
   *   <li>The content length header is removed from Response
   *   <li>Will set the transfer encoding header to chunked
   *   <li>Will flush all the headers down the socker first
   * </ul>
   *
   * <p>From now on, all method that internally uses {@link Response#sendFile(Path)} or {@link
   * Response#send(byte[])} will throw an exception of {@link ChunkedTransferException} as you
   * cannot switch in the middle of response
   *
   * <p>All the viable methods of responding with Chunked mode are
   *
   * <ul>
   *   <li>{@link LevtusContext#sendChunk(byte[])} to send the chunk directly to the socket
   *       (flushed)
   *   <li>{@link LevtusContext#sendChunk(String)} to send the chunk directly to the socket
   *       (flushed)
   *   <li>{@link LevtusContext#finishChunkedResponse()} to finish the chunked response
   * </ul>
   *
   * <p>Take as a note, that {@link LevtusContext#finishChunkedResponse()} is called in finally
   * block, so it is safe whether you call it or not
   *
   * @return the current LevtusContext object to be chained
   * @throws ChunkedTransferException if stream is already called or when a bulk sending method has
   *     been called earlier
   * @throws LevtusIOException if an unexpected IO error occurs
   */
  public LevtusContext stream() throws LevtusIOException, ChunkedTransferException {
    res.stream();
    return this;
  }

  /**
   * The method to sends the chunk of data to the socket (can be chained and used multiple times).
   *
   * <p>Will send the chunk directly to the socket (flushed). This method can be chained and called
   * multiple times for sending multiple chunks
   *
   * <p>Used to specifically control the offset and length the data is sent
   *
   * @param data the data to be sent to the socker
   * @param offset the offset of the data to be sent
   * @param length the length of the data to be sent
   * @return the current LevtusContext object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext sendChunk(byte[] data, int offset, int length)
      throws LevtusIOException, ChunkedTransferException {
    res.sendChunk(data, offset, length);
    return this;
  }

  /**
   * The method to sends the chunk of data to the socket (can be chained and used multiple times).
   *
   * <p>Will send the chunk directly to the socket (flushed). This method can be chained and called
   * multiple times for sending multiple chunks
   *
   * @param data the data to be sent to the socker
   * @return the current LevtusContext object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext sendChunk(byte[] data) throws LevtusIOException, ChunkedTransferException {
    res.sendChunk(data);
    return this;
  }

  /**
   * The method to sends the chunk of data in the form of String to the socket (can be chained and
   * used multiple times).
   *
   * <p>Will send the chunk directly to the socket (flushed). This method can be chained and called
   * multiple times for sending multiple chunks
   *
   * <p>A helper method, internally calling {@link Response#sendChunk(byte[])}
   *
   * <p>Used to specifically control the offset and length the data is sent
   *
   * @param data the data to be sent to the socker
   * @param length the length of the data
   * @param offset the offset of the data
   * @return the current LevtusContext object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext sendChunk(String data, int offset, int length)
      throws LevtusIOException, ChunkedTransferException {
    res.sendChunk(data.getBytes(), offset, length);
    return this;
  }

  /**
   * The method to sends the chunk of data in the form of String to the socket (can be chained and
   * used multiple times).
   *
   * <p>Will send the chunk directly to the socket (flushed). This method can be chained and called
   * multiple times for sending multiple chunks
   *
   * <p>A helper method, internally calling {@link Response#sendChunk(byte[])}
   *
   * @param data the data to be sent to the socker
   * @return the current LevtusContext object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext sendChunk(String data) throws LevtusIOException, ChunkedTransferException {
    res.sendChunk(data.getBytes());
    return this;
  }

  /**
   * The helper method to stream file from the disk to the socket.
   *
   * <p>Will stream the file directly to the socket (flushed). This method can be chained and called
   * multiple times for streaming multiple files
   *
   * <p>Used to specifically control the size of each chunk that is sent
   *
   * @param path the path of the file being sent
   * @param chunkSize the size of each chunk
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file that is given is not found
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamFile(Path path, int chunkSize)
      throws FileNotFound, LevtusIOException, ChunkedTransferException {
    res.streamFile(path, chunkSize);
    return this;
  }

  /**
   * The helper method to stream file from the disk to the socker.
   *
   * <p>Will stream the file directly to the socket (flushed). This method can be chained and called
   * multiple times for streaming multiple files
   *
   * <p>Automatically set the chunk size to the default chunk size {@link Response#chunkSize()}
   *
   * @param path the path of the file being sent
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file that is given is not found
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamFile(Path path)
      throws FileNotFound, LevtusIOException, ChunkedTransferException {
    res.streamFile(path);
    return this;
  }

  /**
   * The helper method to stream file from the disk to the socket.
   *
   * <p>Will stream the file directly to the socket (flushed). This method can be chained and called
   * multiple times for streaming multiple files
   *
   * <p>Used to specifically control the size of each chunk that is sent
   *
   * @param path the path of the file being sent relative to {@link Response#staticFilesPath}
   * @param chunkSize the size of each chunk
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file that is given is not found
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamFile(String path, int chunkSize)
      throws LevtusIOException, ChunkedTransferException, PathTraversalException, FileNotFound {
    res.streamFile(path, chunkSize);
    return this;
  }

  /**
   * The helper method to stream file from the disk to the socker.
   *
   * <p>Will stream the file directly to the socket (flushed). This method can be chained and called
   * multiple times for streaming multiple files
   *
   * <p>Automatically set the chunk size to the default chunk size {@link Response#chunkSize()}
   *
   * @param path the path of the file being sent relative to {@link Response#staticFilesPath}
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file that is given is not found
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamFile(String path)
      throws LevtusIOException, ChunkedTransferException, PathTraversalException, FileNotFound {
    res.streamFile(path);
    return this;
  }

  /**
   * The method to stream from an input stream to the socket output stream.
   *
   * <p>Will stream the input stream directly to the socket (flushed). This method can be chained
   * and called multiple times for streaming multiple files
   *
   * <p>Used to specifically control the size of each chunk that is sent
   *
   * @param is the input stream for the data source
   * @param chunkSize the size of each chunk being sent
   * @return the current LevtusContext object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamFrom(InputStream is, int chunkSize)
      throws LevtusIOException, ChunkedTransferException {
    res.streamFrom(is, chunkSize);
    return this;
  }

  /**
   * The method to stream from an input stream to the socket output stream.
   *
   * <p>Will stream the input stream directly to the socket (flushed). This method can be chained
   * and called multiple times for streaming multiple files
   *
   * <p>Automatically set the chunk size to the default chunk size {@link Response#chunkSize()}
   *
   * @param is the input stream for the data source
   * @return the current LevtusContext object to be chained
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamFrom(InputStream is)
      throws LevtusIOException, ChunkedTransferException {
    res.streamFrom(is);
    return this;
  }

  /**
   * The method to send the final CRLF to end the current Response.
   *
   * <p>Internally called by the HttpConnectionHandler, it is optional to call this method or leave
   * it as is
   *
   * @throws LevtusIOException if an unexpected IO error occurs
   */
  public void finishChunkedResponse() throws LevtusIOException {
    res.finishChunkedResponse();
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer as a downloadable. Uses
   * FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>Internally calls {@link Response#sendFile(Path)}
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the file path to send
   * @param filename the name of the file to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   */
  public void downloadFile(Path path, String filename)
      throws LevtusIOException, FileNotFound {
    res.downloadFile(path, filename);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer as a downloadable. Uses
   * FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>Internally calls {@link Response#sendFile(Path)}
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the relative file path to send
   * @param filename the name of the file to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws PathTraversalException if the path contains traversal characters
   */
  public void downloadFile(String path, String filename)
      throws LevtusIOException, FileNotFound, PathTraversalException {
    res.downloadFile(path, filename);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer as a downloadable. Uses
   * FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>Internally calls {@link Response#sendFile(Path)}
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the file path to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   */
  public void downloadFile(Path path) throws LevtusIOException, FileNotFound {
    res.downloadFile(path);
  }

  /**
   * Sends a file from disk to the client using zero-copy NIO transfer as a downloadable. Uses
   * FileChannel.transferTo() for efficient file-to-socket transfer.
   *
   * <p>Internally calls {@link Response#sendFile(Path)}
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the relative file path to send
   * @throws LevtusIOException if an I/O error occurs while transferring the file
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws PathTraversalException if the path contains traversal characters
   */
  public void downloadFile(String path)
      throws LevtusIOException, FileNotFound, PathTraversalException {
    res.downloadFile(path);
  }

  /**
   * Streams a file from disk to the socket in chunked mode as a download link.
   *
   * <p>Uses buffered transfer (not zero-copy) with configurable chunk size. The file is read into a
   * buffer and sent as chunks with proper chunk headers. This is different from {@link
   * #sendFile(Path)} which uses zero-copy NIO transfer and cannot be used in chunked mode.
   *
   * <p>This method can either be called without {@link Response#stream() or with it:}
   *
   * <ul>
   *   <li>Implicit call: {@code ctx.streamDownloadFile(path, filename, chunkSize); } - Internally
   *       calls strea,
   *   <li>Explicit call: {@code ctx.download(filename).stream().streamDownloadFile(path, filename,
   *       chunkSize); } - Explicitly calls download with filename before stream. As stream flushed
   *       the headers
   *   <li>Middle ground: {@code ctx.download(filename).streamDownloadFile(path, filename,
   *       chunkSize); } - You can do this, but it is not recommended as Implicit calls already
   *       internally checks the filename for you
   * </ul>
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the absolute path of the file being sent
   * @param filename the name of the file to send
   * @param chunkSize the size of each chunk in bytes
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamDownloadFile(Path path, String filename, int chunkSize)
      throws FileNotFound, LevtusIOException, ChunkedTransferException {
    res.streamDownloadFile(path, filename, chunkSize);
    return this;
  }

  /**
   * Streams a file from disk to the socket in chunked mode using the default chunk size as a
   * download link.
   *
   * <p>Uses buffered transfer (not zero-copy) with the default chunk size configured via {@link
   * #withChunkSize(int)}. The file is read into a buffer and sent as chunks with proper chunk
   * headers. This is different from {@link #sendFile(Path)} which uses zero-copy NIO transfer and
   * cannot be used in chunked mode.
   *
   * <p>This method can either be called without {@link Response#stream() or with it:}
   *
   * <ul>
   *   <li>Implicit call: {@code ctx.streamDownloadFile(path, filename, chunkSize); } - Internally
   *       calls strea,
   *   <li>Explicit call: {@code ctx.download(filename).stream().streamDownloadFile(path, filename,
   *       chunkSize); } - Explicitly calls download with filename before stream. As stream flushed
   *       the headers
   *   <li>Middle ground: {@code ctx.download(filename).streamDownloadFile(path, filename,
   *       chunkSize); } - You can do this, but it is not recommended as Implicit calls already
   *       internally checks the filename for you
   * </ul>
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the absolute path of the file being sent
   * @param filename the name of the file to send
   * @param chunkSize the size of each chunk in bytes
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   * @throws PathTraversalException if the path contains traversal characters
   */
  public LevtusContext streamDownloadFile(String path, String filename, int chunkSize)
      throws FileNotFound, LevtusIOException, ChunkedTransferException, PathTraversalException {
    res.streamDownloadFile(path, filename, chunkSize);
    return this;
  }

  /**
   * Streams a file from disk to the socket in chunked mode with configurable chunk size as a
   * download link.
   *
   * <p>Uses buffered transfer (not zero-copy) with configurable chunk size. The file path is
   * resolved relative to the configured static files directory. Path traversal attacks are
   * prevented by validating the resolved path stays within the static directory.
   *
   * <p>This method can either be called without {@link Response#stream() or with it:}
   *
   * <ul>
   *   <li>Implicit call: {@code ctx.streamDownloadFile(path, filename); } - Internally calls strea,
   *   <li>Explicit call: {@code ctx.download(filename).stream().streamDownloadFile(path, filename);
   *       } - Explicitly calls download with filename before stream. As stream flushed the headers
   *   <li>Middle ground: {@code ctx.download(filename).streamDownloadFile(path, filename); } - You
   *       can do this, but it is not recommended as Implicit calls already internally checks the
   *       filename for you
   * </ul>
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the relative path of the file within the static directory
   * @param filename the name of the file to send
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamDownloadFile(Path path, String filename)
      throws LevtusIOException, ChunkedTransferException, FileNotFound {
    res.streamDownloadFile(path, filename);
    return this;
  }

  /**
   * Streams a file from disk to the socket in chunked mode using the default chunk size as a
   * download link.
   *
   * <p>Uses buffered transfer (not zero-copy) with the default chunk size configured via {@link
   * #withChunkSize(int)}. The file path is resolved relative to the configured static files
   * directory. Path traversal attacks are prevented by validating the resolved path stays within
   * the static directory.
   *
   * <p>This method can either be called without {@link Response#stream() or with it:}
   *
   * <ul>
   *   <li>Implicit call: {@code ctx.streamDownloadFile(path, filename); } - Internally calls strea,
   *   <li>Explicit call: {@code ctx.download(filename).stream().streamDownloadFile(path, filename);
   *       } - Explicitly calls download with filename before stream. As stream flushed the headers
   *   <li>Middle ground: {@code ctx.download(filename).streamDownloadFile(path, filename); } - You
   *       can do this, but it is not recommended as Implicit calls already internally checks the
   *       filename for you
   * </ul>
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the relative path of the file within the static directory
   * @param filename the name of the file to send
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   * @throws PathTraversalException if the path contains traversal characters
   */
  public LevtusContext streamDownloadFile(String path, String filename)
      throws LevtusIOException, ChunkedTransferException, PathTraversalException, FileNotFound {
    res.streamDownloadFile(path, filename);
    return this;
  }

  /**
   * Streams a file from disk to the socket in chunked mode using the default chunk size as a
   * download link.
   *
   * <p>Uses buffered transfer (not zero-copy) with the default chunk size configured via {@link
   * #withChunkSize(int)}. The file path is resolved relative to the configured static files
   * directory. Path traversal attacks are prevented by validating the resolved path stays within
   * the static directory.
   *
   * <p>This method can either be called without {@link Response#stream() or with it:}
   *
   * <ul>
   *   <li>Implicit call: {@code ctx.streamDownloadFile(path); } - Internally calls strea,
   *   <li>Explicit call: {@code ctx.download(filename).stream().streamDownloadFile(path); } -
   *       Explicitly calls download with filename before stream. As stream flushed the headers
   *   <li>Middle ground: {@code ctx.download(filename).streamDownloadFile(path); } - You can do
   *       this, but it is not recommended as Implicit calls already internally checks the filename
   *       for you
   * </ul>
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the relative path of the file within the static directory
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   */
  public LevtusContext streamDownloadFile(Path path)
      throws LevtusIOException, ChunkedTransferException, FileNotFound {
    res.streamDownloadFile(path);
    return this;
  }

  /**
   * Streams a file from disk to the socket in chunked mode using the default chunk size as a
   * download link.
   *
   * <p>Uses buffered transfer (not zero-copy) with the default chunk size configured via {@link
   * #withChunkSize(int)}. The file path is resolved relative to the configured static files
   * directory. Path traversal attacks are prevented by validating the resolved path stays within
   * the static directory.
   *
   * <p>This method can either be called without {@link Response#stream() or with it:}
   *
   * <ul>
   *   <li>Implicit call: {@code ctx.streamDownloadFile(path); } - Internally calls strea,
   *   <li>Explicit call: {@code ctx.download(filename).stream().streamDownloadFile(path); } -
   *       Explicitly calls download with filename before stream. As stream flushed the headers
   *   <li>Middle ground: {@code ctx.download(filename).streamDownloadFile(path); } - You can do
   *       this, but it is not recommended as Implicit calls already internally checks the filename
   *       for you
   * </ul>
   *
   * <p>Internally use {@link Response#download(String)} to set the response as downloadable
   *
   * @param path the relative path of the file within the static directory
   * @return the current LevtusContext object to be chained
   * @throws FileNotFound if the file does not exist or is a directory
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws ChunkedTransferException if stream hasn't been called or had already used a normal/bulk
   *     sending method
   * @throws PathTraversalException if the path contains traversal characters
   */
  public LevtusContext streamDownloadFile(String path)
      throws LevtusIOException, ChunkedTransferException, PathTraversalException, FileNotFound {
    res.streamDownloadFile(path);
    return this;
  }

  /**
   * Add a header to signal the browser that the response should be downloaded as a file.
   *
   * @param filename the filename that will be displayed by the browser (should include the file
   *     extension)
   * @return the current LevtusContext object to be chained
   */
  public LevtusContext download(String filename) {
    res.download(filename);
    return this;
  }
}
