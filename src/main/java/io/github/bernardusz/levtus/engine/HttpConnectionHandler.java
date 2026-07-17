package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.exception.http.LevtusHttpException;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import io.github.bernardusz.levtus.routing.Router;
import io.github.bernardusz.levtus.spi.ConnectionHandler;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

class HttpConnectionHandler implements ConnectionHandler {
  final Router router;
  final HttpParser parser;
  private int maxEmptyLines = 10;
  private long maxBodySize = 10 * 1024 * 1024;
  private int maxHeaderCount = 100;
  private int maxLineSize = 8192; // 8 KB Limit
  private int maxHeaderSize = 8192; // 8 KB Limit
  private String staticFilesPath = "./public";
  private long maxChunkSize = 1024 * 1024; // 1 MB Limit
  private long maxChunkCount = 1000;

  /**
   * The constructor for HttpConnectionHandler.
   *
   * @param router the router for HTTP Handler
   * @param parser the parser for HTTP Parser
   */
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
        OutputStream outputStream = client.getOutputStream()) {
      client.setSoTimeout(5000);
      Response res = new Response(outputStream, staticFilesPath, false); // At first, it is false,
      // Because we won't continue sending responses after the error
      try {
        Request req;
        while ((req = parser.parseRequest(this, inputStream, res)) != null) {
          res = new Response(outputStream, staticFilesPath, req.isKeepAlive()); // Now when we respond, that's when the isKeepAlive is needed
          client.setSoTimeout(20000);
          router.handle(req, res);
          if (!res.isSent()){
            if (res.isChunked()){
              res.finishChunkedResponse();
            }
            else {
              res.status(404).send("404 - Not Found");
            }
          }
          try {
            if (!req.isCached()){
              if (req.isChunked()){
                req.body();
              }
              else {
                inputStream.skipNBytes(req.contentLength() - req.bytesRead());
              }
            }
          } catch (EOFException e) {
            break;
          }

          if (!req.isKeepAlive()) {
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
      } finally {
        res.finishChunkedResponse();
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
  long getMaxBodySize() {
    return maxBodySize;
  }

  /**
   * Sets the global max body size for all incoming HTTP requests.
   *
   * @param maxBodySize the max body size
   */
  public void setMaxBodySize(long maxBodySize) {
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

  long getMaxChunkSize() {
    return maxChunkSize;
  }

  public void setMaxChunkSize(int maxChunkSize) {
    this.maxChunkSize = maxChunkSize;
  }

  long getMaxChunkCount() {
    return maxChunkCount;
  }

  public void setMaxChunkCount(long maxChunkCount) {
    this.maxChunkCount = maxChunkCount;
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
