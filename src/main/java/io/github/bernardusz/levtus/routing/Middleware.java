package io.github.bernardusz.levtus.routing;

import io.github.bernardusz.levtus.http.LevtusContext;

@FunctionalInterface
public interface Middleware {
    void handle(LevtusContext ctx, Runnable next);
}
