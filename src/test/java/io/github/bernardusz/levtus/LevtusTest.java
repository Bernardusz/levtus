package io.github.bernardusz.levtus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LevtusTest {

  @Test
  void testCreate() {
    Levtus app = Levtus.create();
    assertNotNull(app);
  }

  /**
   * Integration test verifying the full stack: Levtus -> Router -> LevtusEngine ->
   * HttpConnectionHandler -> Response
   */
  @Test
  void testBasicIntegration() throws Exception {
    Levtus app = Levtus.create();
    AtomicReference<String> receivedValue = new AtomicReference<>();

    app.get(
        "/test",
        ctx -> {
          receivedValue.set("hit");
          ctx.text("ok");
        });

    // Start server in a virtual thread.
    // Note: Using a fixed port for this simple integration test.
    int port = 9090;
    Thread serverThread =
        Thread.ofVirtual()
            .start(
                () -> {
                  app.listen(port);
                });

    // Give the server a moment to bind to the socket
    Thread.sleep(500);

    try {
      URL url = new URL("http://localhost:" + port + "/test");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      int responseCode = connection.getResponseCode();
      assertEquals(200, responseCode);

      try (BufferedReader in =
          new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        String responseBody = in.readLine();
        assertEquals("ok", responseBody);
      }

      assertEquals("hit", receivedValue.get());
    } finally {
      serverThread.interrupt();
    }
  }

  @Test
  void testMiddlewareIntegration() throws Exception {
    Levtus app = Levtus.create();
    AtomicBoolean middlewareHit = new AtomicBoolean(false);

    app.use(
        (ctx, next) -> {
          middlewareHit.set(true);
          next.run();
        });

    app.get("/middleware", ctx -> ctx.text("ok"));

    int port = 9091;
    Thread serverThread =
        Thread.ofVirtual()
            .start(
                () -> {
                  app.listen(port);
                });

    Thread.sleep(500);

    try {
      URL url = new URL("http://localhost:" + port + "/middleware");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      assertEquals(200, connection.getResponseCode());
      assertTrue(middlewareHit.get(), "Middleware should have been executed");
    } finally {
      serverThread.interrupt();
    }
  }

  @Test
  void testFluentConfiguration() {
    Levtus app = Levtus.create();
    // Verify that configuration methods can be called without exceptions
    app.maxConcurrentConnections(50)
        .maxBodySize(1024 * 1024)
        .maxHeaderCount(50)
        .maxHeaderSize(4096)
        .maxLineSize(4096)
        .maxEmptyLines(3)
        .staticFiles("public");
  }
}
