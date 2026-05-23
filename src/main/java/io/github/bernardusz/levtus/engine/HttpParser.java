package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.exception.BadRequestException;
import io.github.bernardusz.levtus.exception.HeaderTooLargeException;
import io.github.bernardusz.levtus.exception.LevtusNotImplementedException;
import io.github.bernardusz.levtus.exception.PayloadTooLargeException;
import io.github.bernardusz.levtus.http.Request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HttpParser {
  /**
   * Responsible for parsing the request from the client socket.
   *
   * <p>Parses:</p>
   * <ul>
   * <li>Request line</li>
   * <li>Headers</li>
   * <li>Body</li>
   * </ul>
   * Immediately throws an Exception if:
   * <ul>
   * <li>Request line is invalid</li>
   * <li>Headers are invalid</li>
   * <li>Body is invalid</li>
   * </ul>
   *
   * <p>Without waiting for all of them; one problem, Exception is thrown</p>
   *
   * @param inputStream The stream to read the HTTP request from
   * @return {@link Request} A fully parsed Request object, or null if the stream is empty
   * @throws IOException If a network or stream error occurs
   * @throws URISyntaxException If the request line is invalid
   * @throws BadRequestException If the request line is invalid or headers are malformed
   * @throws PayloadTooLargeException If the request's body size exceeds {@link LevtusEngine#getMaxBodySize}, set via {@link LevtusEngine#setMaxBodySize(int)}
   * @throws HeaderTooLargeException If the header's total size exceeds {@link LevtusEngine#getMaxHeaderSize}, set via {@link LevtusEngine#setMaxHeaderSize(int)}
   * @throws LevtusNotImplementedException If the method is not implemented.
   */
  Request parseRequest(LevtusEngine engine, InputStream inputStream) throws IOException, URISyntaxException {
    String requestLine = parseRequestLine(inputStream, engine.getMaxLineSize(), engine.getMaxEmptyLines());

    if (requestLine == null) {
      return null;
    }

    String method = parseMethod(requestLine);

    // Parse the Header
    Map<String, List<String>> headers = parseHeaders(inputStream, engine.getMaxHeaderSize(), engine.getMaxHeaderCount());

    // Parse the Body
    validateBodySize(headers, engine.getMaxBodySize());

    // Parse the Path
    String rawPath = parseRawPath(requestLine);
    String path = parsePath(rawPath);
    Map<String, List<String>> queryParams = parseQueryParams(rawPath);

    //Normalize the path
    String newPath = normalizePath(path);

    return new Request(
      method, new URI(newPath).getRawPath(), headers, queryParams, inputStream, engine.getMaxBodySize());
  }

  String parseRequestLine(InputStream inputStream, int maxLineSize, int maxEmptyLines) throws IOException{
    String requestLine;

    // Read the request line
    int b = 0;
    while ((requestLine = readLine(inputStream, maxLineSize)) != null && requestLine.isEmpty()) {
      b++;
      if (b > maxEmptyLines) {
        throw new BadRequestException("400 - Bad Request (Request too large)");
      }
    }
    return requestLine;
  }
  String parseMethod(String requestLine){
    String[] parts = requestLine.split(" ", 3);
    if (parts.length < 2) {
      throw new BadRequestException("400 - Bad Request");
    }

    return parts[0];
  }
  Map<String, List<String>> parseHeaders(InputStream inputStream, int maxHeaderSize, int maxHeaderCount) throws IOException {
    String header;
    int totalHeaderSize = 0;
    Map<String, List<String>> headers = new HashMap<>();
    while ((header = readLine(inputStream, maxHeaderSize)) != null && !(header.isEmpty())) {
      totalHeaderSize += header.length();
      if (totalHeaderSize > maxHeaderSize) {
        throw new HeaderTooLargeException("Header too large");
      }
      if (headers.size() > maxHeaderCount) {
        throw new HeaderTooLargeException("Too many headers");
      }

      String[] headerParts = header.split(":", 2);
      if (headerParts.length == 2) {
        headers
          .computeIfAbsent(headerParts[0].toLowerCase().trim(), _ -> new ArrayList<>())
          .add(headerParts[1].trim());
      }
    }
    if (headers.get("host") == null) {
      throw new BadRequestException("400 - Bad Request (Missing host header)");
    }
    if (headers.get("transfer-encoding") != null) {
      throw new LevtusNotImplementedException(headers.get("transfer-encoding").getFirst());
    }

    return headers;
  }
  void validateBodySize(Map<String, List<String>> headers, int maxBodySize) throws PayloadTooLargeException {
    int contentLength;
    List<String> lengthStrList =
      headers.getOrDefault("content-length", new ArrayList<>(List.of("0")));
    if (lengthStrList.size() > 1) {
      throw new BadRequestException("400 - Bad Request (Multiple content-length headers)");
    }
    String lengthStr = lengthStrList.getFirst();
    if (lengthStr != null && !lengthStr.isEmpty()) {
      try {
        contentLength = Integer.parseInt(lengthStr);
        if (contentLength > maxBodySize) {
          throw new PayloadTooLargeException(
            "Payload Too Large: " + contentLength + " exceeds limit of " + maxBodySize);
        }
      } catch (NumberFormatException e) {
        // Ignore invalid content-length
      }
    }
  }
  String parseRawPath(String requestLine) throws BadRequestException {
    String rawPath = requestLine.split(" ", 3)[1];

    if (rawPath.startsWith("http")) {
      rawPath = rawPath.substring(rawPath.indexOf("//") + 2); // Strip until the https/http
      // Strip domain name
      rawPath = rawPath.substring(!rawPath.contains("/") ? 0 : rawPath.indexOf("/"));
      if (rawPath.equals("/") || rawPath.isEmpty()) {
        rawPath = "/";
      }
    }
    if (!rawPath.contains("/")) {
      rawPath = "/";
    }

    return rawPath;
  }
  String parsePath(String rawPath) throws BadRequestException {
    String path;
    if (rawPath.contains("?")) {
      int queryStart = rawPath.indexOf("?");
      path = rawPath.substring(0, queryStart);

    } else {
      path = rawPath;
    }
    return path;
  }
  Map<String, List<String>> parseQueryParams(String rawPath){
    Map<String, List<String>> queryParams = new HashMap<>();
    int queryStart = rawPath.indexOf("?");
    if (queryStart == -1) {
      return queryParams;
    }
    String queryString = rawPath.substring(queryStart + 1);

    for (String query : queryString.split("&")) {
      String[] pair = query.split("=", 2);
      if (pair.length == 2) {
        queryParams
          .computeIfAbsent(utf8Decoder(pair[0]), _ -> new ArrayList<>())
          .add(utf8Decoder(pair[1]));
      } else if (pair.length >= 1 && !pair[0].isEmpty()) {
        queryParams.computeIfAbsent(utf8Decoder(pair[0]), _ -> new ArrayList<>()).add("");
      }
    }
    return  queryParams;
  }
  String normalizePath(String path) {
    URI uri;
    String newPath;
    try {
      uri = new URI(path).normalize();
      // Now after http is gone, we need to fix every instance of double and trailing slashes
      newPath = (uri.toString()).replaceAll("/{2,}", "/");
    } catch (URISyntaxException e) {
      throw new BadRequestException("400 - Bad Request");
    }
    return newPath;
  }

  String readLine(InputStream inputStream, int maxLineSize) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int b;
    int count = 0;
    while ((b = inputStream.read()) != -1) {
      if (b == '\n') break;
      if (b == '\r') continue;

      buffer.write(b);
      count++;

      if (count > maxLineSize) {
        throw new HeaderTooLargeException("HTTP Header line too long (Limit: " + maxLineSize + ")");
      }
    }
    if (b == -1) return null;
    return buffer.toString(StandardCharsets.UTF_8);
  }
  String utf8Decoder(String body) throws IllegalArgumentException {
    return URLDecoder.decode(body, StandardCharsets.UTF_8);
  }
}
