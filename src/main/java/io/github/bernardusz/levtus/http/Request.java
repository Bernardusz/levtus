package io.github.bernardusz.levtus.http;

import io.github.bernardusz.levtus.exception.PayloadTooLargeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
  private int bytesRead;

  /**
   * Initializes the internal state of an incoming request.
   *
   * @implNote This constructor is primarily used internally by the Levtus engine during the HTTP
   *     parsing phase.
   * @param method the HTTP method (e.g., "GET", "POST") (must not be null)
   * @param path the requested URI path (must not be null)
   * @param headers a map of HTTP headers, where keys are strictly lowercase (must not be null)
   * @param queries a map of parsed query List of String parameters (must not be null)
   * @param bodyStream the raw input stream from the client socket (must not be null)
   * @param maxBodySize the configured absolute byte limit for the payload
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
   * <p>Return the first value inside the list of values for the query parameter.</p>
   *
   * <p>{@code String id = ctx.req().query("id");}</p>
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
   * <p>{@code ArrayList<String> tag = ctx.req().queries("tag");}</p>
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
   * <p>{@code HashMap<String, List<String>> id = ctx.req().queries();}</p>
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
   * <p>{@code String accepts = ctx.req().header("Accept");}</p>
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
   * <p>{@code List<String> accepts = ctx.req().headers("Accept");}</p>
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
   * <p>{@code HashMap<String, List<String>> headers = ctx.req().headers();}</p>
   *
   * @return an unmodifiable map of headers
   */
  public Map<String, List<String>> headers() {
    return headers;
  }

  /**
   * Retrieves the total number of body bytes successfully read from the client stream so far.
   *
   * @return the current count of bytes read
   */
  public int bytesRead() {
    return bytesRead;
  }

  /**
   * Updates the internal counter tracking how many body bytes have been read.
   *
   * @param bytesRead the new byte count to set
   */
  public void setBytesRead(int bytesRead) {
    this.bytesRead = bytesRead;
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
      return Integer.parseInt(
          header("content-length").isEmpty() ? "0" : header("content-length"));
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
   * Lazily reads the incoming payload from the raw stream into a byte array and caches it. Enforces
   * the configured maximum body size limits to prevent memory exhaustion (OOM) attacks.
   *
   * @return the raw byte array of the request body
   * @throws PayloadTooLargeException if the 'Content-Length' or actual stream data exceeds {@code
   *     maxBodySize}
   * @throws UncheckedIOException if an I/O error occurs while reading the socket stream
   */
  public byte[] body() {
    if (cachedBody != null) {
      return cachedBody;
    }
    try {
      if (contentLength() > maxBodySize) {
        throw new PayloadTooLargeException("Request body is too large");
      }
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] data = new byte[8192]; // 8KB chunks
      int nRead;
      int totalRead = 0;
      while (totalRead < contentLength()
          && (nRead = bodyStream.read(data, 0, Math.min(data.length, contentLength() - totalRead)))
              != -1) {
        buffer.write(data, 0, nRead);
        totalRead += nRead;
        setBytesRead(totalRead);
        if (totalRead > maxBodySize) {
          throw new PayloadTooLargeException("HTTP Body too long (Limit: " + maxBodySize + ")");
        }
      }
      cachedBody = buffer.toByteArray();
      return cachedBody;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read request body", e);
    }
  }
}
