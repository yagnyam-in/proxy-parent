package in.yagnyam.persona.conformance;

import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import in.yagnyam.persona.schema.ArrayType;
import in.yagnyam.persona.schema.EnvelopeType;
import in.yagnyam.persona.schema.EnumType;
import in.yagnyam.persona.schema.Field;
import in.yagnyam.persona.schema.MapType;
import in.yagnyam.persona.schema.PrimitiveType;
import in.yagnyam.persona.schema.RecordType;
import in.yagnyam.persona.schema.TypeNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Translates the JSON encoding of a value in a vector file (see
 * {@code vectors/README.md}) into the Java representations the
 * {@code persona-core-lib} codecs accept (§4.2 of {@code docs/SCHEMA.md}:
 * {@code Integer} for {@code int}, {@code Long} for {@code long},
 * {@code Double} for {@code double}, {@code byte[]} for {@code bytes},
 * {@link Instant} for {@code timestamp-millis}, {@code String} for strings and
 * enum symbols, {@code List} for arrays, {@code Map} for maps, records and
 * envelopes), and compares decoded values.
 *
 * <p>The conversion is schema-driven where the vector encoding is type-specific
 * (timestamps, {@code bytes} base64, the {@code @epoch-millis} and non-finite
 * double tokens) and raw otherwise, so that a vector feeding the wrong JSON type
 * to a field reaches the codec as the wrong Java type and is rejected <em>by the
 * codec</em>, not by this translation.
 */
final class VectorValues {

  /** The prefix of the epoch-milliseconds extension token. */
  private static final String EPOCH_MILLIS_TOKEN = "@epoch-millis:";

  /** The non-finite double tokens a JSON number cannot spell. */
  private static final Set<String> DOUBLE_TOKENS = Set.of("NaN", "Infinity", "-Infinity");

  private VectorValues() {}

  /**
   * Converts a vector-encoded value to the codec's Java value model.
   *
   * @param type the schema node type of the value
   * @param node the vector's JSON encoding of the value
   * @return the Java value the codec accepts
   */
  static Object toCodecValue(TypeNode type, JsonNode node) {
    if (node == null) {
      throw new VectorFormatException("missing value for a " + type.getClass().getSimpleName());
    }
    return switch (type) {
      case PrimitiveType primitive -> primitive(primitive, node);
      case EnumType enumType -> enumSymbol(enumType, node);
      case ArrayType arrayType -> array(arrayType, node);
      case MapType mapType -> map(mapType, node);
      case RecordType recordType -> record(recordType, node);
      case EnvelopeType envelopeType -> record(envelopeType.record(), node);
      default -> throw new VectorFormatException(
          "unknown schema node type " + type.getClass().getName());
    };
  }

  /** Converts a record-typed value; the root of every payload. */
  @SuppressWarnings("unchecked")
  static Map<String, Object> toRecordValue(RecordType record, JsonNode node) {
    Object value = toCodecValue(record, node);
    if (!(value instanceof Map<?, ?> map)) {
      throw new VectorFormatException(
          "value must be a JSON object describing record '" + record.name() + "', found " + node.getNodeType());
    }
    return (Map<String, Object>) map;
  }

  /** Converts a primitive-typed value (§4.2). */
  private static Object primitive(PrimitiveType primitive, JsonNode node) {
    return switch (primitive.name()) {
      case PrimitiveType.STRING -> node.isTextual() ? node.textValue() : raw(node);
      case PrimitiveType.BOOLEAN -> node.isBoolean() ? node.booleanValue() : raw(node);
      case PrimitiveType.INT -> node.isIntegralNumber() && node.canConvertToInt()
          ? node.intValue()
          : raw(node);
      case PrimitiveType.LONG -> node.isIntegralNumber() && node.canConvertToLong()
          ? node.longValue()
          : raw(node);
      case PrimitiveType.DOUBLE -> doubleValue(node);
      case PrimitiveType.BYTES -> bytesValue(node);
      case PrimitiveType.TIMESTAMP_MILLIS -> timestampValue(node);
      default -> throw new VectorFormatException(
          "unknown primitive type " + primitive.name() + " (SCHEMA.md §1.2)");
    };
  }

  /** Converts a double-typed value, including the non-finite tokens. */
  private static Object doubleValue(JsonNode node) {
    if (node.isNumber()) {
      return node.doubleValue();
    }
    if (node.isTextual() && DOUBLE_TOKENS.contains(node.textValue())) {
      return switch (node.textValue()) {
        case "NaN" -> Double.NaN;
        case "Infinity" -> Double.POSITIVE_INFINITY;
        default -> Double.NEGATIVE_INFINITY;
      };
    }
    return raw(node);
  }

  /** Converts a bytes-typed value: the vector carries standard base64. */
  private static Object bytesValue(JsonNode node) {
    if (!node.isTextual()) {
      return raw(node);
    }
    try {
      return Base64.getDecoder().decode(node.textValue());
    } catch (IllegalArgumentException e) {
      // Not base64: hand the string through so the codec rejects it for the
      // right reason (expected a byte[], found a String).
      return node.textValue();
    }
  }

  /** Converts a timestamp-typed value: ISO-8601 text, the epoch token, or null. */
  private static Object timestampValue(JsonNode node) {
    if (node.isNull()) {
      return null;
    }
    if (!node.isTextual()) {
      return raw(node);
    }
    Instant instant = parseInstant(node.textValue());
    return instant == null ? node.textValue() : instant;
  }

