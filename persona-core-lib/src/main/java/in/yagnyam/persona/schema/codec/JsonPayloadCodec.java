package in.yagnyam.persona.schema.codec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.yagnyam.persona.schema.ArrayType;
import in.yagnyam.persona.schema.CanonicalJson;
import in.yagnyam.persona.schema.EnumType;
import in.yagnyam.persona.schema.EnvelopeType;
import in.yagnyam.persona.schema.Field;
import in.yagnyam.persona.schema.MapType;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.PrimitiveType;
import in.yagnyam.persona.schema.RecordType;
import in.yagnyam.persona.schema.TypeNode;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The canonical JSON payload encoding of {@code docs/SCHEMA.md} §4 ({@code payloadFormat =
 * "json"}).
 *
 * <p>The payload is a canonical JSON document using the writer rules of §2.1 (no whitespace,
 * object keys sorted by UTF-8 byte order, the exact escaping of §2.1 rule 4) with the renderings
 * of §4.1: {@code bytes} as padded standard base64, {@code timestamp-millis} as ISO-8601 UTC with
 * exactly three fractional digits, enums as their symbol, arrays and maps as JSON arrays and
 * objects, and envelope fields as nested objects whose {@code payload} and signature {@code value}
 * members are base64 strings.
 *
 * <p>The codec is stateless and thread-safe.
 */
public final class JsonPayloadCodec implements PayloadCodec {

  /** The {@code payloadFormat} identifier of this codec (SCHEMA.md §4). */
  public static final String FORMAT = "json";

  private static final ObjectMapper MAPPER =
      new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

