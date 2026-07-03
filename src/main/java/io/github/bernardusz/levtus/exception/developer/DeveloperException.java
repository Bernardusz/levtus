package io.github.bernardusz.levtus.exception.developer;

/**
 * Base exception for developer-level errors (Non-HTTP errors).
 */
public class DeveloperException extends RuntimeException {
  /**
   * Constructs a new DeveloperException with the specified message.
   *
   * @param message the message to be passed to the superclass constructor
   */
  public DeveloperException(String message) {
    super(message);
  }
}
