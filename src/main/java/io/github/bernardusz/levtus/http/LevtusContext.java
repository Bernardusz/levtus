package io.github.bernardusz.levtus.http;

import java.util.Map;

public class LevtusContext{
    Request req;
    Response res;
    Map<String, String> pathParams;

    public LevtusContext(Request req, Response res){
        this.req = req;
        this.res = res;
    }

    public Request req() { return req; }
    public Response res() { return res; }

    public void setPathParams(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }

    public String param(String name){
        return pathParams != null ? pathParams.getOrDefault(name, "") : "";
    }
    public String query(String name){
        return req.query(name);
    }

    public void send(String data) {
        res.send(data);
    }
    public void send(int code, String data) {
        res.status(code).send(data);
    }
    public void send(int code, String contentType, String data) {
        res.status(code).contentType(contentType).send(data);
    }
}
