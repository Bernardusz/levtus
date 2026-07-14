package io.github.bernardusz.levtus.http;

import java.nio.file.Path;

/**
 * The transfer mode of either Levtus as a recipient or sender.
 *
 * <p>{@link TransferMode#NORMAL} will be bulk response, sending everything at once via {@link
 * Response#sendFile(Path) and {@link Response#send(byte[])}}
 *
 * <p>{@link TransferMode#CHUNKED} will be chunked response, sending in chunks via {@link
 * Response#sendChunk(byte[])}
 */
public enum TransferMode {
  NORMAL,
  CHUNKED,
  DEFAULT
}
