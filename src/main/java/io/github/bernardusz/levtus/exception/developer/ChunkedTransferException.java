package io.github.bernardusz.levtus.exception.developer;

/**
 * Developer Exception for Chunked Transfer Encoding
 *
 * <p>Primarily used internally by the Levtus engine during the HTTP parsing phase. When developer switch the transfer mode to chunked when they had already used normal/bulk mode</p>
 */
public class ChunkedTransferException extends DeveloperException {
  /**
   * Constructs a new ChunkedTransferException with the specified message.
   *
   * @param message the message to be passed to the superclass constructor
   */
  public ChunkedTransferException(String message) {
    super(message);
  }

  /**
   * Construct a new ChunkedTransferException with the specified message and the cause of Exception
   *
   * @param message the message to be passed to superclass constructor
   * @param cause the cause of the exception
   */
  public ChunkedTransferException(String message, Throwable cause){
    super(message, cause);
  }
}
