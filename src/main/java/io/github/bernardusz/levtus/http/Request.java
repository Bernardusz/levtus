package io.github.bernardusz.levtus.http;

import io.github.bernardusz.levtus.exception.PayloadTooLargeException;

import java.io.*;
import java.util.List;
import java.util.Map;

public class Request{
    private final String method;
    private final String path;
    private final Map<String, List<String>> headers;
    private final Map<String, String> queryParams;
    private final InputStream bodyStream;
    private byte[] cachedBody;
    private int bytesRead;
    private final int maxBodySize;

    public Request(String method, String path, Map<String, List<String>> headers, Map<String, String> queryParams, InputStream bodyStream, int maxBodySize){
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
        this.bodyStream = bodyStream;
        this.maxBodySize = maxBodySize;
    }

    public String method(){
        return method;
    }
    public String path(){
        return path;
    }
    public  Map<String, List<String>> headers(){
        return headers;
    }
    public Map<String, String> queryParams(){
        return queryParams;
    }
    public List<String> getHeaders(String name) {
        return headers.getOrDefault(name.toLowerCase(), List.of());
    }
    public int bytesRead(){
        return bytesRead;
    }
    public void setBytesRead(int bytesRead){
        this.bytesRead = bytesRead;
    }
    public String contentType(){
        return getHeaders("content-type").isEmpty() ? "text/plain" : getHeaders("content-type").getFirst();
    }
    public int contentLength(){
        try{
            return Integer.parseInt(getHeaders("content-length").isEmpty() ? "0" : getHeaders("content-length").getFirst());
        }
        catch (NumberFormatException e){
            return 0;
        }
    }
    public String query(String key){
        return  queryParams.getOrDefault(key, "");
    }
    public boolean isCached(){
        return cachedBody != null;
    }
    public byte[] body() {
        if (cachedBody != null) return cachedBody;
        try{
            if (contentLength() > maxBodySize){
                throw new PayloadTooLargeException("Request body is too large");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192]; // 8KB chunks
            int nRead;
            int totalRead = 0;
            while (totalRead < contentLength() &&
                (nRead = bodyStream.read(data, 0, Math.min(data.length, contentLength() - totalRead))) != -1) {
                buffer.write(data, 0, nRead);
                totalRead += nRead;
                setBytesRead(totalRead);
                if (totalRead > maxBodySize){
                    throw new PayloadTooLargeException("HTTP Body too long (Limit: " + maxBodySize + ")");
                }
            }
            cachedBody = buffer.toByteArray();
            return cachedBody;
        }
        catch (IOException e){
            throw new UncheckedIOException("Failed to read request body", e);
        }
    }
}
