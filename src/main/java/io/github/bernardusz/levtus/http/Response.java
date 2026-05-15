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

/** The type Response. */
public class Response {
  private final BufferedOutputStream output;

  /** The Static files path. */
  String staticFilesPath;

  private int statusCode = 200;
  private Map<String, List<String>> headers = new HashMap<>();
  private boolean isSent = false;

  /**
   * Instantiates a new Response.
   *
   * @param output the output to the client/socket
   * @param staticFilesPath the static files path/directory
   */
  public Response(BufferedOutputStream output, String staticFilesPath) {
    this.output = output;
    this.staticFilesPath = staticFilesPath;
    headers.computeIfAbsent("Content-Type", _ -> new ArrayList<>(List.of("text/plain")));
    headers.computeIfAbsent("Server", _ -> new ArrayList<>(List.of("Levtus-v0.1")));
  }

  /**
   * Status response.
   *
   * @param code the status code for Response
   * @return the Response object
   */
  public Response status(int code) {
    this.statusCode = code;
    return this;
  }

  /**
   * Return the Content type of the response.
   *
   * @param type the type
   */
  public void contentType(String type) {
    headers.put("Content-Type", new ArrayList<>(List.of(type)));
  }

  /**
   * Add header to the response.
   *
   * @param name the name
   * @param value the value
   * @return the response
   */
  public Response addHeader(String name, String value) {
    headers.computeIfAbsent(name, _ -> new ArrayList<>());
    headers.get(name).add(value);
    return this;
  }

  /**
   * Is sent boolean.
   *
   * <p>Checks whether the Response has been sent.
   *
   * @return the boolean
   */
  public boolean isSent() {
    return isSent;
  }

  /**
   * Send the body of the response (byte array).
   *
   * @param body the body of response (String)
   */
  public void send(String body) {
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Send the body of the response (HTML String).
   *
   * @param body the body of response (HTML String)
   */
  public void html(String body) {
    contentType("text/html");
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Send the body of the response (Plain Text).
   *
   * @param body the body of response (Plain Text)
   */
  public void text(String body) {
    contentType("text/plain");
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Send the body of the response (Byte array) - Will be sent as downloadable.
   *
   * @param body the body of response (Byte array)
   */
  public void sendBinary(byte[] body) {
    contentType("application/octet-stream");
    send(body);
  }

  /**
   * Send the body of the response (JSON String).
   *
   * @param body the body of response (JSON)
   */
  public void json(String body) {
    contentType("application/json");
    send(body.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Send the body of the response (HTML file).
   *
   * @param htmlPath the path of HTML file
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
   * Send the body of the response to the client.
   *
   * @param bodyBytes the body bytes
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
