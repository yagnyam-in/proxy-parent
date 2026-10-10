package in.yagnyam.persona.conformance;

/**
 * Raised when a vector file itself is malformed: a bad format marker, an
 * unknown {@code kind}, an entry whose keys do not belong to the file's kind,
 * malformed hex, or a duplicate vector id.
 *
 * <p>The runner treats this as a hard failure of the whole suite, never as a
 * skipped vector: a foreign implementation that cannot interpret a vector file
 * must not report the run as green.
 *
 * <p>This extends {@link AssertionError}, not {@code Exception}, by design:
 * reject vectors assert that the operation under test throws an exception, and
 * a format error must never satisfy such an assertion (a malformed vector whose
 * {@code input} has the wrong JSON type would otherwise pass as a vacuous
 * "rejection"). A vector file problem is a bug in the suite itself, so it
 * surfaces as a failed assertion.
 */
final class VectorFormatException extends AssertionError {

  VectorFormatException(String message) {
    super(message);
  }

  VectorFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
