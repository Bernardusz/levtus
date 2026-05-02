package io.github.bernardusz.levtus.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Response {
    private final BufferedOutputStream output;
    String staticFilesPath;
    private int statusCode = 200;
    private Map<String, List<String>> headers = new HashMap<>();
    private boolean isSent = false;

    public Response(BufferedOutputStream output, String staticFilesPath) {
        this.output = output;
        this.staticFilesPath = staticFilesPath;
        headers.computeIfAbsent("Content-Type", _ -> new ArrayList<>(List.of("text/plain")));
        headers.computeIfAbsent("Server", _ -> new ArrayList<>(List.of("Levtus-v0.1")));
    }

    public Response status(int code){
        this.statusCode = code;
        return this;
    }

    public void contentType(String type){
        headers.put("Content-Type", new ArrayList<>(List.of(type)));
    }

    public Response addHeader(String name, String value){
        headers.computeIfAbsent(name, _ -> new ArrayList<>());
        headers.get(name).add(value);
        return this;
    }

    public boolean isSent() {
        return isSent;
    }

    public void send(String body) {
        send(body.getBytes(StandardCharsets.UTF_8));
    }
    public void html(String body) {
        contentType("text/html");
        send(body.getBytes(StandardCharsets.UTF_8));
    }
    public void text(String body) {
        contentType("text/plain");
        send(body.getBytes(StandardCharsets.UTF_8));
    }
    public void sendBinary(byte[] body) {
        contentType("application/octet-stream");
        send(body);
    }
    public void json(String body) {
        contentType("application/json");
        send(body.getBytes(StandardCharsets.UTF_8));
    }
    public void render(String htmlPath){
        Path filePath = Path.of(staticFilesPath, htmlPath).normalize();

        Path rootPath = Path.of(staticFilesPath).toAbsolutePath().normalize();
        if (!filePath.toAbsolutePath().startsWith(rootPath)) {
            this.status(403).send("403 Forbidden");
            return;
        }

        if (Files.exists(filePath) && !Files.isDirectory(filePath)){
            try {
                String mimeType = Files.probeContentType(filePath);
                if (mimeType == null) mimeType = "application/octet-stream";
                this.contentType(mimeType);
                this.status(200);

                if (isSent) return;
                isSent = true;

                writeStatus(statusCode);
                writeHeaders(Files.size(filePath));
                Files.copy(filePath, output);
                output.flush();
            }
            catch (IOException e){
                throw new UncheckedIOException(e);
            }
        }
        else {
            this.status(404).send("404 Not Found");
        }
    }


    public void send(byte[] bodyBytes) {
        if (isSent) return;
        isSent = true;
        try{
            // HTTP Status
            writeStatus(statusCode);
            writeHeaders(bodyBytes.length);
            writeBody(bodyBytes);
            output.flush();
        }
        catch (IOException e) {
            System.err.println("Failed to send response: " + e.getMessage());
        }
    }

    private void writeHeaders(long contentLength) throws IOException {
        headers.put("Content-Length", new ArrayList<>(List.of(String.valueOf(contentLength))));
        for (var entry : headers.entrySet()) {
            for (var headerValue : entry.getValue()) {
                output.write((purifyHeader(entry.getKey()) + ": " + purifyHeader(headerValue) + "\r\n").getBytes());
            }
        }
        output.write("\r\n".getBytes());
    }

    private void writeBody(byte[] bodyBytes) throws IOException {
        output.write(bodyBytes);
    }

    private void writeStatus(int statusCode) throws IOException {
        output.write(("HTTP/1.1 " + statusCode + " " + getStatusText(statusCode) + "\r\n").getBytes());
    }

    // A method to clean a header from \r and \n
    private String purifyHeader(String header){
        if (!header.contains("\r") && !header.contains("\n") && !header.contains("\0")) return header;
        return header.replace("\r", "").replace("\n", "").replace("\0", "");
    }

    private String getStatusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 503 -> "Service Unavailable";
            default ->  "Unknown";
        };
    }
}
