package io.github.bernardusz.levtus.exception;

/** The type Levtus timeout exception. */
public class LevtusTimeoutException extends LevtusHttpException {
  /**
   * Instantiates a new Levtus timeout exception.
   *
   * @param message the message
   */
  public LevtusTimeoutException(String message) {
    super(message, 408);
  }
}
