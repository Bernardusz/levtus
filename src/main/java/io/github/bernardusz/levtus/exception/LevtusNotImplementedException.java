package io.github.bernardusz.levtus.exception;

/**
 * The type Levtus not implemented exception.
 */
public class LevtusNotImplementedException extends LevtusHttpException {
  /**
   * Instantiates a new Levtus not implemented exception.
   *
   * @param message the message
   */
  public LevtusNotImplementedException(String message) {
    super(message, 501);
  }
}
