package io.github.bernardusz.levtus.http;

import io.github.bernardusz.levtus.exception.developer.BodyAlreadyConsumedException;
import io.github.bernardusz.levtus.exception.developer.LevtusIOException;
import io.github.bernardusz.levtus.exception.http.PayloadTooLargeException;
import io.github.bernardusz.levtus.io.LevtusInputStream;
import io.github.bernardusz.levtus.io.StreamConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Represents an incoming HTTP/1.1 request.
 *
 * <p>Provides a fluent interface to access the HTTP method, URI path, headers, query parameters,
 * and allows for lazy-loading of the request payload.
 */
public class Request {
  private final String method;
  private final String path;
  private final Map<String, List<String>> headers;
  private final Map<String, List<String>> queries;
  private final InputStream bodyStream;
  private final int maxBodySize;
  private byte[] cachedBody;
  private LevtusInputStream activeStream;
  private boolean streamConsumed = false;

  /**
   * Initializes the internal state of an incoming request.
   *
   * @param method      the HTTP method (e.g., "GET", "POST") (must not be null)
   * @param path        the requested URI path (must not be null)
   * @param headers     a map of HTTP headers, where keys are strictly lowercase (must not be null)
   * @param queries     a map of parsed query List of String parameters (must not be null)
   * @param bodyStream  the raw input stream from the client socket (must not be null)
   * @param maxBodySize the configured absolute byte limit for the payload
   * @implNote This constructor is primarily used internally by the Levtus engine during the HTTP
   * parsing phase.
   */
  public Request(
      String method,
      String path,
      Map<String, List<String>> headers,
      Map<String, List<String>> queries,
      InputStream bodyStream,
      int maxBodySize) {
    this.method = method;
    this.path = path;
    this.headers = headers;
    this.queries = queries;
    this.bodyStream = bodyStream;
    this.maxBodySize = maxBodySize;
  }

  /**
   * Retrieves the HTTP method used for this request.
   *
   * @return the uppercase HTTP method string (e.g., "GET", "POST", "PUT")
   */
  public String method() {
    return method;
  }

  /**
   * Retrieves the requested URI path without the query string.
   *
   * @return the exact path string (e.g., "/api/users")
   */
  public String path() {
    return path;
  }

  /**
   * Retrieves the value - a String - of a specific query parameter by its key.
   *
   * <p>Return the first value inside the list of values for the query parameter.
   *
   * <p>{@code String id = ctx.req().query("id");}
   *
   * @param key The header key
   * @return The first value for the header, or an empty string if the key does not exist
   */
  public String query(String key) {
    List<String> resultList = queries(key);

    if (resultList == null || resultList.isEmpty()) {
      return "";
    }
    return resultList.getFirst();
  }

  /**
   * Retrieves the value - a List of String - of a specific query parameter by its key.
   *
   * <p>{@code ArrayList<String> tag = ctx.req().queries("tag");}
   *
   * @param key the query parameter name (must not be null)
   * @return the associated value, or an empty List if the key does not exist
   */
  public List<String> queries(String key) {
    return queries.getOrDefault(key, List.of());
  }

  /**
   * Retrieves the complete map of parsed URL query parameters.
   *
   * <p>{@code HashMap<String, List<String>> id = ctx.req().queries();}
   *
   * @return a map of query parameters, empty if no query parameters are present
   */
  public Map<String, List<String>> queries() {
    return queries != null ? queries : Map.of();
  }

  /**
   * Retrieves the first value associated with a specific HTTP header. Header name resolution is
   * case-insensitive.
   *
   * <p>{@code String accepts = ctx.req().header("Accept");}
   *
   * @param name the target header name (must not be null)
   * @return a list of header values, or an empty list if the header is not present
   */
  public String header(String name) {
    List<String> resultList = headers(name);

    if (resultList == null || resultList.isEmpty()) {
      return "";
    }
    return resultList.getFirst();
  }

