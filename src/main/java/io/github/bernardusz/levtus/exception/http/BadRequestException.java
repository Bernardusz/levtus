package io.github.bernardusz.levtus.exception.http;

/**
 * Exception thrown when a client sends a request with a malformed URL or invalid syntax.
 *
 * <p>Results in an HTTP {@code 400 Bad Request} response. Extended from: {@link
 * LevtusHttpException}
 */
public class BadRequestException extends LevtusHttpException {
  /**
   * Constructs a new {@code BadRequestException} with the specified message and the status code of
   * 400.
   *
   * @param message the detail message explaining the error
   */
  public BadRequestException(String message) {
    super(message, 400);
  }
}
