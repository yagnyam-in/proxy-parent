package in.yagnyam.persona.schema;

/**
 * Signals that a schema document violates the schema model of {@code docs/SCHEMA.md} §1.3 (or that
 * a schema node was constructed programmatically with an invalid identifier).
 *
 * <p>Extends {@link IllegalArgumentException} so that callers may treat every schema rejection —
 * parse failures and model invariant violations alike — uniformly as an argument error.
 */
public class SchemaValidationException extends IllegalArgumentException {

  /**
   * Creates an exception with the given message.
   *
   * @param message a message naming the offending element
   */
  public SchemaValidationException(String message) {
    super(message);
  }

  /**
   * Creates an exception with the given message and cause.
   *
   * @param message a message naming the offending element
   * @param cause the underlying failure
   */
  public SchemaValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
