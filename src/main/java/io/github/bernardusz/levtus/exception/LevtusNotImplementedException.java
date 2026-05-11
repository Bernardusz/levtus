package io.github.bernardusz.levtus.exception;

/** Exception thrown when a client sends a request that Levtus hasn't implemented a response for.
 *
 * <p>Results in an HTTP {@code 501 Not Implemented} response.</p>
 *
 * Extended from: {@link LevtusHttpException}
 */
public class LevtusNotImplementedException extends LevtusHttpException {
  /**
   * Constructs a new {@code LevtusNotImplementedException} with the specified message and the status code of 501.
   *
   * @param message the detail message explaining the error
   */
  public LevtusNotImplementedException(String message) {
    super(message, 501);
  }
}
