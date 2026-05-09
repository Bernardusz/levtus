package io.github.bernardusz.levtus.security;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Configuration for server security.
 * @param keystorePath The path to the .p12 file.
 * @param keystorePass The password for the keystore.
 */
public record SecurityConfig(String keystorePath, String keystorePass) {
  /**
   * Is enabled boolean.
   *
   * @return  the boolean
   */
public boolean isEnabled() {
    return keystorePath != null && keystorePass != null;
  }

  /**
   * Gets server socket factory.
   *
   * @param port the port
   * @return the server socket factory
   */
  public ServerSocket getServerSocketFactory(int port) {
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

      } else if (keystorePass == null) {
        throw new java.security.NoSuchAlgorithmException("keystorePass is null");
      } else if (keystorePath == null) {
        throw new java.security.NoSuchAlgorithmException("keystorePath is null");
      }

      return serverSocketFactory.createServerSocket(port);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
