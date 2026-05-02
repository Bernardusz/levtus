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

    public void ssl(String keystorePath, String keystorePass) {
        engine.ssl(keystorePath, keystorePass);
    }

    public void setMaxConcurrentConnections(int maxConcurrentConnections){
        engine.setMaxConcurrentConnections(maxConcurrentConnections);
    }

    public void setMaxEmptyLines(int maxEmptyLines){
        engine.setMaxEmptyLines(maxEmptyLines);
    }

    public void setMaxBodySize(int maxBodySize){
        engine.setMaxBodySize(maxBodySize);
    }

    public void setMaxHeaderCount(int maxHeaderCount){
        engine.setMaxHeaderCount(maxHeaderCount);
    }

    public void setMaxLineSize (int maxLineSize){
        engine.setMaxLineSize(maxLineSize);
    }

    public void setMaxHeaderSize(int maxHeaderSize){
        engine.setMaxHeaderSize(maxHeaderSize);
    }

    public void staticFiles(String staticFilesPath){
        engine.setStaticFiles(staticFilesPath);
    }


    public void listen(int port) {
        engine.start(port);
    }
}