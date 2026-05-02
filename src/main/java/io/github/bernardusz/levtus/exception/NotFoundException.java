package io.github.bernardusz.levtus.exception;

public class NotFoundException extends LevtusHttpException {
    public NotFoundException(String message) {
        super(message, 404);
    }
}
