package io.github.bernardusz.levtus.routing;

import io.github.bernardusz.levtus.http.LevtusContext;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The Router for storing routes (and their handler) and handling requests.
 *
 * <p>Router is responsible for:
 *
 * <ul>
 *   <li>Storing routes (and their handler) as Nodes in a Prefix Tree (Trie) {@link Router#root}
 *   <li>Executing the middleware chain {@link Router#executeChain(LevtusContext, Consumer)}
 *   <li>Executing the route handler {@link Router#handle(LevtusContext)}
 * </ul>
 *
 * <p>TLDR: The router is the main entry point for handling HTTP requests and routing them to the
 * appropriate handlers.
 */
public class Router {
  /** The global CopyOnWriteArrayList of middleware that is executed before the route handler. */
  private final List<Middleware> globalMiddleware = new CopyOnWriteArrayList<>();

  /**
   * The root node of the Prefix Tree (Trie) that stores the routes.
   *
   * <p>Inside this Node is a Trie structure, that stores the routes.
   *
   * <p>TLDR: The root node is the starting point of the Trie structure that stores the routes.
   */
  private final Node root = new Node();

  /**
   * The method that stores the route in the Trie structure.
   *
   * @param method the HTTP method
   * @param path the path of the route
   * @param handler the handler/lambda of the route
   */
  public void addRoute(String method, String path, Consumer<LevtusContext> handler) {
    Node current = root.children.computeIfAbsent(method.toUpperCase(), k -> new Node());
    for (String segment : path.split("/")) {
      if (segment.isEmpty()) continue;

      if (segment.startsWith("{") && segment.endsWith("}")) {
        String name = segment.substring(1, segment.length() - 1);

        if (current.wildcardChild != null && !(current.wildcardChild.wildcardName.equals(name))) {
          throw new IllegalStateException(
              "You can't have 2 wildcards with different names on the same spot.");
        }

        if (current.wildcardChild == null) current.wildcardChild = new Node();
        current = current.wildcardChild;
        current.wildcardName = name;
      } else {
        current = current.children.computeIfAbsent(segment.toUpperCase(), k -> new Node());
      }
    }
    current.handler = handler;
  }

  /**
   * The method that handles the request by matching the route and executing the middleware chain
   * first before the route handler.
   *
   * @param ctx the {@link LevtusContext} containing the request and response objects
   */
  public void handle(LevtusContext ctx) {
    String method = ctx.req().method().toUpperCase();
    String path = ctx.req().path();

    Node current = root.children.get(method);
    if (current == null) {
      ctx.res().status(404).send("404 - Not Found");
      return;
    }

    Map<String, String> params = new HashMap<>();
    for (String segment : path.split("/")) {
      if (segment.isEmpty()) continue;

      segment = utf8Decoder(segment);
      String upperSegment = segment.toUpperCase();

      if (current.children.containsKey(upperSegment)) {
        current = current.children.get(upperSegment);
      } else if (current.wildcardChild != null) {
        current = current.wildcardChild;
        params.put(current.wildcardName, segment);
      } else {
        ctx.send(404, "404 - Not Found");
        return;
      }
    }
    if (current.handler != null) {
      ctx.setPathParams(params);
      executeChain(ctx, current.handler);
    } else {
      ctx.send(404, "404 - Not Found");
    }
  }

  /**
   * The method that builds the chain of execution, going from the router handler to last middleware
   * to the first middleware.
   *
   * <p>Explanation:
   *
   * <ul>
   *   <li>Creates a {@link Runnable} that represents the final handler
   *   <li>Iterates over the global middleware in reverse order
   *   <li>For each middleware, creates a new {@link Runnable} that calls the middleware with the
   *       next step
   *   <li>Finally, calls the {@link Runnable} that represents the final handler
   * </ul>
   *
   * <p>TLDR: The method that builds the chain of execution, going from the router handler to last
   * middleware to the first middleware before executing them.
   *
   * @param ctx The {@link LevtusContext} containing the request and response objects
   * @param finalHandler The terminal route handler to be executed at the end of the chain
   */
  private void executeChain(LevtusContext ctx, Consumer<LevtusContext> finalHandler) {
    Runnable current = () -> finalHandler.accept(ctx);

    for (int i = globalMiddleware.size() - 1; i >= 0; i--) {
      Middleware middleware = globalMiddleware.get(i);
      final Runnable nextStep = current;
      current = () -> middleware.handle(ctx, nextStep);
    }

    current.run();
  }

  private String utf8Decoder(String body) throws IllegalArgumentException {
    return URLDecoder.decode(body, StandardCharsets.UTF_8);
  }

  /**
   * A shortcut for adding a GET route.
   *
   * <p>Reference: {@link #addRoute(String, String, Consumer)}
   *
   * @param path the path
   * @param handler the handler
   */
  public void get(String path, Consumer<LevtusContext> handler) {
    addRoute("GET", path, handler);
  }

  /**
   * A shortcut for adding a POST route.
   *
   * <p>Reference: {@link #addRoute(String, String, Consumer)}
   *
   * @param path the path
   * @param handler the handler
   */
  public void post(String path, Consumer<LevtusContext> handler) {
    addRoute("POST", path, handler);
  }

  /**
   * A shortcut for adding a PUT route.
   *
   * <p>Reference: {@link #addRoute(String, String, Consumer)}
   *
   * @param path the path
   * @param handler the handler
   */
  public void put(String path, Consumer<LevtusContext> handler) {
    addRoute("PUT", path, handler);
  }

  /**
   * A shortcut for adding a DELETE route.
   *
   * <p>Reference: {@link #addRoute(String, String, Consumer)}
   *
   * @param path the path
   * @param handler the handler
   */
  public void delete(String path, Consumer<LevtusContext> handler) {
    addRoute("DELETE", path, handler);
  }

  /**
   * Adds a middleware to the global middleware chain.
   *
   * @param middleware the Middleware
   */
  public void use(Middleware middleware) {
    globalMiddleware.add(middleware);
  }
}
