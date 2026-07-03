package io.github.bernardusz.levtus.io;

import io.github.bernardusz.levtus.exception.http.PayloadTooLargeException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

class LevtusInputStreamTest {
  private LevtusInputStream levtusInputStream;

  @Test
  void testBodyTooLarge(){
    assertThrows(PayloadTooLargeException.class, () -> new LevtusInputStream(
        new ByteArrayInputStream(new byte[1024]),
        1024,
        1025
    ));
    assertDoesNotThrow(() -> new LevtusInputStream(
        new ByteArrayInputStream(new byte[1024]),
        1024,
        1024
    ));
  }

  @Test
  void testReadSingular() throws IOException {
    byte[] buffer = "Hello! This is Levtus!".getBytes();
    levtusInputStream = new LevtusInputStream(
        new ByteArrayInputStream(buffer),
        1024,
        22
    );
    StringBuilder sb = new StringBuilder();

    int bytesRead;
    while ((bytesRead = levtusInputStream.read()) != -1) {
      // Convert only the chunk we actually read into a string and append it
      sb.append((char) bytesRead);
    }

    assertEquals("Hello! This is Levtus!", sb.toString());
  }

  @Test
  void testReadIntoBuffer() throws IOException {
    byte[] buffer = "Hello! This is Levtus!".getBytes(); // Read up to 1024 bytes at a time
    levtusInputStream = new LevtusInputStream(
        new ByteArrayInputStream(buffer),
        1024,
        22
    );


    byte[] readBuffer = new byte[1024];

    int bytesRead = levtusInputStream.read(readBuffer, 0, 22);

    assertEquals(bytesRead, 22);

    assertEquals("Hello! This is Levtus!", new String(readBuffer, 0, bytesRead, StandardCharsets.UTF_8));
  }
}
