package in.yagnyam.persona.schema.codec;

import in.yagnyam.persona.schema.CanonicalJson;
import in.yagnyam.persona.schema.Field;
import in.yagnyam.persona.schema.RecordType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The encoder-side value model shared by the payload codecs ({@code docs/SCHEMA.md} §4.2, §4.3).
 *
 * <p>Every check produces a {@link PayloadEncodingException} naming the field path. The accepted
 * Java representations are exactly those of §4.2: {@code Integer} for {@code int}, {@code Long} or
 * a losslessly widened {@code Integer} for {@code long}, {@code Double} for {@code double},
 * {@code byte[]} for {@code bytes}, {@link Instant} for {@code timestamp-millis}, the symbol
 * {@code String} for enums, a {@code List} for arrays and a {@code Map<String,Object>} for maps,
 * records and envelopes.
 */
final class PayloadValues {

  private PayloadValues() {
  }

  /**
   * Names a payload location for error messages.
   *
   * @param path the dotted field path, empty for the root record
   * @return a human-readable location
   */
  static String at(String path) {
    return path.isEmpty() ? "the root record" : "field '" + path + "'";
  }

  /**
   * Extends a dotted field path.
   *
   * @param path the parent path
   * @param name the field name
   * @return the child path
   */
  static String childPath(String path, String name) {
    return path.isEmpty() ? name : path + "." + name;
  }

