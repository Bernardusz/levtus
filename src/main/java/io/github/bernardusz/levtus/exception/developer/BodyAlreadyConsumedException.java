package io.github.bernardusz.levtus.exception.developer;

/**
 * Exception thrown when the request body has already been consumed.
 *
 * <p>Extended from: {@link DeveloperException}
 */
public class BodyAlreadyConsumedException extends DeveloperException {
  /**
   * Constructs a new BodyAlreadyConsumedException with the specified message.
   *
   * @param message the message to be passed to the superclass constructor
   */
  public BodyAlreadyConsumedException(String message) {
    super(message);
  }

  /**
   * Constructs a new BodyAlreadyConsumedException with the specified message and cause Exception
   *
   * @param message the message to be passed to the superclass constructor
   * @param cause the cause of BodyAlreadyConsumedException
   */
  public BodyAlreadyConsumedException(String message, Throwable cause) {
    super(message, cause);
  }
}
