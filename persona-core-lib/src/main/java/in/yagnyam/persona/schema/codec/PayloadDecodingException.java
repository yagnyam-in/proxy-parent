package in.yagnyam.persona.schema.codec;

/**
 * Signals that a payload could not be decoded: trailing bytes, truncated input, an enum index
 * outside the declared symbols, a negative block count, invalid UTF-8
 * ({@code docs/SCHEMA.md} §3.3), or a value that does not match the schema.
 *
 * <p>Extends {@link IllegalArgumentException} so callers may treat every decoding rejection as an
 * argument error.
 */
public class PayloadDecodingException extends IllegalArgumentException {

  /**
   * Creates an exception with the given message.
   *
   * @param message a message naming the offending field or byte range
   */
  public PayloadDecodingException(String message) {
    super(message);
  }

  /**
   * Creates an exception with the given message and cause.
   *
   * @param message a message naming the offending field or byte range
   * @param cause the underlying failure
   */
  public PayloadDecodingException(String message, Throwable cause) {
    super(message, cause);
  }
}
