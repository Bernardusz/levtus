package io.github.bernardusz.levtus.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    @Test
    void testIsEnabled() {
        SecurityConfig secure = new SecurityConfig("path/to/keystore", "password");
        assertTrue(secure.isEnabled(), "Should be enabled when both path and pass are provided");

        SecurityConfig insecure = new SecurityConfig(null, null);
        assertFalse(insecure.isEnabled(), "Should be disabled when both are null");

        SecurityConfig partialPath = new SecurityConfig("path", null);
        assertFalse(partialPath.isEnabled(), "Should be disabled when password is null");

        SecurityConfig partialPass = new SecurityConfig(null, "pass");
        assertFalse(partialPass.isEnabled(), "Should be disabled when path is null");
    }

    @Test
    void testGetDefaultServerSocket() throws IOException {
        SecurityConfig config = new SecurityConfig(null, null);
        // Use port 0 to let the OS pick a free port
        try (ServerSocket socket = config.getServerSocket(0)) {
            assertNotNull(socket);
            assertFalse(socket.getClass().getName().contains("ssl"), "Should be a standard ServerSocket");
        }
    }

    @Test
    void testGetServerSocketWithMissingCredentialsThrows() {
        SecurityConfig missingPass = new SecurityConfig("some/path", null);
        assertThrows(RuntimeException.class, () -> missingPass.getServerSocket(0),
            "Should throw when path is provided but password is null");

        SecurityConfig missingPath = new SecurityConfig(null, "some-pass");
        assertThrows(RuntimeException.class, () -> missingPath.getServerSocket(0),
            "Should throw when password is provided but path is null");
    }

    @Test
    void testGetServerSocketWithInvalidPathThrows() {
        SecurityConfig config = new SecurityConfig("invalid/path/to/keystore.p12", "password");
        assertThrows(RuntimeException.class, () -> config.getServerSocket(0),
            "Should throw RuntimeException when keystore file is not found");
    }
}
