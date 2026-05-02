package io.github.bernardusz.levtus.exception;

public class PayloadTooLargeException extends LevtusHttpException {
    public PayloadTooLargeException(String message) {
        super(message, 413);
    }
}
