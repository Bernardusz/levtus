package io.github.bernardusz.levtus.exception;

/** The type Header too large exception. */
public class HeaderTooLargeException extends LevtusHttpException {
  /**
   * Instantiates a new Header too large exception.
   *
   * @param message the message
   */
  public HeaderTooLargeException(String message) {
    super(message, 431);
  }
}
