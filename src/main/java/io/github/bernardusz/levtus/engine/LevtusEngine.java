package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.exception.LevtusHttpException;
import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import io.github.bernardusz.levtus.routing.Router;
import io.github.bernardusz.levtus.security.SecurityConfig;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * The Levtus engine that handles HTTP requests. Parsing and Creating Request, instantiating Response and LevtusContext (ctx) and passing works to the router.
 *
 * @author Bernardusz
 * @version 0.1.1
 */
public class LevtusEngine {
  private final Router router;
  private final HttpParser parser;
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
    this.parser = new HttpParser();
  }

  /**
   * Starts the Levtus engine based on the provided port.
   *
   * <p>Will start on HTTP by default, unless SSL is configured via {@link #ssl(String, String)}.</p>
   *
   * @param port the port
   */
  public void start(int port) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        ServerSocket server = securityConfig.getServerSocket(port)) {
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
   * Configures the Levtus engine to use SSL by providing the keystore path and password.
   *
   * @param keystorePath the keystore path
   * @param keystorePass the keystore pass
   */
  public void ssl(String keystorePath, String keystorePass) {
    this.securityConfig = new SecurityConfig(keystorePath, keystorePass);
  }

  /**
   * Handles the connection from a client.
   *
   * <p>Responsible for:</p>
   * <ul>
   * <li>Reading from the client socket</li>
   * <li>Parsing incoming Request</li>
   * <li>Instantiating Response and LevtusContext</li>
   * <li>Passing the work to the router</li>
   * <li>Sending the response</li>
   * <li>Closing the client socket</li>
   * </ul>
   *
   * @param client the client socket
   */
  private void handleConnection(Socket client) {
    try (client;
        BufferedInputStream inputStream = new BufferedInputStream(client.getInputStream());
        BufferedOutputStream outputStream = new BufferedOutputStream(client.getOutputStream())) {
      client.setSoTimeout(5000);
      Response res = new Response(outputStream, staticFilesPath);

      try {
        Request req;
        while ((req = parser.parseRequest(this, inputStream)) != null) {
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

  /**
   * Get max concurrent connections for the server.
   *
   * @return the max concurrent connections
   */
  int getMaxConcurrentConnections(){
    return maxConcurrentConnections;
  }

  /**
   * Sets max concurrent connections for the server.
   *
   * @param maxConcurrentConnections the max concurrent connections
   */
  public void setMaxConcurrentConnections(int maxConcurrentConnections) {
    this.maxConcurrentConnections = maxConcurrentConnections;
  }

  /**
   * Get the max empty lines in the request for Keep-Alive connection.
   *
   * @return return the max empty lines in a request
   */
  int getMaxEmptyLines(){
    return maxEmptyLines;
  }

  /**
   * Sets max empty lines in the request for Keep-Alive connection.
   *
   * @param maxEmptyLines the max empty lines
   */
  public void setMaxEmptyLines(int maxEmptyLines) {
    this.maxEmptyLines = maxEmptyLines;
  }

  /**
   * Get the global max body size for all incoming requests.
   *
   * @return return the max body size in a request
   */
  int getMaxBodySize(){
    return maxBodySize;
  }

  /**
   * Sets the global max body size for all incoming requests.
   *
   * @param maxBodySize the max body size
   */
  public void setMaxBodySize(int maxBodySize) {
    this.maxBodySize = maxBodySize;
  }

  /**
   * Get the global max header count for all incoming requests.
   *
   * @return return the max header count in a request
   */
  int getMaxHeaderCount(){
    return maxHeaderCount;
  }

  /**
   * Sets the global max header count for all incoming requests.
   *
   * @param maxHeaderCount the max header count
   */
  public void setMaxHeaderCount(int maxHeaderCount) {
    this.maxHeaderCount = maxHeaderCount;
  }

  /**
   * Get the global max size of a line for all incoming requests.
   *
   * @return return the max size per line of a request
   */
  int getMaxLineSize(){
    return maxLineSize;
  }

  /**
   * Sets the global max size of a line for all incoming requests.
   *
   * @param maxLineSize the max line size
   */
  public void setMaxLineSize(int maxLineSize) {
    this.maxLineSize = maxLineSize;
  }

  /**
   * Get the max header size for all incoming requests.
   *
   * @return return the max header size for all incoming requests
   */
  int getMaxHeaderSize(){
    return maxHeaderSize;
  }

  /**
   * Sets the max header size for all incoming requests.
   *
   * @param maxHeaderSize the max header size
   */
  public void setMaxHeaderSize(int maxHeaderSize) {
    this.maxHeaderSize = maxHeaderSize;
  }

  /**
   * Sets the path/directory in which all static files are set.
   *
   * <p>Default to {@link #staticFilesPath}</p>
   *
   * @param staticFilesPath the static files path
   */
  public void setStaticFiles(String staticFilesPath) {
    this.staticFilesPath = staticFilesPath;
  }

  /**
   * The helper method to send a 503 Service Unavailable response.
   *
   * <p>When the server is overloaded by request (Semaphore is full) this helper method will be called</p>
   * <p>It will send 503 immediately on the Main Thread to sever connection immediately</p>
   *
   * @param client The socket client
   */
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
}
