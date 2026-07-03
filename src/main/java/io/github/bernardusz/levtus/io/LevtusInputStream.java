package io.github.bernardusz.levtus.io;

import io.github.bernardusz.levtus.exception.developer.LevtusIOException;
import io.github.bernardusz.levtus.exception.http.PayloadTooLargeException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A custom InputStream that enforces the maximum size of the body to read data safely.
 * <br>
 * <p>{@code LevtusInputStream stream = new LevtusInputStream(inputStream, maxBodySize,
 * contentLength);}
 *
 * @author Bernardusz
 * @version 0.2.0
 */
public class LevtusInputStream extends InputStream {
  /** The inputStream made from socket's connection to be wrapped in LevtusInputStream. */
  private final InputStream inputStream;

  /** The maximum body size to be read, per route enforced. */
  private final long maxBodySize;

  /** The content length of the request. */
  private final long contentLength;

  /** The number of bytes that has been read from the stream. */
  private long bytesRead = 0;

  /**
   * The constructor for the LevtusInputStream.
   *
   * @param inputStream the underlying input stream
   * @param maxBodySize the maximum allowed size of the body
   * @param contentLength the content length of the request
   */
  public LevtusInputStream(InputStream inputStream, long maxBodySize, long contentLength) {
    this.inputStream = inputStream;
    this.maxBodySize = maxBodySize;
    this.contentLength = contentLength;

    if (contentLength > maxBodySize) {
      throw new PayloadTooLargeException("Content length exceeds max body size");
    }
  }

  /**
   * The method that reads a single byte from the stream.
   *
   * <p>If the content length is set and the number of bytes read is greater than or equal to the
   * content length, the method returns -1.
   *
   * <p>If the number of bytes read is greater than the maximum body size, a
   * PayloadTooLargeException is thrown.
   *
   * <p>Otherwise, the method reads a single byte from the stream and returns it.
   *
   * <pre>{@code
   * while(levtusInputStream.read() != -1) {
   *   // Do something
   * }
   *
   * }</pre>
   *
   * @return the byte read from the stream, or -1 if the end of the stream has been reached
   * @throws PayloadTooLargeException if the total bytes read exceeds {@code maxBodySize}
   */
  @Override
  public int read() {
    if (contentLength >= 0 && bytesRead >= contentLength) {
      return -1;
    }

    int b;
    try {
      b = inputStream.read();
      if (b != -1) {
        bytesRead++; // We track exactly what left the stream
        if (bytesRead > maxBodySize) {
          throw new PayloadTooLargeException("Payload too large");
        }
      }
    } catch (IOException exception) {
      throw new LevtusIOException("An IO error occurred while reading from the stream", exception);
    }
    return b;
  }

  /**
   * Reads data from this stream into a specified byte array buffer.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * int bytes;
   * byte[] buffer = new byte[4096];
   * while ((bytes = levtusInputStream.read(buffer, 0, buffer.length)) != -1) {
   * // Process 'bytes' number of data from buffer
   * }
   * }</pre>
   *
   * @param byteBuffer the buffer to fill with data
   * @param offset the start offset in the destination array {@code byteBuffer}
   * @param length the maximum number of bytes to read
   * @return the total number of bytes read into the buffer, or {@code -1} if the end of the
   *     stream/content-length has been reached.
   * @throws PayloadTooLargeException if the total bytes read exceeds {@code maxBodySize}
   *
   */
  @Override
  public int read(byte[] byteBuffer, int offset, int length) {
    if (contentLength >= 0 && this.bytesRead >= contentLength) {
      return -1;
    }

    long maxToRead = length;
    if (contentLength >= 0) {
      maxToRead = Math.min(length, contentLength - this.bytesRead);
    }
    if (maxToRead <= 0) {
      return -1;
    }

    int newlyReadBytes;
    try {
      newlyReadBytes = inputStream.read(byteBuffer, offset, (int) maxToRead);
      if (newlyReadBytes != -1) {
        // 1. Add to the running total (do not overwrite!)
        this.bytesRead += newlyReadBytes;

        // 2. Check the RUNNING TOTAL against maxBodySize, not the chunk size
        if (this.bytesRead > maxBodySize) {
          throw new PayloadTooLargeException("Payload too large");
        }
      }
    } catch (IOException exception) {
      throw new LevtusIOException("An IO error occurred while reading from the stream", exception);
    }
    return newlyReadBytes;
  }

  /**
   * Returns the number of bytes that have been read from the stream.
   *
   * @return the number of bytes read from the stream
   */
  public long getBytesRead() {
    return this.bytesRead;
  }

  /**
   * Intentional no-op method override.
   *
   * <p>Because the socket lifecycle is managed by {@link io.github.bernardusz.levtus.engine.LevtusEngine}.</p>
   */
  @Override
  public void close() {
    // Intentional no-op. We must NOT close the underlying socket InputStream
    // because the HTTP connection may be reused for keep-alive requests.
    // The socket lifecycle is managed by HttpConnectionHandler.
  }

}
