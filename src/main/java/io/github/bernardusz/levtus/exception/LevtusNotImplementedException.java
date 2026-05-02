package io.github.bernardusz.levtus.exception;

public class LevtusNotImplementedException extends LevtusHttpException {
    public LevtusNotImplementedException(String message) {
        super(message, 501);
    }
}
