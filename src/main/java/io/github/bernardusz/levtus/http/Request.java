package io.github.bernardusz.levtus.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import java.io.UncheckedIOException;

public class Request{
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final InputStream bodyStream;
    private byte[] cachedBody;

    public Request(String method, String path, Map<String, String> headers, Map<String, String> queryParams, InputStream bodyStream){
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
        this.bodyStream = bodyStream;
    }

    public String method(){
        return method;
    }
    public String path(){
        return path;
    }
    public  Map<String, String> headers(){
        return headers;
    }
    public Map<String, String> queryParams(){
        return queryParams;
    }

    public String getHeader(String name) {
        return headers.getOrDefault(name.toLowerCase(), "");
    }
    public String contentType(){
        return  getHeader("Content-Type");
    }
    public int contentLength(){
        try{
            return Integer.parseInt(getHeader("content-length"));
        }
        catch (NumberFormatException e){
            return 0;
        }
    }
    public String query(String key){
        return  queryParams.getOrDefault(key, "");
    }
    public byte[] body() {
        if (cachedBody == null){
            try {
                cachedBody = bodyStream.readNBytes(contentLength());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return cachedBody;
    }
}
