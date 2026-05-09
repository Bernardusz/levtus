package io.github.bernardusz.levtus.http;

import io.github.bernardusz.levtus.exception.PayloadTooLargeException;
import java.io.*;
import java.util.List;
import java.util.Map;

/** The type Request. */
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
   * Instantiates a new Request.
   *
   * @param method the method
   * @param path the path
   * @param headers the headers
   * @param queryParams the query params
   * @param bodyStream the body stream
   * @param maxBodySize the max body size
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
   * Method string.
   *
   * @return the string
   */
  public String method() {
    return method;
  }

  /**
   * Path string.
   *
   * @return the string
   */
  public String path() {
    return path;
  }

  /**
   * Headers map.
   *
   * @return the map
   */
  public Map<String, List<String>> headers() {
    return headers;
  }

  /**
   * Query params map.
   *
   * @return the map
   */
  public Map<String, String> queryParams() {
    return queryParams;
  }

  /**
   * Gets headers.
   *
   * @param name the name
   * @return the headers
   */
  public List<String> getHeaders(String name) {
    return headers.getOrDefault(name.toLowerCase(), List.of());
  }

  /**
   * Bytes read int.
   *
   * @return the int
   */
  public int bytesRead() {
    return bytesRead;
  }

  /**
   * Sets bytes read.
   *
   * @param bytesRead the bytes read
   */
  public void setBytesRead(int bytesRead) {
    this.bytesRead = bytesRead;
  }

  /**
   * Content type string.
   *
   * @return the string
   */
  public String contentType() {
    return getHeaders("content-type").isEmpty()
        ? "text/plain"
        : getHeaders("content-type").getFirst();
  }

  /**
   * Content length int.
   *
   * @return the int
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
   * Query string.
   *
   * @param key the key
   * @return the string
   */
  public String query(String key) {
    return queryParams.getOrDefault(key, "");
  }

  /**
   * Is cached boolean.
   *
   * @return the boolean
   */
  public boolean isCached() {
    return cachedBody != null;
  }

  /**
   * Body byte [ ].
   *
   * @return the byte [ ]
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
