package io.github.bernardusz.levtus.exception;

/** The type Unauthorized exception. */
public class UnauthorizedException extends LevtusHttpException {
  /**
   * Instantiates a new Unauthorized exception.
   *
   * @param message the message
   */
  public UnauthorizedException(String message) {
    super(message, 401);
  }
}
