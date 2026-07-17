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
  /** The transfer mode of the request. */
  private final boolean isChunked;
  /** The maximum chunk size to be read, per route enforced. */
  private final long maxChunkSize;
  /** The maximum number of chunks to be read, per route enforced. */
  private final long maxChunkCount;
  /** The number of bytes that has been read from the stream. */
  private long bytesRead = 0;
  /** The remaining bytes to be read in the current chunk. */
  private long chunkRemaining = 0;
  /** The flag that says whether we're at the end of  */
  private boolean isChunkEof = false;
  /** The number of chunks that has been read from the stream. */
  private long chunkCount = 0;

  /**
   * The constructor for the LevtusInputStream.
   *
   * @param inputStream the underlying input stream
   * @param maxBodySize the maximum allowed size of the body
   * @param contentLength the content length of the request
   * @param isChunked the transfer mode of the request
   * @param maxChunkSize the maximum size of a chunk
   * @param maxChunkCount  the maximum amount of chunk
   */
  public LevtusInputStream(InputStream inputStream, long maxBodySize, long contentLength, boolean isChunked, long maxChunkSize, long maxChunkCount) {
    this.inputStream = inputStream;
    this.maxBodySize = maxBodySize;
    this.contentLength = contentLength;
    this.isChunked = isChunked;
    this.maxChunkSize = maxChunkSize;
    this.maxChunkCount = maxChunkCount;

    if (!isChunked && contentLength > maxBodySize) {
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
   * <p>Adapts flawlessly to chunked method if needed</p>
   *
   * @return the byte read from the stream, or -1 if the end of the stream has been reached
   * @throws PayloadTooLargeException if the total bytes read exceeds {@code maxBodySize}
   */
  @Override
  public int read() {
    if (isChunked){
      if (chunkRemaining == 0) {
        try {
          parseNextChunkHeader();
        } catch (IOException e) {
          throw new LevtusIOException("An IO error occurred while reading from the stream (chunk header)", e);
        }
        if (isChunkEof) return -1;
      }

      try {
        int b = inputStream.read();
        if (b == -1) {
          throw new LevtusIOException("Unexpected EOF while parsing chunk stream data blocks", new IOException());
        }

        chunkRemaining--;
        bytesRead++;

        if (bytesRead > maxBodySize) {
          throw new PayloadTooLargeException("Payload too large");
        }

        if (chunkRemaining == 0) {
          readCRLF();
        }

        return b & 0xFF;
      } catch (IOException exception) {
        throw new LevtusIOException("An IO error occurred while reading from the stream (chunk data)", exception);
      }
    }

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
      throw new LevtusIOException("An IO error occurred while reading from the stream (normal read)", exception);
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
   * <p>Adapt flawlessly when handling chunked Request</p>
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
    if (isChunked){
      return readChunked(byteBuffer, offset, length);
    }

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
      throw new LevtusIOException("An IO error occurred while reading from the stream (normal buffer, offset, length read)", exception);
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
   * The helper method to read chunked from the incoming Request
   *
   * @param byteBuffer the buffer to be filled
   * @param offset the start of the cursor/pointer
   * @param length the length to be taken from the inputStream
   * @return the total number of bytes read from the inputStream
   * @throws LevtusIOException if an unexpected IO error occurs
   * @throws PayloadTooLargeException if the total size has exceeded the maxBodySize
   */
  int readChunked(byte[] byteBuffer, int offset, int length) throws LevtusIOException, PayloadTooLargeException {
    if (isChunkEof) {
      return -1;
    }

    try {
      if (chunkRemaining == 0){
        parseNextChunkHeader();
        if (isChunkEof) return -1;
      }

      int maxToRead = Math.min(length, (int) chunkRemaining);
      int newlyReadBytes = inputStream.read(byteBuffer, offset, maxToRead);
      if (newlyReadBytes == -1) {
        throw new IOException("Unexpected EOF while parsing chunk stream data blocks");
      }

      chunkRemaining -= newlyReadBytes;
      this.bytesRead += newlyReadBytes;

      if (this.bytesRead > maxBodySize){
        throw new PayloadTooLargeException("The total Payload is too large");
      }

      if (chunkRemaining == 0){
        readCRLF();
      }

      return newlyReadBytes;

    } catch (IOException e) {
      throw new LevtusIOException("An IO error occurred while reading from the stream (chunk buffer, offset, length error)", e);
    }
  }

  /**
   * The helper method to read the chunk header to grab the size of a chunk.
   *
   * @throws IOException if an unexpected IO error occurs
   * @throws PayloadTooLargeException if the chunk size is too large or chunk count is too much
   */
  void parseNextChunkHeader() throws IOException, PayloadTooLargeException {
    String sizeLine = readLine().trim();

    if (sizeLine.contains(";")) {
      sizeLine = sizeLine.split(";")[0].trim();
    }

    long chunkSize;
    try{
      chunkSize = Long.parseLong(sizeLine, 16);
    }
    catch (NumberFormatException e){
      throw new IOException("Malformed HTTP chunk size: " + sizeLine);
    }

    if (chunkSize > maxChunkSize) {
      throw new PayloadTooLargeException("The incoming chunk is too large");
    }

    if (chunkSize == 0){
      isChunkEof = true;
      readCRLF();
      return;
    }

    chunkCount++;
    if (chunkCount > maxChunkCount) {
      throw new PayloadTooLargeException("The incoming chunk count is too large");
    }

    chunkRemaining = chunkSize;
  }

  /**
   * The helper method to read the CRLF sequence.
   *
   * @throws IOException if an unexpected IO error occurs
   */
  void readCRLF() throws IOException {
    int r = inputStream.read();
    int n = inputStream.read();

    if (r != '\r' || n != '\n') {
      throw new IOException("Missing CRLF sequence");
    }
  }

  /**
   * The helper method to read the line until the first instance of \r\n
   *
   * @return the string read from inputStream
   * @throws IOException if an unexpected IO error occurs
   */
  String readLine() throws IOException {
    StringBuilder sb = new StringBuilder();
    int c;
    int lineSize = 0;
    while ((c = inputStream.read()) != -1){
      lineSize++;
      if (lineSize > maxChunkSize) {
        throw new PayloadTooLargeException("Payload too large");
      }

      if (c == '\r') {
       int next = inputStream.read();
       if (next == '\n') break;
       sb.append((char) c);
       if (next != -1) sb.append((char) next);
      } else {
        sb.append((char) c);
      }
    }
    return sb.toString();
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
