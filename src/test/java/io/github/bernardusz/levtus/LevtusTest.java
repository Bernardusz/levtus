package io.github.bernardusz.levtus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    Thread serverThread = Thread.ofVirtual().start(() -> app.listen(port));

    // Give the server a moment to bind to the socket
    Thread.sleep(500);

    try {
      URL url = new URI("http://localhost:" + port + "/test").toURL();
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
        (_, next) -> {
          middlewareHit.set(true);
          next.run();
        });

    app.get("/middleware", ctx -> ctx.text("ok"));

    int port = 9091;
    Thread serverThread = Thread.ofVirtual().start(() -> app.listen(port));

    Thread.sleep(500);

    try {
      URL url = new URI("http://localhost:" + port + "/middleware").toURL();
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      assertEquals(200, connection.getResponseCode());
      assertTrue(middlewareHit.get(), "Middleware should have been executed");
    } finally {
      serverThread.interrupt();
    }
  }

  @Test
  void testRouteSpecificMaxBodySizeIntegration() throws Exception {
    Levtus app = Levtus.create();
    app.maxBodySize(10); // global limit 10 bytes
    app.post("/large", ctx -> ctx.text("ok")).limit(100); // route limit 100 bytes
    app.post("/small", ctx -> ctx.text("ok")).limit(50);
    app.post("/default", ctx -> ctx.text("ok"));

    int port = 9091;
    Thread serverThread = Thread.ofVirtual().start(() -> app.listen(port));

    Thread.sleep(500);

    try {
      // Scenario 1: Sending 20 bytes to /large (Limit is 100) -> Should Pass (200 OK)
      URL urlLarge = new URI("http://localhost:" + port + "/large").toURL();
      HttpURLConnection connLarge = (HttpURLConnection) urlLarge.openConnection();
      connLarge.setRequestMethod("POST");
      connLarge.setDoOutput(true);

      byte[] body20Bytes = "12345678901234567890".getBytes(StandardCharsets.UTF_8);
      try (OutputStream os = connLarge.getOutputStream()) {
        os.write(body20Bytes);
      }
      assertEquals(
          200, connLarge.getResponseCode(), "Should allow 20 bytes on a 100-byte limit route");

      // Scenario 2: Sending 60 bytes to /small (Limit is 50) -> Should Fail (e.g., 413 Payload Too
      // Large)
      URL urlSmall = new URI("http://localhost:" + port + "/small").toURL();
      HttpURLConnection connSmall = (HttpURLConnection) urlSmall.openConnection();
      connSmall.setRequestMethod("POST");
      connSmall.setDoOutput(true);

      byte[] body60Bytes = "A".repeat(60).getBytes(StandardCharsets.UTF_8);
      try (OutputStream os = connSmall.getOutputStream()) {
        os.write(body60Bytes);
      }
      // Adjust the expected status code (413) based on what your Levtus framework returns
      assertEquals(
          413, connSmall.getResponseCode(), "Should block 60 bytes on a 50-byte limit route");

      // Scenario 3: Sending 15 bytes to /default (Fallback to global limit 10) -> Should Fail (413)
      URL urlDefault = new URI("http://localhost:" + port + "/default").toURL();
      HttpURLConnection connDefault = (HttpURLConnection) urlDefault.openConnection();
      connDefault.setRequestMethod("POST");
      connDefault.setDoOutput(true);

      byte[] body15Bytes = "123456789012345".getBytes(StandardCharsets.UTF_8);
      try (OutputStream os = connDefault.getOutputStream()) {
        os.write(body15Bytes);
      }
      assertEquals(
          413,
          connDefault.getResponseCode(),
          "Should block 15 bytes on a 10-byte global limit route");

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

  @Test
  void testFluentLimitBodySize() {
    Levtus app = Levtus.create();

    assertEquals(200, app.post("/", ctx -> ctx.text("ok")).limit(200).getMaxBodySize());
  }

  @Test
  void test100Continue() throws Exception {
    Levtus app = Levtus.create();
    app.post("/test", ctx -> ctx.text("ok")).limit(100);

    int port = 9093;
    Thread serverThread = Thread.ofVirtual().start(() -> app.listen(port));

    Thread.sleep(500);

    try (Socket socket = new Socket("localhost", port);
        OutputStream out = socket.getOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      // Test valid 100-continue
      String request =
          "POST /test HTTP/1.1\r\n"
              + "Host: localhost:"
              + port
              + "\r\n"
              + "Expect: 100-continue\r\n"
              + "Content-Length: 11\r\n"
              + "\r\n";

      out.write(request.getBytes(StandardCharsets.UTF_8));
      out.flush();

      // Read 100 Continue response
      String line = in.readLine();
      assertTrue(line.contains("100"), "Should receive 100 Continue");

      while (!in.readLine().isEmpty()) {
        // Skip headers
      }

      // Send body
      out.write("Hello World".getBytes(StandardCharsets.UTF_8));
      out.flush();

      // Read final response
      line = in.readLine();
      System.out.println("Actual response line: " + line); // Debug
      assertTrue(line.contains("200"), "Should receive 200 OK");
    } finally {
      serverThread.interrupt();
    }
  }
}