  /**
   * Describes a value's Java type for error messages.
   *
   * @param value the value, possibly {@code null}
   * @return a short description
   */
  static String describe(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof byte[] bytes) {
      return "byte[] of length " + bytes.length;
    }
    return value.getClass().getSimpleName();
  }

  /**
   * Builds a type mismatch error.
   *
   * @param path the field path
   * @param expected the expected representation
   * @param value the offending value
   * @return the exception to throw
   */
  static PayloadEncodingException mismatch(String path, String expected, Object value) {
    return new PayloadEncodingException(
        at(path) + ": expected " + expected + " but found " + describe(value));
  }

  /**
   * Requires a well-formed string.
   *
   * @param value the value
   * @param path the field path
   * @return the string
   * @throws PayloadEncodingException if the value is not a well-formed {@code String}
   */
  static String requireText(Object value, String path) {
    if (!(value instanceof String text)) {
      throw mismatch(path, "a String", value);
    }
    if (!CanonicalJson.isWellFormed(text)) {
      throw new PayloadEncodingException(
          at(path)
              + ": String contains an unpaired surrogate and cannot be encoded as UTF-8"
              + " (SCHEMA.md §2.1 rule 4)");
    }
    return text;
  }

  /**
   * Requires a boolean.
   *
   * @param value the value
   * @param path the field path
   * @return the boolean
   * @throws PayloadEncodingException if the value is not a {@code Boolean}
   */
  static boolean requireBoolean(Object value, String path) {
    if (!(value instanceof Boolean bool)) {
      throw mismatch(path, "a Boolean", value);
    }
    return bool;
  }

  /**
   * Requires an {@code int} value (§4.3: {@code int} fields are strict about {@code Integer}).
   *
   * @param value the value
   * @param path the field path
   * @return the int value
   * @throws PayloadEncodingException if the value is not an {@code Integer}
   */
  static int requireInt(Object value, String path) {
    if (!(value instanceof Integer number)) {
      throw mismatch(path, "an Integer (int fields accept Integer only, SCHEMA.md §4.3)", value);
    }
    return number;
  }

  /**
   * Requires a {@code long} value (§4.3: {@code Long}, or an {@code Integer} widened without
   * loss).
   *
   * @param value the value
   * @param path the field path
   * @return the long value
   * @throws PayloadEncodingException if the value is neither a {@code Long} nor an {@code Integer}
   */
  static long requireLong(Object value, String path) {
    if (value instanceof Long number) {
      return number;
    }
    if (value instanceof Integer number) {
      return number;
    }
    throw mismatch(path, "a Long or Integer (SCHEMA.md §4.3)", value);
  }

  /**
   * Requires a double (§4.4). Non-finite values are rejected per §4.3.
   *
   * @param value the value
   * @param path the field path
   * @return the double value
   * @throws PayloadEncodingException if the value is not a finite {@code Double}
   */
  static double requireDouble(Object value, String path) {
    if (!(value instanceof Double number)) {
      throw mismatch(path, "a Double", value);
    }
    if (!Double.isFinite(number)) {
      throw new PayloadEncodingException(
          at(path) + ": non-finite double values cannot be encoded (SCHEMA.md §4.3): " + number);
    }
    return number;
  }

  /**
   * Requires a byte array.
   *
   * @param value the value
   * @param path the field path
   * @return the byte array
   * @throws PayloadEncodingException if the value is not a {@code byte[]}
   */
  static byte[] requireBytes(Object value, String path) {
    if (!(value instanceof byte[] bytes)) {
      throw mismatch(path, "a byte[]", value);
    }
    return bytes;
  }

  /**
   * Requires an instant with millisecond precision (§1.2: {@code timestamp-millis} is an instant
   * on the UTC timeline, millisecond precision).
   *
   * @param value the value
   * @param path the field path
   * @return the instant
   * @throws PayloadEncodingException if the value is not an {@link Instant} or carries sub-
   *     millisecond precision that neither encoding can represent
   */
  static Instant requireInstant(Object value, String path) {
    if (!(value instanceof Instant instant)) {
      throw mismatch(path, "an Instant", value);
    }
    if (instant.getNano() % 1_000_000 != 0) {
      throw new PayloadEncodingException(
          at(path) + ": timestamp-millis values must have millisecond precision, found " + instant);
    }
    return instant;
  }

  /**
   * Requires a list.
   *
   * @param value the value
   * @param path the field path
   * @return the list
   * @throws PayloadEncodingException if the value is not a {@code List}
   */
  static List<?> requireList(Object value, String path) {
    if (!(value instanceof List<?> list)) {
      throw mismatch(path, "a List", value);
    }
    return list;
  }

  /**
   * Requires a map with string keys.
   *
   * @param value the value
   * @param path the field path
   * @return a copy of the map keyed by name
   * @throws PayloadEncodingException if the value is not a {@code Map} or has a non-string key
   */
  static Map<String, Object> requireMap(Object value, String path) {
    if (!(value instanceof Map<?, ?> map)) {
      throw mismatch(path, "a Map", value);
    }
    Map<String, Object> entries = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new PayloadEncodingException(
            at(path) + ": map keys must be strings, found " + describe(entry.getKey()));
      }
      entries.put(key, entry.getValue());
    }
    return entries;
  }

  /**
   * Rejects value keys that are not fields of the record (§4.3: unknown fields are rejected).
   *
   * @param record the record type
   * @param value the value
   * @param path the field path of the record itself
   * @throws PayloadEncodingException if the value has a key that is not a declared field
   */
  static void requireKnownFields(RecordType record, Map<String, Object> value, String path) {
    for (String key : value.keySet()) {
      if (!hasField(record, key)) {
        throw new PayloadEncodingException(
            at(path)
                + " (record '"
                + record.name()
                + "'): unknown field '"
                + key
                + "'; declared fields are "
                + record.fields().stream().map(Field::name).toList());
      }
    }
  }

  /**
   * Requires a declared field to be present and non-null.
   *
   * @param record the record type
   * @param value the value
   * @param field the field
   * @param path the field path of the record itself
   * @return the field's value
   * @throws PayloadEncodingException if the field is missing or {@code null}
   */
  static Object requireField(
      RecordType record, Map<String, Object> value, Field field, String path) {
    if (!value.containsKey(field.name())) {
      throw new PayloadEncodingException(
          at(path)
              + " (record '"
              + record.name()
              + "'): missing field '"
              + field.name()
              + "'; all fields are required (SCHEMA.md §1.3 rule 5)");
    }
    Object fieldValue = value.get(field.name());
    if (fieldValue == null) {
      throw new PayloadEncodingException(
          at(childPath(path, field.name())) + ": value must not be null");
    }
    return fieldValue;
  }

  /**
   * Tests whether a record declares a field.
   *
   * @param record the record type
   * @param name the field name
   * @return {@code true} if the field is declared
   */
  static boolean hasField(RecordType record, String name) {
    return record.fields().stream().anyMatch(field -> field.name().equals(name));
  }
}
