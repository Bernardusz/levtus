package io.github.bernardusz.levtus.spi;

import java.net.Socket;

/**
 * The ConnectionHandler interface defines the contract for handling client connections.
 * Implementations of this interface are responsible for protocol-specific logic (e.g., HTTP, MQTT).
 *
 * @author Bernardusz
 * @version 0.1.1
 */
public interface ConnectionHandler {
  /**
   * Handles the client connection.
   *
   * <p>Implementations are responsible for managing the lifecycle of the socket, including
   * input/output parsing and ensuring the socket is properly closed. All protocol-level exceptions
   * should be caught and handled within this method to prevent leaking errors back to the engine's
   * main loop.
   *
   * @param socket the client socket to process
   */
  void handle(Socket socket);
}
