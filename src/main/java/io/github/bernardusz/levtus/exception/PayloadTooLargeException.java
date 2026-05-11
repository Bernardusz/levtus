package io.github.bernardusz.levtus.exception;

/** Exception thrown when client sends a request with payload/body that exceeds payload size.
 *
 * Configuration in {@link io.github.bernardusz.levtus.engine.LevtusEngine}.
 *
 * <p>Results in an HTTP {@code 413 Content Too Large} response.</p>
 *
 * Extended from: {@link LevtusHttpException}
 */
public class PayloadTooLargeException extends LevtusHttpException {
  /**
   * Constructs a new {@code PayloadTooLargeException} with the specified message and the status code of 413.
   *
   * @param message the detail message explaining the error
   */
  public PayloadTooLargeException(String message) {
    super(message, 413);
  }
}
