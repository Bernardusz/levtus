package io.github.bernardusz.levtus.exception;

public class LevtusHttpException extends RuntimeException {
    private final int statusCode;
    public LevtusHttpException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    public int getStatusCode() {
        return statusCode;
    }
}
