package io.github.bernardusz.levtus.exception.http;

/**
 * Base exception class for all HTTP-related errors in the Levtus engine.
 *
 * <p>This exception carries an HTTP status code that will be sent back to the client when the
 * exception is caught by the {@link io.github.bernardusz.levtus.engine.LevtusEngine}.
 *
 * @author Bernardusz
 * @version 1.0
 */
public class LevtusHttpException extends RuntimeException {
  /** The HTTP status code associated with this error. */
  private final int statusCode;

  /**
   * Constructs a new {@code LevtusHttpException} with the specified message and status code.
   *
   * @param message the detail message explaining the error
   * @param statusCode the integer HTTP status code (e.g., 404, 500)
   */
  public LevtusHttpException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Returns the HTTP status code associated with this exception.
   *
   * @return the integer status code
   */
  public int getStatusCode() {
    return statusCode;
  }
}
