package io.github.bernardusz.levtus.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an outgoing HTTP/1.1 response.
 *
 * <p>Provides a fluent API to construct and send data back to the client. It handles headers,
 * status codes, and various payload formats (text, JSON, HTML, binary files).
 */
public class Response {
  private final BufferedOutputStream output;

  /** The base directory path from which static files are served. */
  String staticFilesPath;

  int statusCode = 200;
  Map<String, List<String>> headers = new HashMap<>();
  private boolean isSent = false;

  /**
   * Initializes a new HTTP response bound to a client socket's output stream.
   *
   * @implNote Automatically injects default headers like "Content-Type" and "Server".
   * @param output the buffered output stream connected to the client (must not be null)
   * @param staticFilesPath the directory path for resolving static files (must not be null)
   */
  public Response(BufferedOutputStream output, String staticFilesPath) {
    this.output = output;
    this.staticFilesPath = staticFilesPath;
    headers.computeIfAbsent("Content-Type", _ -> new ArrayList<>(List.of("text/plain")));
    headers.computeIfAbsent("Server", _ -> new ArrayList<>(List.of("Levtus-v0.1")));
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
   * <p>Overwrites existing keys but preserves unique existing headers.</p>
   *
   * @param headers the full Map of headers
   * @return the current Response instance for method chaining
   */
  public Response headers(Map<String, List<String>> headers) {
    this.headers.putAll(headers); // We do this because we want batch-config by user to be authoritative
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
   */
  public void send(String body) {
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Sends an HTML string as the response payload, automatically setting the "Content-Type" to
   * "text/html".
   *
   * @param body the HTML formatted string (must not be null)
   */
  public void html(String body) {
    contentType("text/html");
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Sends a plain text string as the response payload, automatically setting the "Content-Type" to
   * "text/plain".
   *
   * @param body the plain text string (must not be null)
   */
  public void text(String body) {
    contentType("text/plain");
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Sends a raw byte array as a downloadable payload, automatically setting the "Content-Type" to
   * "application/octet-stream".
   *
   * @param body the byte array representing the file or binary data (must not be null)
   */
  public void sendBinary(byte[] body) {
    contentType("application/octet-stream");
    send(body);
  }

  /**
   * Sends a JSON formatted string as the response payload, automatically setting the "Content-Type"
   * to "application/json".
   *
   * @param body the serialized JSON string (must not be null)
   */
  public void json(String body) {
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
   * @throws UncheckedIOException if an error occurs while accessing or streaming the file
   */
  public void render(String htmlPath) {
    Path filePath = Path.of(staticFilesPath, htmlPath).normalize();

    Path rootPath = Path.of(staticFilesPath).toAbsolutePath().normalize();
    if (!filePath.toAbsolutePath().startsWith(rootPath)) {
      this.status(403).send("403 Forbidden");
      return;
    }

    if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
      try {
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) mimeType = "application/octet-stream";
        this.contentType(mimeType);
        this.status(200);

        if (isSent) return;
        isSent = true;

        writeStatus(statusCode);
        writeHeaders(Files.size(filePath));
        Files.copy(filePath, output);
        output.flush();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    } else {
      this.status(404).send("404 Not Found");
    }
  }

  /**
   * Transmits the final HTTP status line, headers, and body bytes to the client socket.
   *
   * @implNote Flushes the stream immediately after writing. Subsequent calls to any send method
   *     will be ignored.
   * @param bodyBytes the complete raw payload (must not be null)
   */
  public void send(byte[] bodyBytes) {
    if (isSent) return;
    isSent = true;
    try {
      // HTTP Status
      writeStatus(statusCode);
      writeHeaders(bodyBytes.length);
      writeBody(bodyBytes);
      output.flush();
    } catch (IOException e) {
      System.err.println("Failed to send response: " + e.getMessage());
    }
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
