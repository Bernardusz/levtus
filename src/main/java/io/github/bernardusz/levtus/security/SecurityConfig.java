package io.github.bernardusz.levtus.security;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Configuration for the server's security layer, specifically SSL/TLS settings.
 *
 * <p>This record manages the loading of cryptographic keys and the instantiation of the {@link
 * ServerSocket}. It supports both standard HTTP and secure HTTPS connections depending on whether
 * keystore information is provided.
 *
 * @param keystorePath the file system path to the .p12 (PKCS12) keystore file; if null, SSL is
 *     disabled
 * @param keystorePass the password required to unlock and read the keystore file
 */
public record SecurityConfig(String keystorePath, String keystorePass) {
  /**
   * Determines if secure connections (HTTPS) are enabled based on the current configuration.
   *
   * @return {@code true} if both keystore path and password are provided, {@code false} otherwise
   */
  public boolean isEnabled() {
    return keystorePath != null && keystorePass != null;
  }

  /**
   * Instantiates and returns a {@link ServerSocket} bound to the specified port.
   *
   * <p>If security is enabled via a valid keystore, this method initializes an {@link SSLContext}
   * with TLSv1.3 and returns a secure socket. Otherwise, it returns a standard plain-text socket.
   *
   * @param port the TCP port to bind the server socket to
   * @return a configured ServerSocket (secure or non-secure)
   * @throws RuntimeException if the keystore cannot be loaded or the SSL context fails to
   *     initialize
   */
  public ServerSocket getServerSocket(int port) {
    try {
      ServerSocketFactory serverSocketFactory = null;
      if (keystorePath != null && keystorePass != null) {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password = keystorePass.toCharArray();

        try (FileInputStream keyStoreFileInputStream = new FileInputStream(keystorePath)) {
          keyStore.load(keyStoreFileInputStream, password);
        }

        KeyManagerFactory kmf =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

        serverSocketFactory = sslContext.getServerSocketFactory();
      } else if (keystorePath == null && keystorePass == null) {
        serverSocketFactory = ServerSocketFactory.getDefault();

      } else {
        throw new IllegalArgumentException("Both keystorePath and keystorePass must be provided for SSL, or both must be null for plain HTTP");
      }

      return serverSocketFactory.createServerSocket(port);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
