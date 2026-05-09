package io.github.bernardusz.levtus.exception;

/** The type Payload too large exception. */
public class PayloadTooLargeException extends LevtusHttpException {
  /**
   * Instantiates a new Payload too large exception.
   *
   * @param message the message
   */
  public PayloadTooLargeException(String message) {
    super(message, 413);
  }
}
