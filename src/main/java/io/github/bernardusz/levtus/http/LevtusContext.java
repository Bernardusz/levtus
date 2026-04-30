package io.github.bernardusz.levtus.http;

public record LevtusContext(
    Request req,
    Response res
) {
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
