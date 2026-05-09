package io.github.bernardusz.levtus.exception;

/** The type Bad request exception. */
public class BadRequestException extends LevtusHttpException {
  /**
   * Instantiates a new Bad request exception.
   *
   * @param message the message
   */
  public BadRequestException(String message) {
    super(message, 400);
  }
}
