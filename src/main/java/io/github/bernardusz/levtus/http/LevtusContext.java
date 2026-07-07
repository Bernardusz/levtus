package io.github.bernardusz.levtus.http;

import io.github.bernardusz.levtus.exception.developer.BodyAlreadyConsumedException;
import io.github.bernardusz.levtus.exception.developer.FileNotFound;
import io.github.bernardusz.levtus.exception.developer.LevtusIOException;
import io.github.bernardusz.levtus.exception.developer.PathTraversalException;
import io.github.bernardusz.levtus.exception.http.PayloadTooLargeException;
import io.github.bernardusz.levtus.io.LevtusInputStream;
import io.github.bernardusz.levtus.io.StreamConsumer;
import java.io.IOException;
import java.io.UncheckedIOException;
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
   * @throws UncheckedIOException if an I/O error occurs while reading the socket stream
   * @throws PayloadTooLargeException if the 'Content-Length' or actual stream data exceeds {@code
   *     maxBodySize}
   */
  public LevtusInputStream bodyStream() {
    return req.bodyStream();
  }

  public void bodyStream(StreamConsumer consumer) {
    try (LevtusInputStream lis = this.bodyStream()) {
      consumer.consume(lis);
    } catch (UncheckedIOException e) {
      // If it's already wrapped by your stream, unwrap or pass it through
      throw e;
    } catch (IOException e) {
      // If the developer's lambda logic threw a raw checked IOException
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
   * @throws UncheckedIOException if an I/O error occurs while reading the socket stream
   * @throws BodyAlreadyConsumedException if the body has already been consumed
   */
  public byte[] body() {
    return req.body();
  }

  /**
   * Retrieves the body of the request in form of String. Enforces the configured maximum body size
   * limits to prevent memory exhaustion (OOM) attacks.
   *
   * <p>{@code String body = ctx.bodyAsString();}
   *
   * @return the body of the request as a String
   */
  public String bodyAsString() {
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
}
