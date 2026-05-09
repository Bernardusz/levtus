package io.github.bernardusz.levtus.exception;

/** The type Levtus http exception. */
public class LevtusHttpException extends RuntimeException {
  /** The status code. */
  private final int statusCode;

  /**
   * Instantiates a new Levtus http exception.
   *
   * @param message the message
   * @param statusCode the status code
   */
  public LevtusHttpException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Gets status code.
   *
   * @return the status code
   */
  public int getStatusCode() {
    return statusCode;
  }
}
