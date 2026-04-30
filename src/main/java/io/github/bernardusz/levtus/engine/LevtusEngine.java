package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class LevtusEngine {
    public final Map<String, Consumer<LevtusContext>> routes;

    public LevtusEngine(Map<String, Consumer<LevtusContext>> routes) {
        this.routes = routes;
    }

    public void start(int port) {
        try (
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            ServerSocket server = new ServerSocket(port);
        ){
            System.out.println("🚀 Levtus Engine started on port " + port);
            while (true) {
                Socket client = server.accept();
                executor.submit(() -> handleConnection(client));
            }
        }
        catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    private void handleConnection(Socket client) {
        try(
            client;
            BufferedInputStream inputStream = new BufferedInputStream(client.getInputStream());
            BufferedOutputStream outputStream = new BufferedOutputStream(client.getOutputStream());
        ){
            // Assemble the context Object
            Request req = parseRequest(inputStream);
            if (req == null) return;
            Response res = new Response(outputStream);
            LevtusContext ctx = new LevtusContext(req, res);

            // Find the route handler
            String routeKey = req.method() + ":" + req.path();
            Consumer<LevtusContext> handler = routes.get(routeKey);
            if (handler != null) {
                try {
                    handler.accept(ctx);
                }
                catch (Exception e) {
                    System.err.println("Handler error: " + e.getMessage());
                    res.status(500).send("500 - An error occurred while processing the request.");
                }
            } else {
                res.status(404).send("404 - Levtus cannot find this path.");
            }
        }
        catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    private Request parseRequest(InputStream inputStream) throws  IOException {
        String requestLine = readLine(inputStream);
        if (requestLine == null || requestLine.isEmpty()) return null;

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) return null;

        String method = parts[0];

        // Parse the Header
        String header;
        Map<String, String> headers = new HashMap<String, String>();
        while (!(header = readLine(inputStream)).isEmpty()){
            String[] headerParts = header.split(": ", 2);
            if (headerParts.length == 2){
                headers.put(headerParts[0].toLowerCase().trim(), headerParts[1].trim());
            }
        }

        // Parse the Body
        int contentLength = 0;
        String lengthStr = headers.getOrDefault("content-length", "0");
        if (!lengthStr.isEmpty()) {
            try{
                contentLength = Integer.parseInt(lengthStr);
            }
            catch (NumberFormatException e){
                contentLength = 0;
            }
        }
        byte[] body = inputStream.readNBytes(contentLength);

        // Parse the Path
        String rawPath = parts[1];
        String path = "";
        Map<String, String> queryParams = new HashMap<>();
        if (rawPath.contains("?")) {
            int queryStart = rawPath.indexOf("?");
            path = rawPath.substring(0, queryStart);
            String queryString = rawPath.substring(queryStart + 1);

            for (String query : queryString.split("&")) {
                String[] pair = query.split("=", 2);
                if (pair.length == 2) {
                    queryParams.put(pair[0], pair[1]);
                }
                else if (pair.length >= 1 && !pair[0].isEmpty()) {
                    queryParams.put(pair[0], "");
                }
            }
        }
        else {
            path = rawPath;
        }

        return new Request(method, path, headers, body, queryParams);
    }

    private String readLine(InputStream inputStream) throws  IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int b;
        while ((b = inputStream.read()) != -1) {
            if (b == '\n') break;
            if (b == '\r') continue;
            stringBuilder.append((char) b);
        }
        if (b == -1 && stringBuilder.length() == 0) return "";
        return  stringBuilder.toString();
    }
}
