package in.yagnyam.persona.envelope;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Strict type checks shared by the envelope records when materializing a
 * wire value (protocol §6.1, fail-closed: malformed input is rejected, never
 * silently coerced).
 *
 * <p>Package-private: this is implementation support for
 * {@link Signature#fromValue(Map)}, {@link SignedEnvelope#fromValue(Map)} and
 * {@link MultiSignedEnvelope#fromValue(Map)}, not public API.
 */
final class EnvelopeValues {

  private EnvelopeValues() {}

  /**
   * Requires a text field value.
   *
   * @param container record name for error messages, e.g. {@code SignedEnvelope}
   * @param value the field value
   * @param field the field name, used in error messages
   * @return the string
   * @throws IllegalArgumentException if the value is not a {@code String}
   */
  static String requireText(String container, Object value, String field) {
    if (!(value instanceof String text)) {
      throw new IllegalArgumentException(
          container + " field '" + field + "' must be a String, found " + describe(value));
    }
    return text;
  }

  /**
   * Requires a byte-array field value.
   *
   * @param container record name for error messages
   * @param value the field value
   * @param field the field name, used in error messages
   * @return the bytes
   * @throws IllegalArgumentException if the value is not a {@code byte[]}
   */
  static byte[] requireBytes(String container, Object value, String field) {
    if (!(value instanceof byte[] bytes)) {
      throw new IllegalArgumentException(
          container + " field '" + field + "' must be bytes, found " + describe(value));
    }
    return bytes;
  }

  /**
   * Requires a list field value.
   *
   * @param container record name for error messages
   * @param value the field value
   * @param field the field name, used in error messages
   * @return the list
   * @throws IllegalArgumentException if the value is not a {@code List}
   */
  static List<?> requireList(String container, Object value, String field) {
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException(
          container + " field '" + field + "' must be an array, found " + describe(value));
    }
    return list;
  }

  /**
   * Requires a record-valued field value.
   *
   * @param container record name for error messages
   * @param value the field value
   * @param field the field name, used in error messages
   * @return the map
   * @throws IllegalArgumentException if the value is not a {@code Map}
   */
  static Map<String, Object> requireMap(String container, Object value, String field) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException(
          container + " field '" + field + "' must be a record, found " + describe(value));
    }
    Map<String, Object> entries = new java.util.LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new IllegalArgumentException(
            container + " field '" + field + "' has a non-string key: " + entry.getKey());
      }
      entries.put(key, entry.getValue());
    }
    return entries;
  }

  /**
   * Rejects a value carrying fields the record does not define (fail
   * closed: a hostile value with extra keys must not silently pass).
   *
   * @param container record name for error messages
   * @param value the record value
   * @param fieldNames the declared field names of the record
   * @throws IllegalArgumentException if the value has a key outside
   *     {@code fieldNames}
   */
  static void rejectUnknownFields(String container, Map<String, Object> value,
      Set<String> fieldNames) {
    for (String key : value.keySet()) {
      if (!fieldNames.contains(key)) {
        throw new IllegalArgumentException(
            container + " carries an unknown field '" + key + "'; the fields of protocol §6.1/§6.2"
                + " are " + fieldNames);
      }
    }
  }

  /**
   * Describes an offending value without echoing large or sensitive
   * material.
   */
  private static String describe(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof byte[] bytes) {
      return "byte[] of length " + bytes.length;
    }
    String text = value.getClass().getSimpleName();
    return text + " (" + shorten(String.valueOf(value)) + ")";
  }

  private static String shorten(String text) {
    return text.length() > 48 ? text.substring(0, 48) + "..." : text;
  }
}
