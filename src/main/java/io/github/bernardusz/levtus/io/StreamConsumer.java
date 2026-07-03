package io.github.bernardusz.levtus.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A functional interface for safely consuming an input stream with automatic resource management.
 *
 * <p>This interface is designed to work with {@link
 * io.github.bernardusz.levtus.http.Request#bodyStream(StreamConsumer)} to provide a safe, one-time
 * consumption pattern for request body streams. When used with {@code bodyStream(StreamConsumer)},
 * the stream is automatically closed via try-with-resources, and any {@link IOException} is wrapped
 * in a {@link io.github.bernardusz.levtus.exception.developer.LevtusIOException}.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * ctx.bodyStream(stream -> {
 *     byte[] buffer = new byte[4096];
 *     int bytesRead;
 *     while ((bytesRead = stream.read(buffer)) != -1) {
 *         // Process the chunk
 *         processChunk(buffer, bytesRead);
 *     }
 * });
 * }</pre>
 *
 * <p><b>Benefits:</b>
 *
 * <ul>
 *   <li>Automatic stream closure - no need for try-with-resources or finally blocks
 *   <li>Unified exception handling - IOExceptions are wrapped in LevtusIOException
 *   <li>Enforces single-use semantics - prevents accidental multiple stream consumption
 *   <li>Functional style - enables clean lambda expressions
 * </ul>
 *
 * <p><b>When to use:</b> Use this interface when you want to process the request body in a
 * streaming fashion without loading the entire body into memory. This is ideal for large payloads,
 * file uploads, or when you want to process data incrementally.
 *
 * <p><b>Alternatives:</b> If you need the entire body in memory, use {@link
 * io.github.bernardusz.levtus.http.Request#body()} or {@link
 * io.github.bernardusz.levtus.http.Request#bodyAsString()} instead.
 */
@FunctionalInterface
public interface StreamConsumer {
  /**
   * Consumes the given input stream.
   *
   * <p>The stream provided is a {@link io.github.bernardusz.levtus.io.LevtusInputStream} which
   * enforces maximum body size limits to prevent memory exhaustion attacks.
   *
   * <p><b>Important:</b> Do not close the stream manually - it will be closed automatically by the
   * framework after this method completes.
   *
   * @param is the input stream to consume (specifically a LevtusInputStream)
   * @throws IOException if an I/O error occurs during processing
   */
  void consume(InputStream is) throws IOException;
}
