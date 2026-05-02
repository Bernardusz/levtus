package io.github.bernardusz.levtus.exception;

public class UnauthorizedException extends LevtusHttpException {
    public UnauthorizedException(String message) {
        super(message, 401);
    }
}