  private static final DateTimeFormatter UTC_SECONDS =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);

  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

  private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

  @Override
  public String format() {
    return FORMAT;
  }

  @Override
  public byte[] encode(PersonaSchema schema, Map<String, Object> value) {
    Objects.requireNonNull(schema, "schema");
    Objects.requireNonNull(value, "value");
    StringBuilder out = new StringBuilder();
    renderRecord(out, schema.rootRecord(), value, "");
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Map<String, Object> decode(PersonaSchema schema, byte[] bytes) {
    Objects.requireNonNull(schema, "schema");
    Objects.requireNonNull(bytes, "bytes");
    JsonNode document = readJson(decodeUtf8(bytes));
    if (!document.isObject()) {
      throw new PayloadDecodingException(
          "payload root must be a JSON object describing record '"
              + schema.rootRecord().name()
              + "', found "
              + document.getNodeType());
    }
    return decodeRecord(document, schema.rootRecord(), "");
  }

  /** Writes a record as a JSON object with sorted keys (§2.1 rule 2). */
  private static void renderRecord(
      StringBuilder out, RecordType record, Map<String, Object> value, String path) {
    PayloadValues.requireKnownFields(record, value, path);
    Map<String, String> members = new LinkedHashMap<>();
    for (Field field : record.fields()) {
      Object fieldValue = PayloadValues.requireField(record, value, field, path);
      members.put(
          field.name(),
          render(field.type(), fieldValue, PayloadValues.childPath(path, field.name())));
    }
    CanonicalJson.writeSortedObject(out, members);
  }

  /** Writes a value of the given type (§4.1). */
  private static void renderValue(StringBuilder out, TypeNode type, Object value, String path) {
    switch (type) {
      case PrimitiveType primitive -> renderPrimitive(out, primitive, value, path);
      case EnumType enumType -> renderEnum(out, enumType, value, path);
      case ArrayType arrayType -> renderArray(out, arrayType, value, path);
      case MapType mapType -> renderMap(out, mapType, value, path);
      case RecordType recordType ->
          renderRecord(out, recordType, PayloadValues.requireMap(value, path), path);
      case EnvelopeType envelopeType ->
          renderRecord(out, envelopeType.record(), PayloadValues.requireMap(value, path), path);
    }
  }

  /** Writes a primitive (§4.1). */
  private static void renderPrimitive(
      StringBuilder out, PrimitiveType primitive, Object value, String path) {
    switch (primitive.name()) {
      case PrimitiveType.STRING ->
          CanonicalJson.writeString(out, PayloadValues.requireText(value, path));
      case PrimitiveType.BOOLEAN -> out.append(PayloadValues.requireBoolean(value, path));
      case PrimitiveType.INT -> out.append(PayloadValues.requireInt(value, path));
      case PrimitiveType.LONG -> out.append(PayloadValues.requireLong(value, path));
      case PrimitiveType.DOUBLE ->
          out.append(CanonicalJson.doubleLiteral(PayloadValues.requireDouble(value, path)));
      case PrimitiveType.BYTES -> CanonicalJson.writeString(
          out, BASE64_ENCODER.encodeToString(PayloadValues.requireBytes(value, path)));
      case PrimitiveType.TIMESTAMP_MILLIS -> CanonicalJson.writeString(
          out, isoInstant(PayloadValues.requireInstant(value, path)));
      default -> throw new IllegalStateException("unknown primitive type " + primitive.name());
    }
  }

  /** Writes an enum symbol (§4.1). */
  private static void renderEnum(
      StringBuilder out, EnumType enumType, Object value, String path) {
    String symbol = PayloadValues.requireText(value, path);
    if (!enumType.symbols().contains(symbol)) {
      throw new PayloadEncodingException(
          PayloadValues.at(path)
              + ": '"
              + symbol
              + "' is not a symbol of enum '"
              + enumType.name()
              + "'; declared symbols are "
              + enumType.symbols());
    }
    CanonicalJson.writeString(out, symbol);
  }

  /** Writes an array (§4.1). */
  private static void renderArray(
      StringBuilder out, ArrayType arrayType, Object value, String path) {
    List<?> items = PayloadValues.requireList(value, path);
    out.append('[');
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      Object item = items.get(i);
      if (item == null) {
        throw new PayloadEncodingException(
            PayloadValues.at(path + "[" + i + "]") + ": value must not be null");
      }
      renderValue(out, arrayType.items(), item, path + "[" + i + "]");
    }
    out.append(']');
  }

  /** Writes a map as a JSON object with sorted keys (§2.1 rule 2, §4.1). */
  private static void renderMap(StringBuilder out, MapType mapType, Object value, String path) {
    Map<String, Object> entries = PayloadValues.requireMap(value, path);
    Map<String, String> members = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : entries.entrySet()) {
      if (!CanonicalJson.isWellFormed(entry.getKey())) {
        throw new PayloadEncodingException(
            PayloadValues.at(path)
                + ": map key contains an unpaired surrogate and cannot be encoded as UTF-8");
      }
      if (entry.getValue() == null) {
        throw new PayloadEncodingException(
            PayloadValues.at(path + "['" + entry.getKey() + "']") + ": value must not be null");
      }
      members.put(
          entry.getKey(),
          render(mapType.values(), entry.getValue(), path + "['" + entry.getKey() + "']"));
    }
    CanonicalJson.writeSortedObject(out, members);
  }

  /** Renders a value to a standalone canonical JSON fragment. */
  private static String render(TypeNode type, Object value, String path) {
    StringBuilder out = new StringBuilder();
    renderValue(out, type, value, path);
    return out.toString();
  }

  /** Renders an instant as ISO-8601 UTC with exactly three fractional digits (§4.1). */
  private static String isoInstant(Instant instant) {
    return UTC_SECONDS.format(instant)
        + "."
        + String.format("%03d", instant.getNano() / 1_000_000)
        + "Z";
  }

  /** Decodes a JSON document from payload bytes (§4, §3.3-style strictness). */
  private static String decodeUtf8(byte[] bytes) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException e) {
      throw new PayloadDecodingException("payload is not valid UTF-8 (SCHEMA.md §4.1)", e);
    }
  }

  /** Parses exactly one JSON value. */
  private static JsonNode readJson(String json) {
    try (JsonParser parser = MAPPER.getFactory().createParser(json)) {
      JsonNode document = MAPPER.readTree(parser);
      if (parser.nextToken() != null) {
        throw new PayloadDecodingException(
            "payload must contain exactly one JSON value, found trailing content");
      }
      if (document == null || document.isMissingNode()) {
        throw new PayloadDecodingException("payload is empty");
      }
      return document;
    } catch (IOException e) {
      throw new PayloadDecodingException("payload is not valid JSON: " + e.getMessage(), e);
    }
  }

  /** Decodes a JSON object into a record value. */
  private static Map<String, Object> decodeRecord(JsonNode node, RecordType record, String path) {
    if (!node.isObject()) {
      throw new PayloadDecodingException(
          PayloadValues.at(path)
              + ": expected a JSON object for record '"
              + record.name()
              + "', found "
              + node.getNodeType());
    }
    node.fieldNames()
        .forEachRemaining(
            key -> {
              if (!PayloadValues.hasField(record, key)) {
                throw new PayloadDecodingException(
                    PayloadValues.at(path)
                        + " (record '"
                        + record.name()
                        + "'): unknown field '"
                        + key
                        + "'; declared fields are "
                        + record.fields().stream().map(Field::name).toList());
              }
            });
    Map<String, Object> value = new LinkedHashMap<>();
    for (Field field : record.fields()) {
      JsonNode fieldNode = node.get(field.name());
      if (fieldNode == null) {
        throw new PayloadDecodingException(
            PayloadValues.at(path)
                + " (record '"
                + record.name()
                + "'): missing field '"
                + field.name()
                + "'; all fields are required (SCHEMA.md §1.3 rule 5)");
      }
      value.put(
          field.name(),
          decodeValue(fieldNode, field.type(), PayloadValues.childPath(path, field.name())));
    }
    return value;
  }

  /** Decodes a JSON value against its declared type. */
  private static Object decodeValue(JsonNode node, TypeNode type, String path) {
    if (node.isNull()) {
      throw new PayloadDecodingException(PayloadValues.at(path) + ": value must not be null");
    }
    return switch (type) {
      case PrimitiveType primitive -> decodePrimitive(node, primitive, path);
      case EnumType enumType -> decodeEnum(node, enumType, path);
      case ArrayType arrayType -> decodeArray(node, arrayType, path);
      case MapType mapType -> decodeMap(node, mapType, path);
      case RecordType recordType -> decodeRecord(node, recordType, path);
      case EnvelopeType envelopeType -> decodeRecord(node, envelopeType.record(), path);
    };
  }

  /** Decodes a primitive (§4.1). */
  private static Object decodePrimitive(JsonNode node, PrimitiveType primitive, String path) {
    return switch (primitive.name()) {
      case PrimitiveType.STRING -> requireTextual(node, path);
      case PrimitiveType.BOOLEAN -> {
        if (!node.isBoolean()) {
          throw mismatch(node, path, "a JSON boolean");
        }
        yield node.booleanValue();
      }
      case PrimitiveType.INT -> {
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
          throw mismatch(node, path, "a JSON integer in the signed 32-bit range");
        }
        yield node.intValue();
      }
      case PrimitiveType.LONG -> {
        if (!node.isIntegralNumber() || !node.canConvertToLong()) {
          throw mismatch(node, path, "a JSON integer in the signed 64-bit range");
        }
        yield node.longValue();
      }
      case PrimitiveType.DOUBLE -> {
        if (!node.isNumber()) {
          throw mismatch(node, path, "a JSON number");
        }
        double value = node.doubleValue();
        if (!Double.isFinite(value)) {
          throw new PayloadDecodingException(
              PayloadValues.at(path)
                  + ": non-finite double values are not encodable (SCHEMA.md §4.3)");
        }
        yield value;
      }
      case PrimitiveType.BYTES -> {
        String text = requireTextual(node, path);
        try {
          yield BASE64_DECODER.decode(text);
        } catch (IllegalArgumentException e) {
          throw new PayloadDecodingException(
              PayloadValues.at(path)
                  + ": '"
                  + text
                  + "' is not standard base64 with padding (SCHEMA.md §4.1)",
              e);
        }
      }
      case PrimitiveType.TIMESTAMP_MILLIS -> parseInstant(requireTextual(node, path), path);
      default -> throw new IllegalStateException("unknown primitive type " + primitive.name());
    };
  }

  /** Decodes an enum symbol (§4.1). */
  private static String decodeEnum(JsonNode node, EnumType enumType, String path) {
    String symbol = requireTextual(node, path);
    if (!enumType.symbols().contains(symbol)) {
      throw new PayloadDecodingException(
          PayloadValues.at(path)
              + ": '"
              + symbol
              + "' is not a symbol of enum '"
              + enumType.name()
              + "'; declared symbols are "
              + enumType.symbols());
    }
    return symbol;
  }

  /** Decodes a JSON array. */
  private static List<Object> decodeArray(JsonNode node, ArrayType arrayType, String path) {
    if (!node.isArray()) {
      throw mismatch(node, path, "a JSON array");
    }
    List<Object> items = new ArrayList<>();
    for (int i = 0; i < node.size(); i++) {
      items.add(decodeValue(node.get(i), arrayType.items(), path + "[" + i + "]"));
    }
    return items;
  }

  /**
   * Decodes a JSON object into a map value (§4.1: map keys are arbitrary strings, so no
   * unknown-field check applies).
   *
   */
  private static Map<String, Object> decodeMap(JsonNode node, MapType mapType, String path) {
    if (!node.isObject()) {
      throw mismatch(node, path, "a JSON object");
    }
    Map<String, Object> entries = new LinkedHashMap<>();
    node.fields()
        .forEachRemaining(
            entry ->
                entries.put(
                    entry.getKey(),
                    decodeValue(
                        entry.getValue(), mapType.values(), path + "['" + entry.getKey() + "']")));
    return entries;
  }

  /** Parses an ISO-8601 instant (§4.1). */
  private static Instant parseInstant(String text, String path) {
    Instant instant;
    try {
      instant = Instant.parse(text);
    } catch (DateTimeException e) {
      throw new PayloadDecodingException(
          PayloadValues.at(path)
              + ": '"
              + text
              + "' is not an ISO-8601 UTC instant (SCHEMA.md §4.1)",
          e);
    }
    if (instant.getNano() % 1_000_000 != 0) {
      throw new PayloadDecodingException(
          PayloadValues.at(path)
              + ": timestamp-millis values must have millisecond precision, found "
              + text);
    }
    return instant;
  }

  /** Requires a textual JSON node. */
  private static String requireTextual(JsonNode node, String path) {
    if (!node.isTextual()) {
      throw mismatch(node, path, "a JSON string");
    }
    return node.textValue();
  }

  /** Builds a type mismatch error. */
  private static PayloadDecodingException mismatch(JsonNode node, String path, String expected) {
    return new PayloadDecodingException(
        PayloadValues.at(path) + ": expected " + expected + ", found " + node.getNodeType());
  }
}
