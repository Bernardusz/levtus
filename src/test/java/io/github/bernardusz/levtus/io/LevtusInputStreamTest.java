package io.github.bernardusz.levtus.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.bernardusz.levtus.exception.http.PayloadTooLargeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LevtusInputStreamTest {
  private LevtusInputStream levtusInputStream;
  private int defaultMaxChunkSize = 64 * 1024;
  private int defaultMaxChunkCount = 1000;

  @Test
  void testBodyTooLarge(){
    assertThrows(PayloadTooLargeException.class, () -> new LevtusInputStream(
        new ByteArrayInputStream(new byte[1024]),
        1024,
        1025,
        false,
        defaultMaxChunkSize,
        defaultMaxChunkCount
    ));
    assertDoesNotThrow(() -> new LevtusInputStream(
        new ByteArrayInputStream(new byte[1024]),
        1024,
        1024,
        false,
        defaultMaxChunkSize,
        defaultMaxChunkCount
    ));
  }

  @Test
  void testReadSingular() throws IOException {
    byte[] buffer = "Hello! This is Levtus!".getBytes();
    levtusInputStream = new LevtusInputStream(
        new ByteArrayInputStream(buffer),
        1024,
        22,
        false,
        defaultMaxChunkSize,
        defaultMaxChunkCount
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
        22,
        false,
        defaultMaxChunkSize,
        defaultMaxChunkCount
    );


    byte[] readBuffer = new byte[1024];

    int bytesRead = levtusInputStream.read(readBuffer, 0, 22);

    assertEquals(bytesRead, 22);

    assertEquals("Hello! This is Levtus!", new String(readBuffer, 0, bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  void testChunked() throws IOException {
    String chunkedData = "6\r\nHello!\r\n0\r\n\r\n";
    ByteArrayInputStream data = new ByteArrayInputStream(chunkedData.getBytes());
    LevtusInputStream lis = new LevtusInputStream(
        data,
        8192,
        6,
        true,
        defaultMaxChunkSize,
        defaultMaxChunkCount
    );
    byte[] result = new byte[6];

    assertEquals(
        6,
        lis.read(result)
    );

    assertEquals(
        "Hello!",
        new String(result)
    );
  }

  @Test
  void testChunkedWithSize() throws IOException {
    String chunkedData = "6\r\nHello!\r\n0\r\n\r\n";
    ByteArrayInputStream data = new ByteArrayInputStream(chunkedData.getBytes());
    LevtusInputStream lis = new LevtusInputStream(
        data,
        8192,
        6,
        true,
        defaultMaxChunkSize,
        defaultMaxChunkCount
    );
    byte[] result = new byte[6];

    int bytesRead = lis.read(result, 0, 5);
    assertEquals(5, bytesRead);

    assertEquals(
        "Hello",
        new String(result, 0, bytesRead, StandardCharsets.UTF_8)
    );
  }

  @Test
  void testChunkedTooBig() throws IOException {
    String chunkedData = "6\r\nHello!\r\n0\r\n\r\n";
    ByteArrayInputStream data = new ByteArrayInputStream(chunkedData.getBytes());
    LevtusInputStream lis = new LevtusInputStream(
        data,
        8192,
        6,
        true,
        5,  // maxChunkSize = 5, but chunk size is 6
        defaultMaxChunkCount
    );
    byte[] result = new byte[6];

    assertThrows(
        PayloadTooLargeException.class,
        () -> lis.read(result)
    );
  }

  @Test
  void testChunkedTooManyChunk() throws IOException {
    String chunkedData = "2\r\nHe\r\n2\r\nll\r\n1\r\no!\r\n0\r\n\r\n";
    ByteArrayInputStream data = new ByteArrayInputStream(chunkedData.getBytes());
    LevtusInputStream lis = new LevtusInputStream(
        data,
        8192,
        5,
        true,
        defaultMaxChunkSize,
        2  // Allow only 2 chunks, but data has 3
    );
    byte[] result = new byte[6];

    // Read in a loop to consume chunks and trigger the chunk count check
    assertThrows(
        PayloadTooLargeException.class,
        () -> {
          int totalRead = 0;
          while (totalRead < 5) {
            int read = lis.read(result, totalRead, 5 - totalRead);
            if (read == -1) break;
            totalRead += read;
          }
        }
    );
  }
}
