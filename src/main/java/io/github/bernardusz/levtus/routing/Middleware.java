package io.github.bernardusz.levtus.routing;

import io.github.bernardusz.levtus.http.LevtusContext;

/** The interface Middleware. */
@FunctionalInterface
public interface Middleware {
  /**
   * Handle.
   *
   * @param ctx the ctx
   * @param next the next
   */
  void handle(LevtusContext ctx, Runnable next);
}
