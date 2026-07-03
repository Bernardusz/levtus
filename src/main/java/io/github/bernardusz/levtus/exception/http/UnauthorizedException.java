package io.github.bernardusz.levtus.exception.http;

/**
 * Exception thrown when client sends an unauthorized request.
 *
 * <p>Results in an HTTP {@code 401 Unauthorized} response. Extended from: {@link
 * LevtusHttpException}
 */
public class UnauthorizedException extends LevtusHttpException {
  /**
   * Constructs a new {@code UnauthorizedException} with the specified message and the status code
   * of 401.
   *
   * @param message the detail message explaining the error
   */
  public UnauthorizedException(String message) {
    super(message, 401);
  }
}
