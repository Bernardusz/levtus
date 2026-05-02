package io.github.bernardusz.levtus.exception;

public class HeaderTooLargeException extends LevtusHttpException {
    public HeaderTooLargeException(String message) {
        super(message, 431);
    }
}
