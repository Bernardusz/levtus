package io.github.bernardusz.levtus.exception;

public class BadRequestException extends LevtusHttpException {
    public BadRequestException(String message) {
        super(message, 400);
    }
}