  /**
   * Retrieves all values associated with a specific HTTP header. Header name resolution is
   * case-insensitive.
   *
   * <p>{@code List<String> accepts = ctx.req().headers("Accept");}
   *
   * @param name the target header name (must not be null)
   * @return a list of header values, or an empty list if the header is not present
   */
  public List<String> headers(String name) {
    return headers.getOrDefault(name.toLowerCase(), List.of());
  }

  /**
   * Retrieves the complete map of HTTP headers associated with this request.
   *
   * <p>{@code HashMap<String, List<String>> headers = ctx.req().headers();}
   *
   * @return an unmodifiable map of headers
   */
  public Map<String, List<String>> headers() {
    return headers;
  }

  /**
   * Determines the MIME type of the request payload based on the 'Content-Type' header. Defaults to
   * "text/plain" if the header is missing.
   *
   * @return the resolved content type string
   */
  public String contentType() {
    return header("content-type").isEmpty() ? "text/plain" : header("content-type");
  }

  /**
   * Parses the 'Content-Length' header to determine the expected size of the request body. Defaults
   * to 0 if the header is missing or malformed.
   *
   * @return the expected body size in bytes
   */
  public int contentLength() {
    try {
      return Integer.parseInt(header("content-length").isEmpty() ? "0" : header("content-length"));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Checks whether the request body has already been fully read and stored in memory.
   *
   * @return {@code true} if the body is cached, {@code false} otherwise
   */
  public boolean isCached() {
    return cachedBody != null;
  }

  /**
   * Retrieves the total number of body bytes successfully read from the client stream so far.
   *
   * @return the current count of bytes read
   */
  public long bytesRead() {
    return activeStream != null ? activeStream.getBytesRead() : 0;
  }

  /**
   * Retrieves the body stream of the request.
   * {@code LevtusInputStream bodyStream = ctx.bodyStream();}
   * {@code InputStream bodyStream = ctx.bodyStream();}
   *
   * @return the body stream from the socket {@link LevtusInputStream}
   * @throws BodyAlreadyConsumedException if the body has already been consumed
   * @throws UncheckedIOException         if an I/O error occurs while reading the socket stream
   * @throws PayloadTooLargeException     if the 'Content-Length' or actual stream data exceeds {@code
   *                                      maxBodySize}
   */
  public LevtusInputStream bodyStream() {
    if (this.cachedBody != null) {
      throw new BodyAlreadyConsumedException(
          "The request body has already been consumed and cached via body(). "
              + "You cannot access the raw stream now.");
    }
    // Constraint: If they already grabbed the stream once before, block them
    if (this.streamConsumed) {
      throw new BodyAlreadyConsumedException(
          "The request body stream has already been exclusively consumed.");
    }
    if (this.activeStream == null) {
      this.activeStream = new LevtusInputStream(this.bodyStream, this.maxBodySize, this.contentLength());
    }


    this.streamConsumed = true;
    return this.activeStream;
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
   * {@code byte[] body = ctx.body();}
   *
   * @return the raw byte array of the request body
   * @throws PayloadTooLargeException     if the 'Content-Length' or actual stream data exceeds {@code
   *                                      maxBodySize}
   * @throws UncheckedIOException         if an I/O error occurs while reading the socket stream
   * @throws BodyAlreadyConsumedException if the body has already been consumed
   */
  public byte[] body() {
    if (isCached()) {
      return cachedBody;
    }
    try {
      if (this.streamConsumed) {
        throw new BodyAlreadyConsumedException(
            "Cannot access body(). The stream has already been exclusively claimed via bodyStream()."
        );
      }

      InputStream stream = bodyStream();
      cachedBody = stream.readAllBytes();
      return cachedBody;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read request body", e);
    }
  }

  /**
   * Retrieves the body of the request in form of String. Enforces
   * the configured maximum body size limits to prevent memory exhaustion (OOM) attacks.
   * <p>
   * {@code String body = ctx.bodyAsString();}
   *
   * @return the body of the request as a String
   */
  public String bodyAsString() {
    return new String(body(), StandardCharsets.UTF_8);
  }
}
