package io.github.bernardusz.levtus.exception;

/** Exception thrown when the object client requested isn't found.
 *
 * <p>Results in an HTTP {@code 404 Not Found} response.</p>
 *
 * Extended from: {@link LevtusHttpException}
 */
public class NotFoundException extends LevtusHttpException {
  /**
   * Constructs a new {@code NotFoundException} with the specified message and the status code of 404.
   *
   * @param message the detail message explaining the error
   */
  public NotFoundException(String message) {
    super(message, 404);
  }
}