  /** Parses an instant from the vector encodings of {@code timestamp-millis}. */
  private static Instant parseInstant(String text) {
    if (text.startsWith(EPOCH_MILLIS_TOKEN)) {
      try {
        return Instant.ofEpochMilli(Long.parseLong(text.substring(EPOCH_MILLIS_TOKEN.length())));
      } catch (NumberFormatException e) {
        return null;
      }
    }
    try {
      return Instant.parse(text);
    } catch (DateTimeParseException notAnInstant) {
      try {
        return OffsetDateTime.parse(text).toInstant();
      } catch (DateTimeParseException notAnOffsetDateTime) {
        return null;
      }
    }
  }

  /** Converts an enum-typed value: the declared symbol as a string. */
  private static Object enumSymbol(EnumType enumType, JsonNode node) {
    return node.isTextual() ? node.textValue() : raw(node);
  }

  /** Converts an array-typed value. */
  private static Object array(ArrayType arrayType, JsonNode node) {
    if (!node.isArray()) {
      return raw(node);
    }
    List<Object> items = new ArrayList<>(node.size());
    for (JsonNode item : node) {
      items.add(toCodecValue(arrayType.items(), item));
    }
    return items;
  }

  /** Converts a map-typed value. */
  private static Object map(MapType mapType, JsonNode node) {
    if (!node.isObject()) {
      return raw(node);
    }
    Map<String, Object> entries = new LinkedHashMap<>();
    node.fields().forEachRemaining(entry -> entries.put(entry.getKey(), toCodecValue(mapType.values(), entry.getValue())));
    return entries;
  }

  /**
   * Converts a record-typed value. Every declared field present in the JSON is
   * converted through its field type, keys that are not declared fields are
   * preserved raw (so the codec rejects them), and absent or {@code null}
   * fields stay absent or {@code null} (so the codec rejects them).
   */
  private static Object record(RecordType record, JsonNode node) {
    if (!node.isObject()) {
      return raw(node);
    }
    Map<String, Object> value = new LinkedHashMap<>();
    for (Field field : record.fields()) {
      JsonNode fieldNode = node.get(field.name());
      if (fieldNode != null) {
        value.put(field.name(), toCodecValue(field.type(), fieldNode));
      }
    }
    Set<String> declared = Set.copyOf(record.fields().stream().map(Field::name).toList());
    node.fields().forEachRemaining(entry -> {
      if (!declared.contains(entry.getKey())) {
        value.put(entry.getKey(), raw(entry.getValue()));
      }
    });
    return value;
  }

  /** Converts a JSON node to the closest Java object, with no schema knowledge. */
  private static Object raw(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.textValue();
    }
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    if (node.isIntegralNumber()) {
      // Preserve out-of-long-range values as BigInteger so codec range/type
      // checks reject the original value instead of a wrapped long.
      return node.canConvertToInt()
          ? node.intValue()
          : node.canConvertToLong() ? node.longValue() : node.bigIntegerValue();
    }
    if (node.isNumber()) {
      return node.doubleValue();
    }
    if (node.isArray()) {
      List<Object> items = new ArrayList<>(node.size());
      node.forEach(item -> items.add(raw(item)));
      return items;
    }
    if (node.isObject()) {
      Map<String, Object> entries = new LinkedHashMap<>();
      node.fields().forEachRemaining(entry -> entries.put(entry.getKey(), raw(entry.getValue())));
      return entries;
    }
    return node.toString();
  }

  /**
   * Compares a decoded value against the converted golden, exactly.
   *
   * @param expected the golden converted with {@link #toCodecValue}
   * @param actual the value the codec produced
   * @param what human-readable location for the failure message
   */
  static void assertEquals(Object expected, Object actual, String what) {
    if (!deepEquals(expected, actual)) {
      fail("%s: expected %s but decoded %s", what, render(expected), render(actual));
    }
  }

  /** Structural equality over the codec value model (byte-exact and sign-exact). */
  private static boolean deepEquals(Object expected, Object actual) {
    if (expected instanceof byte[] expectedBytes && actual instanceof byte[] actualBytes) {
      return java.util.Arrays.equals(expectedBytes, actualBytes);
    }
    if (expected instanceof Double expectedDouble && actual instanceof Double actualDouble) {
      // Double.equals is bit-exact, so -0.0 and +0.0 are distinguishable.
      return expectedDouble.equals(actualDouble);
    }
    if (expected instanceof List<?> expectedList && actual instanceof List<?> actualList) {
      if (expectedList.size() != actualList.size()) {
        return false;
      }
      for (int i = 0; i < expectedList.size(); i++) {
        if (!deepEquals(expectedList.get(i), actualList.get(i))) {
          return false;
        }
      }
      return true;
    }
    if (expected instanceof Map<?, ?> expectedMap && actual instanceof Map<?, ?> actualMap) {
      if (!Objects.equals(expectedMap.keySet(), actualMap.keySet())) {
        return false;
      }
      for (Map.Entry<?, ?> entry : expectedMap.entrySet()) {
        if (!deepEquals(entry.getValue(), actualMap.get(entry.getKey()))) {
          return false;
        }
      }
      return true;
    }
    return Objects.equals(expected, actual);
  }

  /** Renders a value model object for failure messages. */
  static String render(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof byte[] bytes) {
      return "bytes:" + HexFormat.of().formatHex(bytes);
    }
    if (value instanceof String text) {
      return "\"" + text + "\"";
    }
    if (value instanceof Map<?, ?> map) {
      return map.toString();
    }
    if (value instanceof List<?> list) {
      return list.toString();
    }
    return value.toString();
  }
}
