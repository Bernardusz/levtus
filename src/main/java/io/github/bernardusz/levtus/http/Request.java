package io.github.bernardusz.levtus.http;

import io.github.bernardusz.levtus.exception.PayloadTooLargeException;
import java.io.*;
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
  private final Map<String, String> queryParams;
  private final InputStream bodyStream;
  private byte[] cachedBody;
  private int bytesRead;
  private final int maxBodySize;

  /**
   * Initializes the internal state of an incoming request.
   *
   * @implNote This constructor is primarily used internally by the Levtus engine during the HTTP
   *     parsing phase.
   * @param method the HTTP method (e.g., "GET", "POST") (must not be null)
   * @param path the requested URI path (must not be null)
   * @param headers a map of HTTP headers, where keys are strictly lowercase (must not be null)
   * @param queryParams a map of parsed query string parameters (must not be null)
   * @param bodyStream the raw input stream from the client socket (must not be null)
   * @param maxBodySize the configured absolute byte limit for the payload
   */
  public Request(
      String method,
      String path,
      Map<String, List<String>> headers,
      Map<String, String> queryParams,
      InputStream bodyStream,
      int maxBodySize) {
    this.method = method;
    this.path = path;
    this.headers = headers;
    this.queryParams = queryParams;
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
   * Retrieves the complete map of HTTP headers associated with this request.
   *
   * @return an unmodifiable map of headers
   */
  public Map<String, List<String>> headers() {
    return headers;
  }

  /**
   * Retrieves the complete map of parsed URL query parameters.
   *
   * @return a map of query parameters
   */
  public Map<String, String> queryParams() {
    return queryParams;
  }

  /**
   * Retrieves all values associated with a specific HTTP header. Header name resolution is
   * case-insensitive.
   *
   * <p>{@code List<String> accepts = req.getHeaders("Accept");}
   *
   * @param name the target header name (must not be null)
   * @return a list of header values, or an empty list if the header is not present
   */
  public List<String> getHeaders(String name) {
    return headers.getOrDefault(name.toLowerCase(), List.of());
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
    return getHeaders("content-type").isEmpty()
        ? "text/plain"
        : getHeaders("content-type").getFirst();
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
          getHeaders("content-length").isEmpty() ? "0" : getHeaders("content-length").getFirst());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Retrieves the value of a specific query parameter by its key.
   *
   * <p>{@code String id = req.query("id");}
   *
   * @param key the query parameter name (must not be null)
   * @return the associated value, or an empty string if the key does not exist
   */
  public String query(String key) {
    return queryParams.getOrDefault(key, "");
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
    if (cachedBody != null) return cachedBody;
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
