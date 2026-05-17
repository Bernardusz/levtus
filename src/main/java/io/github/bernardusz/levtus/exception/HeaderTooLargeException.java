package io.github.bernardusz.levtus.exception;

/** Exception thrown when a client sends a request with header that exceeds header size (line size, header count, etc.).
 *
 * Configuration in {@link io.github.bernardusz.levtus.engine.LevtusEngine}.
 *
 * <p>Results in an HTTP {@code 431 Header Too Large} response.</p>
 *
 * Extended from: {@link LevtusHttpException}
 */
public class HeaderTooLargeException extends LevtusHttpException {
  /**
   * Constructs a new {@code HeaderTooLargeException} with the specified message and the status code of 431.
   *
   * @param message the detail message explaining the error
   */
  public HeaderTooLargeException(String message) {
    super(message, 431);
  }
}
