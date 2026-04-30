package io.github.bernardusz.levtus.http;

import java.util.Map;

public record Request(
    String method,
    String path,
    Map<String, String> headers,
    byte[] body,
    Map<String, String> queryParams
) {
    public String getHeader(String name) {
        return headers.getOrDefault(name.toLowerCase(), "");
    }
    public String contentType(){
        return  getHeader("Content-Type");
    }
    public int contentLength(){
        return body.length;
    }
    public String query(String key){
        return  queryParams.getOrDefault(key, "");
    }
}
