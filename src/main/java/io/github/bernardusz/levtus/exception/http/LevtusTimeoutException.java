package io.github.bernardusz.levtus.exception.http;

/**
 * Exception thrown when a client takes too long to complete a response (Socket timeout).
 *
 * <p>Results in an HTTP {@code 408 Request Timeout} response. Extended from: {@link
 * LevtusHttpException}
 */
public class LevtusTimeoutException extends LevtusHttpException {
  /**
   * Constructs a new {@code LevtusTimeoutException} with the specified message and the status code
   * of 408.
   *
   * @param message the detail message explaining the error
   */
  public LevtusTimeoutException(String message) {
    super(message, 408);
  }
}
