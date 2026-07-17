package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.routing.Router;
import io.github.bernardusz.levtus.security.SecurityConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * The Levtus engine that handles HTTP requests. Parsing and Creating Request, instantiating
 * Response and LevtusContext (ctx) and passing works to the router.
 *
 * @author Bernardusz
 * @version 0.1.1
 */
public class LevtusEngine {
  final HttpParser parser;
  final HttpConnectionHandler handler;
  volatile SecurityConfig securityConfig;
  int maxConcurrentConnections = 10000;

  /**
   * Instantiates a new Levtus engine.
   *
   * @param router the router
   */
  public LevtusEngine(Router router) {
    this.securityConfig = new SecurityConfig(null, null);
    this.parser = new HttpParser();
    this.handler = new HttpConnectionHandler(router, parser);
  }

  /**
   * Starts the Levtus engine based on the provided port.
   *
   * <p>Will start on HTTP by default, unless SSL is configured via {@link #ssl(String, String)}.
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
                  handler.handle(client);
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
   * Get max concurrent connections for the server.
   *
   * @return the max concurrent connections
   */
  int getMaxConcurrentConnections() {
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
  int getMaxEmptyLines() {
    return handler.getMaxEmptyLines();
  }

  /**
   * Sets max empty lines in the request for Keep-Alive connection.
   *
   * @param maxEmptyLines the max empty lines
   */
  public void setMaxEmptyLines(int maxEmptyLines) {
    handler.setMaxEmptyLines(maxEmptyLines);
  }

  /**
   * Get the global max body size for all incoming requests.
   *
   * @return return the max body size in a request
   */
  long getMaxBodySize() {
    return handler.getMaxBodySize();
  }

  /**
   * Sets the global max body size for all incoming HTTP requests.
   *
   * @param maxBodySize the max body size
   */
  public void setMaxBodySize(long maxBodySize) {
    handler.setMaxBodySize(maxBodySize);
  }

  /**
   * Get the global max header count for all incoming HTTP requests.
   *
   * @return return the max header count in a request
   */
  int getMaxHeaderCount() {
    return handler.getMaxHeaderCount();
  }

  /**
   * Sets the global max header count for all incoming HTTP requests.
   *
   * @param maxHeaderCount the max header count
   */
  public void setMaxHeaderCount(int maxHeaderCount) {
    handler.setMaxHeaderCount(maxHeaderCount);
  }

  /**
   * Get the global max size of a line for all incoming requests.
   *
   * @return return the max size per line of an HTTP request
   */
  int getMaxLineSize() {
    return handler.getMaxLineSize();
  }

  /**
   * Sets the global max size of a line for all incoming requests.
   *
   * @param maxLineSize the max line size
   */
  public void setMaxLineSize(int maxLineSize) {
    handler.setMaxLineSize(maxLineSize);
  }

  /**
   * Get the max header size for all incoming HTTP requests.
   *
   * @return return the max header size for all incoming requests
   */
  int getMaxHeaderSize() {
    return handler.getMaxHeaderSize();
  }

  /**
   * Sets the max header size for all incoming HTTP requests.
   *
   * @param maxHeaderSize the max header size
   */
  public void setMaxHeaderSize(int maxHeaderSize) {
    handler.setMaxHeaderSize(maxHeaderSize);
  }

  /**
   * Sets the path/directory in which all static files are set.
   *
   * <p>Default to "./public" in {@link HttpConnectionHandler}
   *
   * @param staticFilesPath the static files path
   */
  public void setStaticFiles(String staticFilesPath) {
    handler.setStaticFiles(staticFilesPath);
  }

  /**
   * Set the initial socket timeout for all incoming HTTP requests
   *
   * @param initialSocketTimeout the initial socket timeout
   */
  public void setInitialSocketTimeout(int initialSocketTimeout) {
    handler.setInitialSocketTimeout(initialSocketTimeout);
  }

  /**
   * Set the processing socket timeout for all incoming HTTP requests
   *
   * @param processingSocketTimeout the processing socket timeout
   */
  public void setProcessingSocketTimeout(int processingSocketTimeout) {
    handler.setProcessingSocketTimeout(processingSocketTimeout);
  }

  /**
   * The helper method to send a 503 Service Unavailable response.
   *
   * <p>When the server is overloaded by request (Semaphore is full) this helper method will be
   * called
   *
   * <p>It will send 503 immediately on the Main Thread to sever connection immediately Visible for
   * testing
   *
   * @param client The socket client
   */
  void sendOverloadedResponse(Socket client) {
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
