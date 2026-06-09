package io.github.bernardusz.levtus.routing;

import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;

/**
 * A functional interface representing an interceptor that can process HTTP requests before they
 * reach the final route handler.
 *
 * <p>Middleware components are executed in a sequential chain. They can be used for cross-cutting
 * concerns such as logging, authentication, compression, or header manipulation.
 *
 * <p>Each middleware has the choice to:
 *
 * <ul>
 *   <li>Examine or modify the request/response and then pass control to the next handler by calling
 *       {@code next.run()}.
 *   <li>Short-circuit the request by sending a response directly and <b>not</b> calling {@code
 *       next.run()}.
 *   <li>Execution link: {@link Router#handle(Request, Response)}
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * app.use((ctx, next) -> {
 *     long start = System.currentTimeMillis();
 *     next.run();
 *     System.out.println("Request took: " + (System.currentTimeMillis() - start) + "ms");
 * });
 * }</pre>
 */
@FunctionalInterface
public interface Middleware {
  /**
   * Processes the incoming request and/or the outgoing response.
   *
   * @param ctx the execution context containing the request and response objects
   * @param next a callback to trigger the next middleware or the final route handler in the chain
   */
  void handle(LevtusContext ctx, Runnable next);
}
