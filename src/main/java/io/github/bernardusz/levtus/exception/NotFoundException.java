package io.github.bernardusz.levtus.exception;

/**
 * The type Not found exception.
 */
public class NotFoundException extends LevtusHttpException {
  /**
   * Instantiates a new Not found exception.
   *
   * @param message the message
   */
  public NotFoundException(String message) {
    super(message, 404);
  }
}
