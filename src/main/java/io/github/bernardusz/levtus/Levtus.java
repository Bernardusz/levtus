package io.github.bernardusz.levtus;

import io.github.bernardusz.levtus.engine.LevtusEngine;
import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.routing.Middleware;
import io.github.bernardusz.levtus.routing.Router;

import java.util.function.Consumer;

public class Levtus {
    private final Router router = new Router();
    private final LevtusEngine engine;

    private Levtus() {
        this.engine = new LevtusEngine(router);
    }

    public static Levtus create() {
        return new Levtus();
    }

    public void get(String path, Consumer<LevtusContext> handler) {
        router.get(path, handler);
    }

    public void post(String path, Consumer<LevtusContext> handler) {
        router.post(path, handler);
    }

    public void put(String path, Consumer<LevtusContext> handler) {
        router.put(path, handler);
    }

    public void delete(String path, Consumer<LevtusContext> handler) {
        router.delete(path, handler);
    }

    public void use(Middleware middleware) {
        router.use(middleware);
    }

    public void listen(int port) {
        engine.start(port);
    }
}