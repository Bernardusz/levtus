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
   * <p>Parses:
   *
   * <ul>
   *   <li>Request line
   *   <li>Headers
   *   <li>Body
   * </ul>
   *
   * Immediately throws an Exception if:
   *
   * <ul>
   *   <li>Request line is invalid
   *   <li>Headers are invalid
   *   <li>Body is invalid
   * </ul>
   *
   * <p>Without waiting for all of them; one problem, Exception is thrown
   *
   * @param inputStream The stream to read the HTTP request from
   * @return {@link Request} A fully parsed Request object, or null if the stream is empty
   * @throws IOException If a network or stream error occurs
   * @throws URISyntaxException If the request line is invalid
   * @throws BadRequestException If the request line is invalid or headers are malformed
   * @throws PayloadTooLargeException If the request's body size exceeds {@link
   *     LevtusEngine#getMaxBodySize()}, set via {@link LevtusEngine#setMaxBodySize(int)}
   * @throws HeaderTooLargeException If the header's total size exceeds {@link
   *     LevtusEngine#getMaxHeaderSize()}, set via {@link LevtusEngine#setMaxHeaderSize(int)}
   * @throws LevtusNotImplementedException If the method is not implemented.
   */
  Request parseRequest(HttpConnectionHandler handler, InputStream inputStream)
      throws IOException, URISyntaxException, BadRequestException, PayloadTooLargeException,  HeaderTooLargeException, LevtusNotImplementedException {
    String requestLine =
        parseRequestLine(inputStream, handler.getMaxLineSize(), handler.getMaxEmptyLines());

    if (requestLine == null) {
      return null;
    }

    String method = parseMethod(requestLine);

    // Parse the Header
    Map<String, List<String>> headers =
        parseHeaders(inputStream, handler.getMaxHeaderSize(), handler.getMaxHeaderCount());

    // Parse the Body
    validateBodySize(headers, handler.getMaxBodySize());

    // Parse the Path
    String rawPath = parseRawPath(requestLine);
    String path = parsePath(rawPath);
    Map<String, List<String>> queryParams = parseQueryParams(rawPath);

    // Normalize the path
    String newPath = normalizePath(path);

    return new Request(
        method,
        new URI(newPath).getRawPath(),
        headers,
        queryParams,
        inputStream,
        handler.getMaxBodySize());
  }

  /**
   * The helper method for parsing the request line of an HTTP request.
   *
   * @param inputStream the socket inputStream that allows us to get the first line
   * @param maxLineSize the maximum size (the number of characters) of a line in an HTTP request
   * @param maxEmptyLines the maximum number of empty lines allowed in an HTTP request
   * @return the parsed request line
   * @throws IOException if a network or stream error occurs.
   * @throws BadRequestException if the request line is invalid
   */
  String parseRequestLine(InputStream inputStream, int maxLineSize, int maxEmptyLines)
      throws IOException, BadRequestException {
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

  /**
   * The helper method for parsing the method of an HTTP request.
   *
   * @param requestLine the parsed request line
   * @return the parsed method
   * @throws BadRequestException if the request line is invalid
   */
  String parseMethod(String requestLine) throws BadRequestException {
    String[] parts = requestLine.split(" ", 3);
    if (parts.length < 2) {
      throw new BadRequestException("400 - Bad Request");
    }

    return parts[0];
  }

  /**
   * The helper method for parsing the headers of an HTTP request.
   *
   * @param inputStream the input stream that allows us to get the headers in an HTTP request
   * @param maxHeaderSize the maximum size of a header in an HTTP request
   * @param maxHeaderCount the maximum amount of headers in an HTTP request
   * @return the parsed headers
   * @throws IOException if a network or stream error occurs.
   * @throws HeaderTooLargeException if the header is too large
   * @throws BadRequestException if the header is invalid
   * @throws LevtusNotImplementedException if the header is not implemented
   */
  Map<String, List<String>> parseHeaders(
      InputStream inputStream, int maxHeaderSize, int maxHeaderCount) throws IOException, HeaderTooLargeException, BadRequestException, LevtusNotImplementedException {
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

  /**
   * The helper method to validate the body size of an HTTP request.
   *
   * <p>This helper method throws a PayloadTooLargeException if the body size exceeds the maximum allowed size {@link  LevtusEngine#getMaxBodySize()}.</p>
   * <p>This helper method throws a BadRequestException if the body size is invalid or exceeds the maximum allowed size.</p>
   *
   * @param headers the headers of an HTTP request
   * @param maxBodySize the maximum body size in an HTTP request
   * @throws PayloadTooLargeException if the body size exceeds the maximum allowed size {@link  LevtusEngine#getMaxBodySize()}
   * @throws BadRequestException if more than one Content-Length header is present
   */
  void validateBodySize(Map<String, List<String>> headers, int maxBodySize)
      throws PayloadTooLargeException, BadRequestException {
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

  /**
   * The helper method to parse the raw path into a valid path of an HTTP request.
   *
   * <p>The raw path is the path of an HTTP request that is received from a client.</p>
   * <p>The parsed path is the path of an HTTP request that is used to match a route.</p>
   * <p>The path returned hasn't been split between the path and the query parameters.</p>
   *
   * @param requestLine the request line in an HTTP request
   * @return the parsed "valid" path of an HTTP request
   */
  String parseRawPath(String requestLine){
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

  /**
   * The helper method to parse the real path from query params.
   *
   * @param rawPath the parsed "valid" path of an HTTP request
   * @return the real path of an HTTP request
   */
  String parsePath(String rawPath) {
    String path;
    if (rawPath.contains("?")) {
      int queryStart = rawPath.indexOf("?");
      path = rawPath.substring(0, queryStart);

    } else {
      path = rawPath;
    }
    return path;
  }

  /**
   * The helper method to parse query params from the "valid" path.
   *
   * <p>May also return an empty map if there are no query params.</p>
   *
   * @param rawPath the "valid" path
   * @return the query params
   */
  Map<String, List<String>> parseQueryParams(String rawPath) {
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
    return queryParams;
  }

  /**
   * The helper method to normalize the path to a safe path.
   *
   * @param path the parsed and real path
   * @return the normalized path
   * @throws BadRequestException if the path is invalid
   */
  String normalizePath(String path) throws BadRequestException {
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

  /**
   * The helper method to read a line in an HTTP request
   *
   * @param inputStream the input stream to read from the HTTP request
   * @param maxLineSize the maximum line size (the number of characters) in an HTTP request
   * @return the line read from the HTTP request
   * @throws IOException if an I/O error occurs
   * @throws HeaderTooLargeException if the header line is too long
   */
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

  /**
   * The helper method to decode a string from UTF-8
   *
   * @param body the String that needs to be decoded
   * @return a decoded String
   * @throws IllegalArgumentException if the string is not a valid UTF-8 string
   */
  String utf8Decoder(String body) throws IllegalArgumentException {
    return URLDecoder.decode(body, StandardCharsets.UTF_8);
  }
}
