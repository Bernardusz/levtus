package io.github.bernardusz.levtus;

import io.github.bernardusz.levtus.engine.LevtusEngine;
import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.routing.Middleware;
import io.github.bernardusz.levtus.routing.Router;
import java.util.function.Consumer;

/**
 *
 *
 * <h2>Levtus - Levis Conatus</h2>
 *
 * The main entry point for the Levtus HTTP engine.
 *
 * <p>Levtus is a high-performance, zero-dependency HTTP/1.1 server designed to leverage Java
 * Virtual Threads for efficient concurrency. It provides a fluent API for defining routes,
 * middleware, and server configurations.
 */
public class Levtus {
  private final Router router = new Router();
  private final LevtusEngine engine;

  private Levtus() {
    this.engine = new LevtusEngine(router);
  }

  /**
   * Creates a new instance of the Levtus application.
   *
   * @return a new Levtus instance
   */
  public static Levtus create() {
    return new Levtus();
  }

  /**
   * Registers a handler for GET requests at the specified path.
   *
   * @param path the URL path to match (e.g., "/hello")
   * @param handler the logic to execute when the route is matched
   */
  public void get(String path, Consumer<LevtusContext> handler) {
    router.get(path, handler);
  }

  /**
   * Registers a handler for POST requests at the specified path.
   *
   * @param path the URL path to match
   * @param handler the logic to execute when the route is matched
   */
  public void post(String path, Consumer<LevtusContext> handler) {
    router.post(path, handler);
  }

  /**
   * Registers a handler for PUT requests at the specified path.
   *
   * @param path the URL path to match
   * @param handler the logic to execute when the route is matched
   */
  public void put(String path, Consumer<LevtusContext> handler) {
    router.put(path, handler);
  }

  /**
   * Registers a handler for DELETE requests at the specified path.
   *
   * @param path the URL path to match
   * @param handler the logic to execute when the route is matched
   */
  public void delete(String path, Consumer<LevtusContext> handler) {
    router.delete(path, handler);
  }

  /**
   * Adds a global middleware to the request processing chain.
   *
   * @param middleware the middleware logic to execute for every request
   */
  public void use(Middleware middleware) {
    router.use(middleware);
  }

  /**
   * Configures SSL/TLS for the server using a PKCS12 keystore.
   *
   * @param keystorePath the path to the .p12 or .jks keystore file
   * @param keystorePass the password for the keystore
   */
  public void ssl(String keystorePath, String keystorePass) {
    engine.ssl(keystorePath, keystorePass);
  }

  /**
   * Sets the maximum number of concurrent connections the server will accept. This acts as a global
   * throttle to protect system resources.
   *
   * @param maxConcurrentConnections the maximum number of active connections
   */
  public void setMaxConcurrentConnections(int maxConcurrentConnections) {
    engine.setMaxConcurrentConnections(maxConcurrentConnections);
  }

  /**
   * Sets the maximum number of empty lines allowed between requests in a persistent connection.
   *
   * @param maxEmptyLines the maximum number of allowed empty lines
   */
  public void setMaxEmptyLines(int maxEmptyLines) {
    engine.setMaxEmptyLines(maxEmptyLines);
  }

  /**
   * Sets the maximum allowed size for the request body in bytes.
   *
   * @param maxBodySize the maximum body size in bytes
   */
  public void setMaxBodySize(int maxBodySize) {
    engine.setMaxBodySize(maxBodySize);
  }

  /**
   * Sets the maximum number of headers allowed in a single request.
   *
   * @param maxHeaderCount the maximum number of headers
   */
  public void setMaxHeaderCount(int maxHeaderCount) {
    engine.setMaxHeaderCount(maxHeaderCount);
  }

  /**
   * Sets the maximum length of a single request line (e.g., the URL line).
   *
   * @param maxLineSize the maximum line size in characters
   */
  public void setMaxLineSize(int maxLineSize) {
    engine.setMaxLineSize(maxLineSize);
  }

  /**
   * Sets the maximum cumulative size of all headers in a request.
   *
   * @param maxHeaderSize the maximum header size in bytes
   */
  public void setMaxHeaderSize(int maxHeaderSize) {
    engine.setMaxHeaderSize(maxHeaderSize);
  }

  /**
   * Configures the directory from which static files (HTML, CSS, JS) will be served.
   *
   * @param staticFilesPath the directory path containing static assets
   */
  public void staticFiles(String staticFilesPath) {
    engine.setStaticFiles(staticFilesPath);
  }

  /**
   * Starts the server and begins listening for connections on the specified port.
   *
   * @param port the TCP port to listen on (e.g., 8080)
   */
  public void listen(int port) {
    engine.start(port);
  }
}
