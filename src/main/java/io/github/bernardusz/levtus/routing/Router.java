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
 * The type Router.
 */
public class Router {
  private final List<Middleware> globalMiddleware = new CopyOnWriteArrayList<>();
  private final Node root = new Node();

  /**
   * Add route.
   *
   * @param method the method
   * @param path the path
   * @param handler the handler
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
   * Handle.
   *
   * @param ctx the ctx
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
   * Get.
   *
   * @param path the path
   * @param handler the handler
   */
public void get(String path, Consumer<LevtusContext> handler) {
    addRoute("GET", path, handler);
  }

  /**
   * Post.
   *
   * @param path the path
   * @param handler the handler
   */
public void post(String path, Consumer<LevtusContext> handler) {
    addRoute("POST", path, handler);
  }

  /**
   * Put.
   *
   * @param path the path
   * @param handler the handler
   */
public void put(String path, Consumer<LevtusContext> handler) {
    addRoute("PUT", path, handler);
  }

  /**
   * Delete.
   *
   * @param path the path
   * @param handler the handler
   */
public void delete(String path, Consumer<LevtusContext> handler) {
    addRoute("DELETE", path, handler);
  }

  /**
   * Use.
   *
   * @param middleware the middleware
   */
  public void use(Middleware middleware) {
    globalMiddleware.add(middleware);
  }
}
