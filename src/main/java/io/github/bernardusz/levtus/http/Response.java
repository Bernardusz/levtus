package io.github.bernardusz.levtus.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Response {
    private final BufferedOutputStream output;
    private int statusCode = 200;
    private Map<String, String> headers = new HashMap<String, String>();
    private boolean isSent = false;

    public Response(BufferedOutputStream output) {
        this.output = output;
        headers.put("Content-Type", "text/plain");
        headers.put("Server", "Levtus-v0.1");
    }

    public Response status(int code){
        this.statusCode = code;
        return this;
    }

    public Response contentType(String type){
        this.headers.put("Content-Type", type);
        return this;
    }

    public void send(String body){
        if (isSent) return;
        isSent = true;
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
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

    private void writeHeaders(int contentLength) throws IOException {
        headers.put("Content-Length", utf8Decoder(String.valueOf(contentLength)));
        for (var entry : headers.entrySet()) {
            output.write((utf8Decoder(entry.getKey()) + ": " + utf8Decoder(entry.getValue()) + "\r\n").getBytes());
        }
        output.write("\r\n".getBytes());
    }

    private void writeBody(byte[] bodyBytes) throws IOException {
        output.write(bodyBytes);
    }

    private void writeStatus(int statusCode) throws IOException {
        output.write(("HTTP/1.1 " + statusCode + " " + getStatusText(statusCode) + "\r\n").getBytes());
    }

    private String utf8Decoder(String body){
        return URLDecoder.decode(body, StandardCharsets.UTF_8);
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
