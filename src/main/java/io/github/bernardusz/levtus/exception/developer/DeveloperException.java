package io.github.bernardusz.levtus.exception.developer;

/** Base exception for developer-level errors (Non-HTTP errors). */
public class DeveloperException extends RuntimeException {
  /**
   * Constructs a new DeveloperException with the specified message.
   *
   * @param message the message to be passed to the superclass constructor
   */
  public DeveloperException(String message) {
    super(message);
  }

  /**
   * Constructs a new DeveloperException with the specified message and cause Exception
   *
   * @param message the message to be passed to the superclass constructor
   * @param cause the cause of DeveloperException
   */
  public DeveloperException(String message, Throwable cause) {
    super(message, cause);
  }
}
