package io.github.bernardusz.levtus.exception;

public class LevtusTimeoutException extends LevtusHttpException {
    public LevtusTimeoutException(String message) {
        super(message, 408);
    }
}
