package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.exception.*;
import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import io.github.bernardusz.levtus.routing.Router;
import io.github.bernardusz.levtus.security.SecurityConfig;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * The type Levtus engine.
 */
public class LevtusEngine {
  private final Router router;
  private volatile SecurityConfig securityConfig;
  private int maxConcurrentConnections = 10000;
  private int maxEmptyLines = 10;
  private int maxBodySize = 10 * 1024 * 1024;
  private int maxHeaderCount = 100;
  private int maxLineSize = 8192; // 8 KB Limit
  private int maxHeaderSize = 8192; // 8 KB Limit
  private String staticFilesPath = "./public";

  /**
   * Instantiates a new Levtus engine.
   *
   * @param router the router
   */
public LevtusEngine(Router router) {
    this.router = router;
    this.securityConfig = new SecurityConfig(null, null);
  }

  /**
   * Start.
   *
   * @param port the port
   */
public void start(int port) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        ServerSocket server = securityConfig.getServerSocketFactory(port)) {
      System.out.println(
          "🚀 Levtus Engine started on port "
              + port
              + (securityConfig.isEnabled() ? " (HTTPS)" : " (HTTP)"));
      Semaphore semaphore = new Semaphore(maxConcurrentConnections);
      while (!Thread.currentThread().isInterrupted()) {
        Socket client = server.accept();
        if (semaphore.tryAcquire()) {
          executor.submit(
              () -> {
                try {
                  handleConnection(client);
                } finally {
                  semaphore.release();
                }
              });
        } else {
          sendOverloadedResponse(client);
        }
      }
    } catch (IOException e) {
      System.err.println("❌ Engine failed: " + e.getMessage());
    }
  }

  /**
   * Ssl.
   *
   * @param keystorePath the keystore path
   * @param keystorePass the keystore pass
   */
