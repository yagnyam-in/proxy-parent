package in.yagnyam.persona.conformance;

/**
 * Raised when a vector file itself is malformed: a bad format marker, an
 * unknown {@code kind}, an entry whose keys do not belong to the file's kind,
 * malformed hex, or a duplicate vector id.
 *
 * <p>The runner treats this as a hard failure of the whole suite, never as a
 * skipped vector: a foreign implementation that cannot interpret a vector file
 * must not report the run as green.
 */
final class VectorFormatException extends RuntimeException {

  VectorFormatException(String message) {
    super(message);
  }

  VectorFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
