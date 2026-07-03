package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.exception.http.LevtusHttpException;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import io.github.bernardusz.levtus.routing.Router;
import io.github.bernardusz.levtus.spi.ConnectionHandler;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.net.Socket;
import java.net.SocketTimeoutException;

class HttpConnectionHandler implements ConnectionHandler {
  final Router router;
  final HttpParser parser;
  private int maxEmptyLines = 10;
  private int maxBodySize = 10 * 1024 * 1024;
  private int maxHeaderCount = 100;
  private int maxLineSize = 8192; // 8 KB Limit
  private int maxHeaderSize = 8192; // 8 KB Limit
  private String staticFilesPath = "./public";

  public HttpConnectionHandler(Router router, HttpParser parser) {
    this.router = router;
    this.parser = parser;
  }

  /**
   * The main method that handles a client connection.
   *
   * @param client the client socket to process
   */
  public void handle(Socket client) {
    try (client;
        BufferedInputStream inputStream = new BufferedInputStream(client.getInputStream());
        BufferedOutputStream outputStream = new BufferedOutputStream(client.getOutputStream())) {
      client.setSoTimeout(5000);
      Response res = new Response(outputStream, staticFilesPath);

      try {
        Request req;
        while ((req = parser.parseRequest(this, inputStream, res)) != null) {
          res = new Response(outputStream, staticFilesPath);
          client.setSoTimeout(20000);
          router.handle(req, res);
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

  /**
   * Get the max empty lines in the HTTP request for Keep-Alive connection.
   *
   * @return return the max empty lines in a request
   */
  int getMaxEmptyLines() {
    return maxEmptyLines;
  }

  /**
   * Sets max empty lines in the HTTP request for Keep-Alive connection.
   *
   * @param maxEmptyLines the max empty lines
   */
  public void setMaxEmptyLines(int maxEmptyLines) {
    this.maxEmptyLines = maxEmptyLines;
  }

  /**
   * Get the global max body size for all incoming HTTP requests.
   *
   * @return return the max body size in a request
   */
  int getMaxBodySize() {
    return maxBodySize;
  }

  /**
   * Sets the global max body size for all incoming HTTP requests.
   *
   * @param maxBodySize the max body size
   */
  public void setMaxBodySize(int maxBodySize) {
    this.maxBodySize = maxBodySize;
  }

  /**
   * Get the global max header count for all incoming HTTP requests.
   *
   * @return return the max header count in a request
   */
  int getMaxHeaderCount() {
    return maxHeaderCount;
  }

  /**
   * Sets the global max header count for all incoming HTTP requests.
   *
   * @param maxHeaderCount the max header count
   */
  public void setMaxHeaderCount(int maxHeaderCount) {
    this.maxHeaderCount = maxHeaderCount;
  }

  /**
   * Get the global max size of a line for all incoming HTTP requests.
   *
   * @return return the max size per line of a request
   */
  int getMaxLineSize() {
    return maxLineSize;
  }

  /**
   * Sets the global max size of a line for all incoming HTTP requests.
   *
   * @param maxLineSize the max line size
   */
  public void setMaxLineSize(int maxLineSize) {
    this.maxLineSize = maxLineSize;
  }

  /**
   * Get the max header size for all incoming requests.
   *
   * @return return the max header size for all incoming HTTP requests
   */
  int getMaxHeaderSize() {
    return maxHeaderSize;
  }

  /**
   * Sets the max header size for all incoming HTTP requests.
   *
   * @param maxHeaderSize the max header size
   */
  public void setMaxHeaderSize(int maxHeaderSize) {
    this.maxHeaderSize = maxHeaderSize;
  }

  /**
   * Sets the path/directory in which all static files are set.
   *
   * <p>Default to {@link #staticFilesPath}
   *
   * @param staticFilesPath the static files path
   */
  public void setStaticFiles(String staticFilesPath) {
    this.staticFilesPath = staticFilesPath;
  }
}
