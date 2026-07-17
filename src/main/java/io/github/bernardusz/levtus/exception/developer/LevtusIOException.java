package io.github.bernardusz.levtus.exception.developer;

/**
 * Thrown when an unrecoverable I/O error occurs within the Levtus framework. This is a runtime
 * exception to keep the framework API fluent and boilerplate-free.
 */
public class LevtusIOException extends DeveloperException {

  /**
   * Constructs a new LevtusIOException with the specified message.
   *
   * @param message the message to be passed to the superclass constructor
   */
  public LevtusIOException(String message) {
    super(message);
  }

  /**
   * Constructs a new LevtusIOException with the specified message and cause Exception
   *
   * @param message the message to be passed to the superclass constructor
   * @param cause the cause of the LevtusIOException
   */
  public LevtusIOException(String message, Throwable cause) {
    super(message, cause);
  }
}
