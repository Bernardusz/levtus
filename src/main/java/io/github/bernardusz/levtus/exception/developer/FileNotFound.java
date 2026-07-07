package io.github.bernardusz.levtus.exception.developer;

import java.nio.file.Path;

/**
 * Thrown when developer attempts to access a file that does not exist.
 *
 * <p>Thrown by these methods:
 *
 * <ul>
 *   <li>{@link io.github.bernardusz.levtus.http.Response#render(String)}
 *   <li>{@link io.github.bernardusz.levtus.http.Response#sendFile(Path)}}
 *   <li>{@link io.github.bernardusz.levtus.http.Response#sendFile(String)}
 *   <li>{@link io.github.bernardusz.levtus.http.Response#sendBinary(String)}
 *   <li>{@link io.github.bernardusz.levtus.http.Response#sendBinary(Path)}
 * </ul>
 */
public class FileNotFound extends DeveloperException {
  /**
   * Constructs a new FileNotFound exception with the specified message.
   *
   * @param message the message to be passed to the superclass constructor
   */
  public FileNotFound(String message) {
    super(message);
  }

  /**
   * Constructs a new FileNotFound with the specified message and cause Exception
   *
   * @param message the message to be passed to the superclass constructor
   * @param cause the cause of FileNotFound
   */
  public FileNotFound(String message, Throwable cause) {
    super(message, cause);
  }
}
