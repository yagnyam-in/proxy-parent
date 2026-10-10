package in.yagnyam.persona.schema.codec;

/**
 * Signals that a payload value cannot be encoded: a missing or unknown field, a value that does not
 * match its field's type, or a value that this encoding cannot represent
 * ({@code docs/SCHEMA.md} §4.3, §3.1).
 *
 * <p>Extends {@link IllegalArgumentException} so callers may treat every encoding rejection as an
 * argument error.
 */
public class PayloadEncodingException extends IllegalArgumentException {

  /**
   * Creates an exception with the given message.
   *
   * @param message a message naming the offending field
   */
  public PayloadEncodingException(String message) {
    super(message);
  }

  /**
   * Creates an exception with the given message and cause.
   *
   * @param message a message naming the offending field
   * @param cause the underlying failure
   */
  public PayloadEncodingException(String message, Throwable cause) {
    super(message, cause);
  }
}
