package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.exception.http.BadRequestException;
import io.github.bernardusz.levtus.exception.http.HeaderTooLargeException;
import io.github.bernardusz.levtus.exception.http.LevtusNotImplementedException;
import io.github.bernardusz.levtus.exception.http.PayloadTooLargeException;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import io.github.bernardusz.levtus.routing.Node;
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
   * <p>Immediately throws an Exception if:
   *
   * <ul>
   *   <li>Request line is invalid
   *   <li>Headers are invalid
   *   <li>Body is invalid
   * </ul>
   *
   * <p>Without waiting for all of them; one problem, Exception is thrown
   *
   * @param handler The handler that uses HttpParser for parsing
   * @param inputStream The stream to read the HTTP request from
   * @param response The response object to be used for sending responses for accepting 100-Continue
   *     request
   * @return {@link Request} A fully parsed Request object, or null if the stream is empty
   * @throws IOException If a network or stream error occurs
   * @throws BadRequestException If the request line is invalid or headers are malformed
   * @throws PayloadTooLargeException If the request's body size exceeds {@link
   *     LevtusEngine#getMaxBodySize()} or {@link HttpConnectionHandler#getMaxBodySize()}, set via
   *     {@link LevtusEngine#setMaxBodySize(long)} or {@link
   *     HttpConnectionHandler#setMaxBodySize(long)}
   * @throws HeaderTooLargeException If the header's total size exceeds {@link
   *     LevtusEngine#getMaxHeaderSize()} or {@link HttpConnectionHandler#getMaxHeaderSize()}, set
   *     via {@link LevtusEngine#setMaxHeaderSize(int)} or {@link
   *     HttpConnectionHandler#setMaxHeaderSize(int)}
   * @throws LevtusNotImplementedException If the method is not implemented.
   */
  Request parseRequest(HttpConnectionHandler handler, InputStream inputStream, Response response)
      throws IOException,
          BadRequestException,
          PayloadTooLargeException,
          HeaderTooLargeException,
          LevtusNotImplementedException {
    String requestLine =
        parseRequestLine(inputStream, handler.getMaxLineSize(), handler.getMaxEmptyLines());

    if (requestLine == null) {
      return null;
    }

    String method = parseMethod(requestLine);
    HttpProtocol protocol = parseHttpProtocol(requestLine);

    // Parse the Header
    Map<String, List<String>> headers =
        parseHeaders(inputStream, handler.getMaxHeaderSize(), handler.getMaxHeaderCount());

    // Parse the Path
    String rawPath = parseRawPath(requestLine);

    URI uri;
    try {
      uri = new URI(rawPath);
    } catch (URISyntaxException e) {
      throw new BadRequestException("400 - Bad Request (Invalid URI)");
    }
    String path = uri.getRawPath();

    // %2F is decoded and treated as a path separator.
    // This is intentional for levtus-core: microservice routing
    // has no filesystem traversal risk. levtus-http/ will enforce
    // strict RFC 3986 segment isolation.
    String decodedPath = decodePath(path);

    Map<String, List<String>> queryParams = parseQueryParams(uri.getRawQuery());
    String normalizedPath = normalizePath(decodedPath);

    // Parse the Body
    long limit = handler.getMaxBodySize();
    Node matchedNode = handler.router.matchRoute(method, normalizedPath);
    if (matchedNode != null && matchedNode.getMaxBodySize() != -1) {
      limit = matchedNode.getMaxBodySize();
    }
    validateBodySize(headers, limit, response);

    return new Request(
        method,
        normalizedPath,
        headers,
        queryParams,
        inputStream,
        handler.getMaxBodySize(),
        handler.getMaxChunkSize(),
        handler.getMaxChunkCount(),
        protocol);
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
    if (parts.length != 3) {
      throw new BadRequestException("400 - Bad Request (Invalid request line)");
    }

    if (!parts[2].matches("HTTP/1\\.[01]")) {
      if (parts[2].matches("HTTP/[0-9]+\\.[0-9]+")) {
        throw new LevtusNotImplementedException("505 - Unsupported HTTP version");
      } else {
        throw new BadRequestException("400 - Bad Request (Invalid HTTP version)");
      }
    }

    return parts[0];
  }

  /**
   * The helper method for parsing the protocol of an HTTP request.
   *
   * @param requestLine the parsed request line
   * @return the HttpProtocol in the form of an enum {@link HttpProtocol}
   * @throws BadRequestException if the request line is invalid or the HTTP version is not supported
   * @throws LevtusNotImplementedException if the HTTP version is not supported
   */
  HttpProtocol parseHttpProtocol(String requestLine)
      throws BadRequestException, LevtusNotImplementedException {
    String[] parts = requestLine.split(" ", 3);
    if (parts.length != 3) {
      throw new BadRequestException("400 - Bad Request (Invalid request line)");
    }

    if (!parts[2].matches("HTTP/1\\.[01]")) {
      if (parts[2].matches("HTTP/[0-9]+\\.[0-9]+")) {
        throw new LevtusNotImplementedException("505 - Unsupported HTTP version");
      } else {
        throw new BadRequestException("400 - Bad Request (Invalid HTTP version)");
      }
    }

    return parts[2].matches("HTTP/1.1") ? HttpProtocol.HTTP_1_1 : HttpProtocol.HTTP_1_0;
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
      InputStream inputStream, int maxHeaderSize, int maxHeaderCount)
      throws IOException,
          HeaderTooLargeException,
          BadRequestException,
          LevtusNotImplementedException {
    String header;
    int totalHeaderSize = 0;
    int headerCount = 0;
    Map<String, List<String>> headers = new HashMap<>();
    while ((header = readLine(inputStream, maxHeaderSize)) != null && !(header.isEmpty())) {
      totalHeaderSize += header.length();
      headerCount++;
      if (totalHeaderSize > maxHeaderSize) {
        throw new HeaderTooLargeException("Header too large");
      }
      if (headerCount > maxHeaderCount) {
        throw new HeaderTooLargeException("Too many headers");
      }

      String[] headerParts = header.split(":", 2);
      if (headerParts.length == 2) {
        headers
            .computeIfAbsent(headerParts[0].toLowerCase().trim(), _ -> new ArrayList<>())
            .add(headerParts[1].trim());
      } else {
        throw new BadRequestException("400 - Bad Request (Invalid header)");
      }
    }
    if (headers.get("host") == null) {
      throw new BadRequestException("400 - Bad Request (Missing host header)");
    }
    if (headers.get("host").size() > 1) {
      throw new BadRequestException("400 - Bad Request (Duplicate host header)");
    }

    return headers;
  }

  /**
   * The helper method to validate the body size of an HTTP request.
   *
   * <p>This helper method throws a PayloadTooLargeException if the body size exceeds the maximum
   * allowed size {@link LevtusEngine#getMaxBodySize()} or {@link
   * HttpConnectionHandler#getMaxBodySize()}.
   *
   * <p>This helper method throws a BadRequestException if the body size is invalid or exceeds the
   * maximum allowed size.
   *
   * @param headers the headers of an HTTP request
   * @param maxBodySize the maximum body size in an HTTP request
   * @param response the {@link Response} object used for accepting 100-Continue request
   * @throws PayloadTooLargeException if the body size exceeds the maximum allowed size {@link
   *     LevtusEngine#getMaxBodySize()} or {@link HttpConnectionHandler#getMaxBodySize()}.
   * @throws BadRequestException if more than one Content-Length header is present
   */
  void validateBodySize(Map<String, List<String>> headers, long maxBodySize, Response response)
      throws PayloadTooLargeException, BadRequestException {
    List<String> chunkedHeaders = headers.get("transfer-encoding");

    boolean isChunked =
        chunkedHeaders != null
            && !chunkedHeaders.isEmpty()
            && chunkedHeaders.stream().anyMatch("chunked"::equalsIgnoreCase);

    if (isChunked) {
      headers.remove("content-length");
    } else {
      int contentLength;
      List<String> lengthStrList =
          headers.getOrDefault("content-length", new ArrayList<>(List.of("0")));

      if (lengthStrList != null) {
        if (lengthStrList.isEmpty()) {
          throw new BadRequestException("400 - Bad Request (Missing content-length header)");
        }

        if (lengthStrList.size() > 1) {
          throw new BadRequestException("400 - Bad Request (Multiple content-length headers)");
        }
        String lengthStr = lengthStrList.getFirst();
        try {
          contentLength = Integer.parseInt(lengthStr);
          if (contentLength > maxBodySize) {
            throw new PayloadTooLargeException(
                "Payload Too Large: " + contentLength + " exceeds limit of " + maxBodySize);
          }

        } catch (NumberFormatException e) {
          throw new BadRequestException("400 - Bad Request (Content Length is invalid)");
        }
      }
    }
    List<String> expectHeaders = headers.get("expect");

    if (expectHeaders != null
        && !expectHeaders.isEmpty()
        && expectHeaders.getFirst().equalsIgnoreCase("100-continue")) {
      response.status(100).send();
    }
  }

  /**
   * Decodes the path for incoming HTTP requests.
   *
   * <p>Decode the path before normalization, so we can catch invalid/malicious paths early.
   *
   * @param path the path to be checked
   * @return the decoded path
   * @throws BadRequestException if the path is invalid
   */
  String decodePath(String path) throws BadRequestException {
    String decodedPath;
    try {
      decodedPath = utf8Decoder(path);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("400 - Invalid encoding");
    }

    // Security Fix: Reject null byte injection
    if (decodedPath.contains("\0")) {
      throw new BadRequestException("400 - Bad Request (Null byte detected)");
    }
    return decodedPath;
  }

  /**
   * The helper method to parse the raw path into a valid path of an HTTP request.
   *
   * <p>The raw path is the path of an HTTP request that is received from a client.
   *
   * <p>The parsed path is the path of an HTTP request that is used to match a route.
   *
   * <p>The path returned hasn't been split between the path and the query parameters.
   *
   * @param requestLine the request line in an HTTP request
   * @return the parsed "valid" path of an HTTP request
   */
  String parseRawPath(String requestLine) {
    String rawPath = requestLine.split(" ", 3)[1];

    // Strip https:// or http://
    if (rawPath.startsWith("http://") || rawPath.startsWith("https://")) {
      rawPath = rawPath.substring(rawPath.indexOf("//") + 2); // Strip until the https/http
    }

    // Strip domain name
    if (!rawPath.startsWith("/")) {
      int firstSlash = rawPath.indexOf("/");
      if (firstSlash != -1) {
        rawPath = rawPath.substring(firstSlash);
      } else {
        rawPath = "/";
      }
    }

    return rawPath;
  }

  /**
   * The helper method to parse query params from the "valid" path.
   *
   * <p>May also return an empty map if there are no query params.
   *
   * @param rawQuery the raw query String
   * @return the query params
   */
  Map<String, List<String>> parseQueryParams(String rawQuery) throws BadRequestException {
    Map<String, List<String>> queryParams = new HashMap<>();
    if (rawQuery == null || rawQuery.isEmpty()) {
      return queryParams;
    }

    for (String query : rawQuery.split("&")) {
      String[] pair = query.split("=", 2);
      try {
        if (pair.length == 2) {

          String key = utf8Decoder(pair[0]);
          String value = utf8Decoder(pair[1]);

          if (key.contains("\0") || value.contains("\0")) {
            throw new BadRequestException("400 - Bad Request (Null byte in query)");
          }
          queryParams.computeIfAbsent(key, _ -> new ArrayList<>()).add(value);

        } else if (!pair[0].isEmpty()) {
          String key = utf8Decoder(pair[0]);
          if (key.contains("\0")) {
            throw new BadRequestException("400 - Bad Request (Null byte in query)");
          }
          queryParams.computeIfAbsent(key, _ -> new ArrayList<>()).add("");
        }
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("400 - Bad Request (Invalid encoding)");
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
    if (path == null || path.isEmpty()) {
      return "/";
    }

    String normalized = path.replace('\\', '/');
    normalized = normalized.replaceAll("/{2,}", "/");

    String[] segments = normalized.split("/");
    List<String> stack = new ArrayList<>();

    for (String segment : segments) {
      if (segment.isEmpty() || segment.equals(".")) {
        continue;
      }
      if (segment.equals("..")) {
        if (stack.isEmpty()) {
          throw new BadRequestException("400 - Bad Request");
        }
        stack.removeLast();
      } else {
        stack.add(segment);
      }
    }

    StringBuilder builder = new StringBuilder();
    if (stack.isEmpty()) {
      builder.append("/");
    } else {
      for (String segment : stack) {
        builder.append("/").append(segment);
      }
    }

    if (normalized.endsWith("/") && builder.length() > 1) {
      builder.append("/");
    }

    return builder.toString();
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
   * The helper method to decode a string from UTF-8.
   *
   * @param body the String that needs to be decoded
   * @return a decoded String
   * @throws IllegalArgumentException if the string is not a valid UTF-8 string
   */
  String utf8Decoder(String body) throws IllegalArgumentException {
    return URLDecoder.decode(body, StandardCharsets.UTF_8);
  }
}