public void ssl(String keystorePath, String keystorePass) {
    this.securityConfig = new SecurityConfig(keystorePath, keystorePass);
  }

  private void handleConnection(Socket client) {
    try (client;
        BufferedInputStream inputStream = new BufferedInputStream(client.getInputStream());
        BufferedOutputStream outputStream = new BufferedOutputStream(client.getOutputStream())) {
      client.setSoTimeout(5000);
      Response res = new Response(outputStream, staticFilesPath);

      try {
        Request req;
        while ((req = parseRequest(inputStream)) != null) {
          res = new Response(outputStream, staticFilesPath);
          LevtusContext ctx = new LevtusContext(req, res);
          client.setSoTimeout(20000);
          router.handle(ctx);
          if (!res.isSent()) {
            res.status(404).send("404 - Not Found");
          }
          try {
            if (!req.isCached()) {
              inputStream.skipNBytes(req.contentLength() - req.bytesRead());
            }
          } catch (EOFException e) {
            break;
          }
        }
      } catch (IllegalArgumentException e) {
        res.status(400).send("400 - Bad Request (Malformed URL)");
      } catch (LevtusHttpException e) {
        res.status(e.getStatusCode()).send(e.getMessage());
      } catch (SocketTimeoutException e) {
        res.status(408).send("408 - Request Timeout");
      } catch (Exception e) {
        res.status(500).send("500 - Internal Server Error");
        throw e;
      }

    } catch (Exception e) {
      System.err.println("Connection failed: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private Request parseRequest(InputStream inputStream) throws IOException, URISyntaxException {
    String requestLine;

    // Read the request line
    int b = 0;
    while ((requestLine = readLine(inputStream)) != null && requestLine.isEmpty()) {
      b++;
      if (b > maxEmptyLines) {
        throw new BadRequestException("400 - Bad Request (Request too large)");
      }
    }

    if (requestLine == null) return null;

    String[] parts = requestLine.split(" ", 3);
    if (parts.length < 2) {
      throw new BadRequestException("400 - Bad Request");
    }

    String method = parts[0];

    // Parse the Header
    String header;
    int totalHeaderSize = 0;
    Map<String, List<String>> headers = new HashMap<>();
    while ((header = readLine(inputStream)) != null && !(header.isEmpty())) {
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
            .computeIfAbsent(headerParts[0].toLowerCase().trim(), k -> new ArrayList<>())
            .add(headerParts[1].trim());
      }
    }
    if (headers.get("host") == null) {
      throw new BadRequestException("400 - Bad Request (Missing host header)");
    }
    if (headers.get("transfer-encoding") != null) {
      throw new LevtusNotImplementedException(headers.get("transfer-encoding").getFirst());
    }

    // Parse the Body
    int contentLength;
    List<String> lengthStrList =
        headers.getOrDefault("content-length", new ArrayList<>(List.of("0")));
    if (lengthStrList.size() > 1) {
      throw new BadRequestException("400 - Bad Request (Multiple content-length headers)");
    }
    String lengthStr = lengthStrList.getFirst();
    if (!lengthStr.isEmpty()) {
      try {
        contentLength = Integer.parseInt(lengthStr);
        if (contentLength > maxBodySize) {
          throw new PayloadTooLargeException(
              "Payload Too Large: " + contentLength + " exceeds limit of " + maxBodySize);
        }
      } catch (NumberFormatException _) {
      }
    }

    // Parse the Path
    String rawPath = parts[1];
    String path;

    if (rawPath.startsWith("http")) {
      rawPath = rawPath.substring(rawPath.indexOf("//") + 2); // Strip until the https/http
      rawPath =
          rawPath.substring(
              !rawPath.contains("/")
                  ? 0
                  : rawPath.indexOf("/")); // with http:// or https://, it includes the full domain.
      // So the first / is the domain name
      if (rawPath.equals("/") || rawPath.isEmpty()) {
        rawPath = "/";
      }
    }
    if (!rawPath.contains("/")) {
      rawPath = "/";
    }

    Map<String, String> queryParams = new HashMap<>();
    if (rawPath.contains("?")) {
      int queryStart = rawPath.indexOf("?");
      path = rawPath.substring(0, queryStart);
      String queryString = rawPath.substring(queryStart + 1);

      for (String query : queryString.split("&")) {
        String[] pair = query.split("=", 2);
        if (pair.length == 2) {
          queryParams.put(utf8Decoder(pair[0]), utf8Decoder(pair[1]));
        } else if (pair.length >= 1 && !pair[0].isEmpty()) {
          queryParams.put(utf8Decoder(pair[0]), "");
        }
      }
    } else {
      path = rawPath;
    }

    URI uri;
    String newPath;
    try {
      uri = new URI(path).normalize();
      // Now after http is gone, we need to fix every instance of double and trailing slashes
      newPath = (uri.toString()).replaceAll("/{2,}", "/");
    } catch (URISyntaxException e) {
      throw new BadRequestException("400 - Bad Request");
    }
    return new Request(
        method, new URI(newPath).getRawPath(), headers, queryParams, inputStream, maxBodySize);
  }

  /**
   * Sets max concurrent connections.
   *
   * @param maxConcurrentConnections the max concurrent connections
   */
public void setMaxConcurrentConnections(int maxConcurrentConnections) {
    this.maxConcurrentConnections = maxConcurrentConnections;
  }

  /**
   * Sets max empty lines.
   *
   * @param maxEmptyLines the max empty lines
   */
public void setMaxEmptyLines(int maxEmptyLines) {
    this.maxEmptyLines = maxEmptyLines;
  }

  /**
   * Sets max body size.
   *
   * @param maxBodySize the max body size
   */
public void setMaxBodySize(int maxBodySize) {
    this.maxBodySize = maxBodySize;
  }

  /**
   * Sets max header count.
   *
   * @param maxHeaderCount the max header count
   */
public void setMaxHeaderCount(int maxHeaderCount) {
    this.maxHeaderCount = maxHeaderCount;
  }

  /**
   * Sets max line size.
   *
   * @param maxLineSize the max line size
   */
public void setMaxLineSize(int maxLineSize) {
    this.maxLineSize = maxLineSize;
  }

  /**
   * Sets max header size.
   *
   * @param maxHeaderSize the max header size
   */
public void setMaxHeaderSize(int maxHeaderSize) {
    this.maxHeaderSize = maxHeaderSize;
  }

  /**
   * Sets static files.
   *
   * @param staticFilesPath the static files path
   */
public void setStaticFiles(String staticFilesPath) {
    this.staticFilesPath = staticFilesPath;
  }

  private String readLine(InputStream inputStream) throws IOException {
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

  private void sendOverloadedResponse(Socket client) {
    try (client; // This ensures the socket closes after the try block
        OutputStream out = client.getOutputStream()) {

      String response =
          """
                HTTP/1.1 503 Service Unavailable\r
                Content-Type: text/plain\r
                Connection: close\r
                \r
                Server Overloaded: Please try again later.""";

      out.write(response.getBytes(StandardCharsets.UTF_8));
      out.flush();
    } catch (IOException e) {
      // If the client already disconnected, just ignore it
    }
  }

  private String utf8Decoder(String body) throws IllegalArgumentException {
    return URLDecoder.decode(body, StandardCharsets.UTF_8);
  }
}
