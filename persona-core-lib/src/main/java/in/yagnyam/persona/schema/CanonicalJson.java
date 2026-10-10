package in.yagnyam.persona.schema;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The hand-rolled canonical JSON writer defined by {@code docs/SCHEMA.md} §2.1. It is used for
 * both schema canonicalization (§2) and payload canonical JSON (§4).
 *
 * <p>Rules implemented here:
 *
 * <ol>
 *   <li>no whitespace around {@code :}, {@code ,}, <code>{</code>, <code>}</code>, {@code [},
 *       {@code ]} (this class writes the separators; callers concatenate values directly);
 *   <li>object keys sorted by the UTF-8 byte sequence of the key
 *       ({@link #UTF8_ORDER}, {@link #writeSortedObject});
 *   <li>arrays in semantic order (callers join values with {@code ,});
 *   <li>string escaping limited to the quote and backslash escapes, the five named control escapes
 *       (backspace, tab, newline, form feed, carriage return), and lowercase
 *       <code>&#92;u00xx</code> escapes for the remaining control characters; every other
 *       character is emitted as raw UTF-8 ({@link #writeString});
 *   <li>numbers rendered as decimal digits, doubles per §4.4 ({@link #doubleLiteral}).
 * </ol>
 *
 * <p>The escaping and the key order are deliberately hand-written rather than delegated to Jackson:
 * the digest in §2 is a cross-language contract and the writer must be byte-exact.
 */
public final class CanonicalJson {

  /**
   * Orders strings by the unsigned byte sequence of their UTF-8 encoding (§2.1 rule 2).
   *
   * <p>Only well-formed strings (no unpaired surrogates, see {@link #isWellFormed}) are ordered
   * meaningfully; for well-formed strings this order coincides with the order of the UTF-8 byte
   * sequences of the code points.
   */
  public static final Comparator<String> UTF8_ORDER =
      (left, right) -> {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        int shared = Math.min(a.length, b.length);
        for (int i = 0; i < shared; i++) {
          int difference = (a[i] & 0xff) - (b[i] & 0xff);
          if (difference != 0) {
            return difference;
          }
        }
        return a.length - b.length;
      };

  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private CanonicalJson() {
  }

  /**
   * Appends a double in the shortest round-tripping decimal form of §4.4, which is what Java 19+
   * {@link Double#toString(double)} produces.
   *
   * @param value the value to render
   * @return the decimal literal, e.g. {@code "1.5"} or {@code "1.0E308"}
   * @throws IllegalArgumentException if {@code value} is NaN or infinite (§4.3: non-finite doubles
   *     are rejected)
   */
  public static String doubleLiteral(double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(
          "non-finite double values cannot be encoded (SCHEMA.md §4.3): " + value);
    }
    return Double.toString(value);
  }

  /**
   * Tests whether a string can be encoded as UTF-8 without loss, i.e. it contains no unpaired
   * surrogate (§2.1 rule 4 emits all non-ASCII characters as raw UTF-8, which requires well-formed
   * UTF-16 input).
   *
   * @param value the string
   * @return {@code true} if every surrogate in {@code value} is properly paired
   */
  public static boolean isWellFormed(String value) {
    Objects.requireNonNull(value, "value");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isHighSurrogate(c)) {
        if (i + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(i + 1))) {
          return false;
        }
        i++;
      } else if (Character.isLowSurrogate(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Encodes a string as well-formed UTF-8 bytes.
   *
   * @param value the string
   * @return its UTF-8 encoding
   * @throws IllegalArgumentException if {@code value} is not {@link #isWellFormed well-formed}
   */
  public static byte[] utf8Bytes(String value) {
    if (!isWellFormed(value)) {
      throw new IllegalArgumentException("string is not well-formed UTF-16 (unpaired surrogate)");
    }
    return value.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Appends a JSON string literal per §2.1 rule 4: quotes, backslash, the five named controls and
   * lowercase <code>&#92;u00xx</code> for any other control character; all other characters are
   * appended raw (the caller encodes the whole document as UTF-8).
   *
   * @param out the destination
   * @param value the string
   * @throws IllegalArgumentException if {@code value} is not {@link #isWellFormed well-formed}
   */
  public static void writeString(StringBuilder out, String value) {
    if (!isWellFormed(value)) {
      throw new IllegalArgumentException("string is not well-formed UTF-16 (unpaired surrogate)");
    }
    out.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\b' -> out.append("\\b");
        case '\t' -> out.append("\\t");
        case '\n' -> out.append("\\n");
        case '\f' -> out.append("\\f");
        case '\r' -> out.append("\\r");
        default -> {
          if (c < 0x20) {
            out.append("\\u00").append(HEX[c >> 4]).append(HEX[c & 0x0f]);
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
  }

  /**
   * Appends an object whose values are already rendered as canonical JSON, with keys sorted per
   * §2.1 rule 2 and no whitespace (§2.1 rule 1).
   *
   * @param out the destination
   * @param renderedValues map from key to the canonical JSON text of the value
   */
  public static void writeSortedObject(StringBuilder out, Map<String, String> renderedValues) {
    TreeMap<String, String> sorted = new TreeMap<>(UTF8_ORDER);
    sorted.putAll(renderedValues);
    out.append('{');
    boolean first = true;
    for (Map.Entry<String, String> entry : sorted.entrySet()) {
      if (!first) {
        out.append(',');
      }
      first = false;
      writeString(out, entry.getKey());
      out.append(':').append(entry.getValue());
    }
    out.append('}');
  }
}
