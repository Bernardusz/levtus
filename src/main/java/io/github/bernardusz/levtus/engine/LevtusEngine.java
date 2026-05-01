package io.github.bernardusz.levtus.engine;

import io.github.bernardusz.levtus.http.LevtusContext;
import io.github.bernardusz.levtus.http.Request;
import io.github.bernardusz.levtus.http.Response;
import io.github.bernardusz.levtus.routing.Router;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class LevtusEngine {
    public final Router router;

    public LevtusEngine(Router router) {
        this.router = router;
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
            client.setSoTimeout(5000);
            Response res = new Response(outputStream);

            try {
                Request req = parseRequest(inputStream);
                if (req == null) {
                    res.status(400).send("400 - Bad Request");
                    return;
                }

                LevtusContext ctx = new LevtusContext(req, res);
                client.setSoTimeout(10000);
                router.handle(ctx);
            }
            catch (IllegalArgumentException e) {
                res.status(400).send("400 - Bad Request (Malformed URL)");
            }
            catch (Exception e) {
                res.status(500).send("500 - Internal Server Error");
                throw e;
            }
        }
        catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    private Request parseRequest(InputStream inputStream) throws  IOException {
        String requestLine = readLine(inputStream);
        if (requestLine == null || requestLine.isEmpty()) return null;

        String[] parts = requestLine.split(" ");
        if (parts.length < 2){
            return null;
        }

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
                final int MAX_BODY_SIZE = 10 * 1024 * 1024;
                contentLength = Integer.parseInt(lengthStr);
                if (contentLength > MAX_BODY_SIZE){
                    throw new IOException("Payload Too Large: " + contentLength + " exceeds limit of " + MAX_BODY_SIZE);
                }
            }
            catch (NumberFormatException e){
                contentLength = 0;
            }
        }

        // Parse the Path
        String rawPath = parts[1];
        String path = "";
        Map<String, String> queryParams = new HashMap<>();
        if (rawPath.contains("?")) {
            int queryStart = rawPath.indexOf("?");
            path = utf8Decoder(rawPath.substring(0, queryStart));
            String queryString = rawPath.substring(queryStart + 1);

            for (String query : queryString.split("&")) {
                String[] pair = query.split("=", 2);
                if (pair.length == 2) {
                    queryParams.put(utf8Decoder(pair[0]), utf8Decoder(pair[1]));
                }
                else if (pair.length >= 1 && !pair[0].isEmpty()) {
                    queryParams.put(utf8Decoder(pair[0]), "");
                }
            }
        }
        else {
            path = utf8Decoder(rawPath);
        }

        return new Request(method, path, headers, queryParams, inputStream);
    }

    private String readLine(InputStream inputStream) throws  IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        int count = 0;
        final int MAX_LINE_SIZE = 8192; // 8 KB Limit

        while ((b = inputStream.read()) != -1) {
            if (b == '\n') break;
            if (b == '\r') continue;

            buffer.write(b);
            count++;

            if (count > MAX_LINE_SIZE){
                throw new IOException("HTTP Header line too long (Limit: " + MAX_LINE_SIZE + ")");
            }
        }
        if (b == -1 && count == 0) return "";
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private String utf8Decoder(String body){
        return URLDecoder.decode(body, StandardCharsets.UTF_8);
    }

}
