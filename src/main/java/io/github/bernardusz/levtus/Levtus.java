package io.github.bernardusz.levtus;

import io.github.bernardusz.levtus.engine.LevtusEngine;
import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.routing.Middleware;
import io.github.bernardusz.levtus.routing.Router;
import java.util.function.Consumer;

/**
 * <h2>Levtus - Levis Conatus</h2>
 *
 * The main entry point for the Levtus HTTP engine.
 *
 * <p>Levtus is a high-performance, zero-dependency HTTP/1.1 server designed to leverage Java
 * Virtual Threads for efficient concurrency. It provides a fluent API for defining routes,
 * middleware, and server configurations. * @apiNote Use {@link #create()} to initialize the app,
 * configure routes, and call {@link #listen(int)} to boot the server.
 */
public class Levtus {
  private final Router router = new Router();
  private final LevtusEngine engine;

  private Levtus() {
    this.engine = new LevtusEngine(router);
  }

  /**
   * Initializes a new Levtus application instance with a fresh routing engine.
   *
   * <p>Usage: {@code Levtus app = Levtus.create();}
   *
   * @return a non-null Levtus instance ready for configuration
   */
  public static Levtus create() {
    return new Levtus();
  }

  /**
   * Maps an HTTP GET request to a specific handler function.
   *
   * <p>{@code app.get("/hello", ctx -> ctx.res().send("World!"));}
   *
   * @param path the exact URL path to match (must not be null, e.g., "/api/users")
   * @param handler the functional callback executed upon a successful match (must not be null)
   */
  public void get(String path, Consumer<LevtusContext> handler) {
    router.get(path, handler);
  }

  /**
   * Maps an HTTP POST request to a specific handler function.
   *
   * <p>{@code app.post("/submit", ctx -> System.out.println(ctx.req().body()));}
   *
   * @param path the exact URL path to match (must not be null)
   * @param handler the functional callback executed upon a successful match (must not be null)
   */
  public void post(String path, Consumer<LevtusContext> handler) {
    router.post(path, handler);
  }

  /**
   * Maps an HTTP PUT request to a specific handler function, typically used for updating resources.
   *
   * @param path the exact URL path to match (must not be null)
   * @param handler the functional callback executed upon a successful match (must not be null)
   */
  public void put(String path, Consumer<LevtusContext> handler) {
    router.put(path, handler);
  }

  /**
   * Maps an HTTP DELETE request to a specific handler function.
   *
   * @param path the exact URL path to match (must not be null)
   * @param handler the functional callback executed upon a successful match (must not be null)
   */
  public void delete(String path, Consumer<LevtusContext> handler) {
    router.delete(path, handler);
  }

  /**
   * Injects a global middleware into the request processing pipeline. Middleware runs sequentially
   * for every incoming request before hitting the final route handler.
   *
   * <p>{@code app.use((req, res, next) -> { System.out.println("Incoming!"); next.run(); });}
   *
   * @param middleware the interceptor logic (must not be null)
   * @return The current Levtus instance for method chaining
   */
  public Levtus use(Middleware middleware) {
    router.use(middleware);
    return this;
  }

  /**
   * Secures the server by enabling SSL/TLS using a provided keystore. Must be called before {@link
   * #listen(int)}.
   *
   * @param keystorePath the file system path to the .p12 or .jks keystore file (must not be null)
   * @param keystorePass the plain-text password to unlock the keystore (must not be null)
   * @return The current Levtus instance for method chaining
   */
  public Levtus ssl(String keystorePath, String keystorePass) {
    engine.ssl(keystorePath, keystorePass);
    return this;
  }

  /**
   * Restricts the maximum number of concurrent client connections. Acts as a global throttle to
   * prevent thread starvation and protect system resources.
   *
   * @param maxConcurrentConnections the absolute upper limit for active connections
   * @return The current Levtus instance for method chaining
   */
  public Levtus maxConcurrentConnections(int maxConcurrentConnections) {
    engine.setMaxConcurrentConnections(maxConcurrentConnections);
    return this;
  }

  /**
   * Defines the maximum number of empty lines tolerated between requests on a Keep-Alive
   * connection. Prevents idle connection abuse.
   *
   * @param maxEmptyLines the maximum allowed consecutive empty lines
   * @return The current Levtus instance for method chaining
   */
  public Levtus maxEmptyLines(int maxEmptyLines) {
    engine.setMaxEmptyLines(maxEmptyLines);
    return this;
  }

  /**
   * Imposes a hard limit on the HTTP request body size to prevent memory exhaustion (OOM) attacks.
   * If a client sends a payload exceeding this limit, the connection is rejected.
   *
   * @param maxBodySize the absolute byte limit for the payload (e.g., 1048576 for 1MB)
   * @return The current Levtus instance for method chaining
   */
  public Levtus maxBodySize(int maxBodySize) {
    engine.setMaxBodySize(maxBodySize);
    return this;
  }

  /**
   * Restricts the maximum number of HTTP headers a client can send in a single request. Protects
   * against header-smuggling or buffer overflow attacks.
   *
   * @param maxHeaderCount the upper limit for incoming headers
   * @return The current Levtus instance for method chaining
   */
  public Levtus maxHeaderCount(int maxHeaderCount) {
    engine.setMaxHeaderCount(maxHeaderCount);
    return this;
  }

  /**
   * Sets the maximum character length for the initial HTTP request line (Method + URI + Protocol).
   * Useful to prevent URI Too Long attacks.
   *
   * @param maxLineSize the maximum allowed characters in the request line
   * @return The current Levtus instance for method chaining
   */
  public Levtus maxLineSize(int maxLineSize) {
    engine.setMaxLineSize(maxLineSize);
    return this;
  }

  /**
   * Defines the maximum cumulative byte size allowed for all HTTP headers combined.
   *
   * @param maxHeaderSize the maximum header block size in bytes
   * @return The current Levtus instance for method chaining
   */
  public Levtus maxHeaderSize(int maxHeaderSize) {
    engine.setMaxHeaderSize(maxHeaderSize);
    return this;
  }

  /**
   * Designates a local directory to automatically serve static web assets (HTML, CSS, JS, Images).
   *
   * <p>{@code app.staticFiles("public");} Serves files from the "public" folder</p>
   *
   * @param staticFilesPath the relative or absolute path to the directory (must not be null)
   * @return The current Levtus instance for method chaining
   */
  public Levtus staticFiles(String staticFilesPath) {
    engine.setStaticFiles(staticFilesPath);
    return this;
  }

  /**
   * Binds the Levtus application to a specific TCP port and boots the server.
   *
   * @implNote This is a blocking operation; the main thread will halt here while the server is active.
   * @param port the valid TCP port number (1-65535) to listen on
   */
  public void listen(int port) {
    engine.start(port);
  }
}
