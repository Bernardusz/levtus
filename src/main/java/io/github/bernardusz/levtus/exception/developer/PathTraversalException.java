package io.github.bernardusz.levtus.exception.developer;

/**
 * Thrown when developers pass a path that contains traversal characters.
 *
 * <p>Thrown by methods:
 *
 * <ul>
 *   <li>{@link io.github.bernardusz.levtus.http.Response#render(String)}
 *   <li>{@link io.github.bernardusz.levtus.http.Response#sendFile(String)}
 *   <li>{@link io.github.bernardusz.levtus.http.Response#sendBinary(String)}
 * </ul>
 */
public class PathTraversalException extends DeveloperException {
  /**
   * Constructs a new PathTraversalException with the specified message.
   *
   * @param message the message to be passed to the superclass constructor
   */
  public PathTraversalException(String message) {
    super(message);
  }

  /**
   * Constructs a new PathTraversalException with the specified message and cause Exception
   *
   * @param message the message to be passed to the superclass constructor
   * @param cause the cause of PathTraversalException
   */
  public PathTraversalException(String message, Throwable cause) {
    super(message, cause);
  }
}
