package in.yagnyam.persona.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadDecodingException;
import in.yagnyam.persona.schema.codec.PayloadEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BinaryPayloadCodec}: pinned golden bytes for the SCHEMA.md §3 encodings, a
 * full round trip over every node kind and primitive, decoder strictness (§3.3) and encoder
 * rejections.
 *
 * <p>The golden bytes were produced by an independent Python implementation of §3, not by this
 * codec.
 */
class BinaryPayloadCodecTest {

  private static final PayloadCodec CODEC = new BinaryPayloadCodec();

  @Test
  void formatIsPersonaBinary() {
    assertThat(CODEC.format()).isEqualTo("persona-binary");
  }

  @Test
  void encodesPrimitivesAsPinnedGoldenBytes() {
    assertThat(encodeSingle("\"int\"", 64)).isEqualTo("80 01");
    assertThat(encodeSingle("\"int\"", -64)).isEqualTo("7f");
    assertThat(encodeSingle("\"int\"", 0)).isEqualTo("00");
    assertThat(encodeSingle("\"int\"", 1)).isEqualTo("02");
    assertThat(encodeSingle("\"int\"", -1)).isEqualTo("01");
    assertThat(encodeSingle("\"int\"", Integer.MAX_VALUE)).isEqualTo("fe ff ff ff 0f");
    assertThat(encodeSingle("\"int\"", Integer.MIN_VALUE)).isEqualTo("ff ff ff ff 0f");
    assertThat(encodeSingle("\"long\"", 150L)).isEqualTo("ac 02");
    assertThat(encodeSingle("\"long\"", -1L)).isEqualTo("01");
    assertThat(encodeSingle("\"long\"", Long.MAX_VALUE)).isEqualTo("fe ff ff ff ff ff ff ff ff 01");
    assertThat(encodeSingle("\"long\"", Long.MIN_VALUE)).isEqualTo("ff ff ff ff ff ff ff ff ff 01");
    assertThat(encodeSingle("\"string\"", "foo")).isEqualTo("06 66 6f 6f");
    assertThat(encodeSingle("\"string\"", "")).isEqualTo("00");
    assertThat(encodeSingle("\"string\"", "é")).isEqualTo("04 c3 a9");
    assertThat(encodeSingle("\"boolean\"", true)).isEqualTo("01");
    assertThat(encodeSingle("\"boolean\"", false)).isEqualTo("00");
    assertThat(encodeSingle("\"double\"", 1.5d)).isEqualTo("00 00 00 00 00 00 f8 3f");
    assertThat(encodeSingle("\"double\"", -0.0d)).isEqualTo("00 00 00 00 00 00 00 80");
    assertThat(encodeSingle("\"double\"", 1.0d)).isEqualTo("00 00 00 00 00 00 f0 3f");
    assertThat(encodeSingle("\"bytes\"", bytes(1, 2, 3))).isEqualTo("06 01 02 03");
    assertThat(encodeSingle("\"bytes\"", new byte[0])).isEqualTo("00");
    assertThat(encodeSingle("\"timestamp-millis\"", Instant.ofEpochMilli(0))).isEqualTo("00");
    assertThat(encodeSingle("\"timestamp-millis\"", Instant.ofEpochMilli(150))).isEqualTo("ac 02");
    assertThat(
            encodeSingle("\"timestamp-millis\"", Instant.parse("2026-10-10T12:00:00.000Z")))
        .isEqualTo("80 b8 f9 da a4 68");
  }

  @Test
  void encodesCompoundsAsPinnedGoldenBytes() {
    String enumSchema = "{\"type\":\"enum\",\"name\":\"M\",\"symbols\":[\"A\",\"B\",\"C\"]}";
    assertThat(encodeSingle(enumSchema, "A")).isEqualTo("00");
    assertThat(encodeSingle(enumSchema, "B")).isEqualTo("02");
    assertThat(encodeSingle(enumSchema, "C")).isEqualTo("04");
    assertThat(encodeSingle("{\"type\":\"array\",\"items\":\"string\"}", List.of()))
        .isEqualTo("00");
    assertThat(encodeSingle("{\"type\":\"array\",\"items\":\"string\"}", List.of("a", "b")))
        .isEqualTo("04 02 61 02 62 00");
    assertThat(encodeSingle("{\"type\":\"map\",\"values\":\"string\"}", Map.of())).isEqualTo("00");
    assertThat(encodeSingle("{\"type\":\"map\",\"values\":\"string\"}", Map.of("a", "x")))
        .isEqualTo("02 02 61 02 78 00");
    assertThat(
            encodeSingle(
                "{\"type\":\"record\",\"name\":\"Inner\",\"fields\":[{\"name\":\"x\","
                    + "\"type\":\"long\"}]}",
                Map.of("x", 7L)))
        .isEqualTo("0e");
    assertThat(encodeSingle("\"envelope\"", SchemaFixtures.signedEnvelopeValue()))
        .isEqualTo(
            "26 65 78 61 6d 70 6c 65 2e 47 72 65 65 74 69 6e 67 40 76 31 08 6a 73 6f 6e 50 36 36"
                + " 63 64 34 63 63 37 66 38 39 30 66 38 30 63 62 64 61 34 33 66 61 35 64 36 39 65"
                + " 66 65 63 63 61 64 61 64 30 38 33 33 06 01 02 03 0a 61 6c 69 63 65 02 1a 53 48 41"
                + " 32 35 36 77 69 74 68 52 53 41 04 09 08 00");
  }

  @Test
  void sortsMapKeysByUtf8Bytes() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("z", "1");
    value.put("é", "2");
    value.put("a", "3");

    assertThat(hex(CODEC.encode(SchemaFixtures.singleField("{\"type\":\"map\",\"values\":\"string\"}"),
            Map.of("v", value))))
        .isEqualTo("06 02 61 02 33 02 7a 02 31 04 c3 a9 02 32 00");
  }

  @Test
  void encodesTheKitchenSinkExactly() {
    assertThat(hex(CODEC.encode(SchemaFixtures.KITCHEN, SchemaFixtures.kitchenValue())))
        .isEqualTo(KITCHEN_GOLDEN_HEX);
  }

  @Test
  void roundTripsTheKitchenSink() {
    byte[] encoded = CODEC.encode(SchemaFixtures.KITCHEN, SchemaFixtures.kitchenValue());
    Map<String, Object> decoded = CODEC.decode(SchemaFixtures.KITCHEN, encoded);

    ValueSupport.assertDeepEquals(SchemaFixtures.kitchenValue(), decoded);
    assertThat(CODEC.encode(SchemaFixtures.KITCHEN, decoded)).isEqualTo(encoded);
  }

  @Test
  void roundTripsEmptyArraysAndMaps() {
    PersonaSchema schema =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"Empty\",\"fields\":["
                + "{\"name\":\"a\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},"
                + "{\"name\":\"m\",\"type\":{\"type\":\"map\",\"values\":\"int\"}}]}");
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("a", List.of());
    value.put("m", Map.of());

    byte[] encoded = CODEC.encode(schema, value);

    assertThat(hex(encoded)).isEqualTo("00 00");
    ValueSupport.assertDeepEquals(value, CODEC.decode(schema, encoded));
  }

  @Test
  void decodesArraysSpanningSeveralBlocks() {
    PersonaSchema schema = SchemaFixtures.singleField("{\"type\":\"array\",\"items\":\"string\"}");

    // block of one item, block of one item, terminator: readers MUST loop over blocks (§3.2)
    Map<String, Object> decoded =
        CODEC.decode(schema, bytes(0x02, 0x02, 0x61, 0x02, 0x02, 0x62, 0x00));

    assertThat(decoded).containsEntry("v", List.of("a", "b"));
  }

  @Test
  void roundTripsMultiEnvelopeValues() {
    PersonaSchema schema = SchemaFixtures.singleField("\"multi-envelope\"");
    Map<String, Object> value = new LinkedHashMap<>();
    Map<String, Object> multiSignature = new LinkedHashMap<>();
    multiSignature.put("signedBy", "alice");
    multiSignature.put("signatures", List.of(Map.of("algorithm", "SHA256withRSA", "value", bytes(7))));
    value.put("messageType", "example.Charter@v1");
    value.put("payloadFormat", "persona-binary");
    value.put("schemaId", SchemaFixtures.SIMPLE_SCHEMA_ID);
    value.put("payload", bytes(1, 2));
    value.put("signatures", List.of(multiSignature));

    byte[] encoded = CODEC.encode(schema, Map.of("v", value));

    ValueSupport.assertDeepEquals(Map.of("v", value), CODEC.decode(schema, encoded));
  }

  @Test
  void decoderRejectsTrailingBytes() {
    PersonaSchema schema = SchemaFixtures.singleField("\"int\"");

    assertThatThrownBy(() -> CODEC.decode(schema, bytes(0x00, 0x00)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("trailing byte(s)");
  }

  @Test
  void decoderRejectsTruncatedInput() {
    PersonaSchema stringSchema = SchemaFixtures.singleField("\"string\"");
    PersonaSchema intSchema = SchemaFixtures.singleField("\"int\"");
    PersonaSchema doubleSchema = SchemaFixtures.singleField("\"double\"");
    PersonaSchema recordSchema =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"},"
                + "{\"name\":\"b\",\"type\":\"string\"}]}");
    byte[] kitchen = CODEC.encode(SchemaFixtures.KITCHEN, SchemaFixtures.kitchenValue());
    byte[] truncatedKitchen = new byte[kitchen.length - 1];
    System.arraycopy(kitchen, 0, truncatedKitchen, 0, truncatedKitchen.length);

    assertThatThrownBy(() -> CODEC.decode(stringSchema, bytes(0x04, 0x61)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("truncated input");
    assertThatThrownBy(() -> CODEC.decode(intSchema, bytes(0x80)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("truncated input");
    assertThatThrownBy(() -> CODEC.decode(doubleSchema, bytes(0x00, 0x00, 0x00)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("truncated input");
    assertThatThrownBy(() -> CODEC.decode(recordSchema, bytes(0x02)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("truncated input");
    assertThatThrownBy(() -> CODEC.decode(SchemaFixtures.KITCHEN, truncatedKitchen))
        .isInstanceOf(PayloadDecodingException.class);
  }

  @Test
  void decoderRejectsMalformedVarints() {
    PersonaSchema schema = SchemaFixtures.singleField("\"int\"");
    PersonaSchema longSchema = SchemaFixtures.singleField("\"long\"");

    assertThatThrownBy(
            () ->
                CODEC.decode(
                    schema,
                    bytes(0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x01)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("longer than 10 bytes");
    assertThatThrownBy(
            () ->
                CODEC.decode(
                    longSchema,
                    bytes(0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x02)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("varint overflow");
  }

  @Test
  void decoderRejectsAnIntOutsideTheThirtyTwoBitRange() {
    PersonaSchema schema = SchemaFixtures.singleField("\"int\"");

    assertThatThrownBy(() -> CODEC.decode(schema, bytes(0x80, 0x80, 0x80, 0x80, 0x80, 0x20)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("out of range");
  }

  @Test
  void decoderRejectsAnEnumIndexOutsideTheSymbols() {
    PersonaSchema schema =
        SchemaFixtures.singleField("{\"type\":\"enum\",\"name\":\"M\",\"symbols\":[\"A\",\"B\"]}");

    assertThatThrownBy(() -> CODEC.decode(schema, bytes(0x0a)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("enum index 5 is outside the 2 symbol(s) of enum 'M'");
    assertThatThrownBy(() -> CODEC.decode(schema, bytes(0x01)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("enum index -1");
  }

  @Test
  void decoderRejectsNegativeAndOversizedBlockCounts() {
    PersonaSchema arraySchema =
        SchemaFixtures.singleField("{\"type\":\"array\",\"items\":\"string\"}");
    PersonaSchema mapSchema =
        SchemaFixtures.singleField("{\"type\":\"map\",\"values\":\"string\"}");

    assertThatThrownBy(() -> CODEC.decode(arraySchema, bytes(0x01)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("negative block count -1");
    assertThatThrownBy(() -> CODEC.decode(mapSchema, bytes(0x01)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("negative block count -1");
    assertThatThrownBy(() -> CODEC.decode(arraySchema, bytes(0x80, 0x80, 0x80, 0x02)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("exceeds the implementation limit");
  }

  @Test
  void decoderRejectsInvalidUtf8() {
    PersonaSchema stringSchema = SchemaFixtures.singleField("\"string\"");
    PersonaSchema mapSchema =
        SchemaFixtures.singleField("{\"type\":\"map\",\"values\":\"string\"}");

    assertThatThrownBy(() -> CODEC.decode(stringSchema, bytes(0x04, 0xff, 0xfe)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not valid UTF-8");
    assertThatThrownBy(() -> CODEC.decode(mapSchema, bytes(0x02, 0x02, 0xc3, 0x28, 0x00)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not valid UTF-8");
    assertThatThrownBy(() -> CODEC.decode(stringSchema, bytes(0x02, 0xc3)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not valid UTF-8");
  }

  @Test
  void decoderRejectsOverlongUtf8Forms() {
    PersonaSchema stringSchema = SchemaFixtures.singleField("\"string\"");

    // 0xC0 0x80 is the overlong encoding of NUL; strict UTF-8 decoding rejects it (§3.4 rule 5)
    assertThatThrownBy(() -> CODEC.decode(stringSchema, bytes(0x04, 0xc0, 0x80)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not valid UTF-8");
    // 0xED 0xA0 0x80 encodes a UTF-16 surrogate, which valid UTF-8 never contains
    assertThatThrownBy(() -> CODEC.decode(stringSchema, bytes(0x06, 0xed, 0xa0, 0x80)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not valid UTF-8");
  }

  @Test
  void decoderAcceptsNonMinimalVarints() {
    PersonaSchema intSchema = SchemaFixtures.singleField("\"int\"");
    PersonaSchema longSchema = SchemaFixtures.singleField("\"long\"");

    // readers are lenient about non-minimal LEB128 encodings (§3.4 rule 3): zigzag(5) = 10 = 0x0a
    // padded to two bytes (0x8a 0x00) and long 0 padded to three bytes all decode
    assertThat(CODEC.decode(intSchema, bytes(0x8a, 0x00))).containsEntry("v", 5);
    assertThat(CODEC.decode(longSchema, bytes(0x80, 0x80, 0x00))).containsEntry("v", 0L);
  }

  @Test
  void decoderRejectsNonFiniteDoubles() {
    PersonaSchema schema = SchemaFixtures.singleField("\"double\"");

    // NaN, +Infinity and -Infinity as raw IEEE 754 bits, little-endian (§3.4 rule 9)
    assertThatThrownBy(
            () -> CODEC.decode(schema, bytes(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf8, 0x7f)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("non-finite double NaN");
    assertThatThrownBy(
            () -> CODEC.decode(schema, bytes(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0, 0x7f)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("non-finite double Infinity");
    assertThatThrownBy(
            () -> CODEC.decode(schema, bytes(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0, 0xff)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("non-finite double -Infinity");
  }

  @Test
  void decoderRejectsNegativeLengthsAndInvalidBooleanBytes() {
    PersonaSchema stringSchema = SchemaFixtures.singleField("\"string\"");
    PersonaSchema booleanSchema = SchemaFixtures.singleField("\"boolean\"");

    assertThatThrownBy(() -> CODEC.decode(stringSchema, bytes(0x01)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("negative length -1");
    assertThatThrownBy(() -> CODEC.decode(booleanSchema, bytes(0x02)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("invalid boolean byte 0x2");
  }

  @Test
  void decoderEnforcesTheEpochMillisFloor() {
    PersonaSchema schema = SchemaFixtures.singleField("\"timestamp-millis\"");
    long floor = -9223372036854775000L;

    // the bottom 808 long values cannot round-trip the Instant re-encoding (the epochSecond ×
    // 1000 computation overflows) and are rejected on decode (SCHEMA.md §3.4 rule 7)
    assertThatThrownBy(
            () ->
                CODEC.decode(
                    schema,
                    bytes(0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("below the minimum timestamp-millis value");
    assertThatThrownBy(() -> CODEC.decode(schema, zigzagVarint(Long.MIN_VALUE + 1)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("below the minimum timestamp-millis value");
    assertThatThrownBy(() -> CODEC.decode(schema, zigzagVarint(floor - 1)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("below the minimum timestamp-millis value");

    // floor is the lowest round-trippable value and must round-trip through encode + decode
    assertThat(CODEC.decode(schema, CODEC.encode(schema, Map.of("v", Instant.ofEpochMilli(floor)))))
        .containsEntry("v", Instant.ofEpochMilli(floor));
    // the top of the long range is intact: Long.MAX_VALUE round-trips
    assertThat(
            CODEC.decode(
                schema,
                bytes(0xfe, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01)))
        .containsEntry("v", Instant.ofEpochMilli(Long.MAX_VALUE));
  }

  @Test
  void decoderRejectsDuplicateMapKeys() {
    PersonaSchema schema =
        SchemaFixtures.singleField("{\"type\":\"map\",\"values\":\"string\"}");

    // block of two entries, both keyed "a"
    assertThatThrownBy(
            () -> CODEC.decode(schema, bytes(0x04, 0x02, 0x61, 0x02, 0x78, 0x02, 0x61, 0x02, 0x79, 0x00)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("appears more than once");
  }

  @Test
  void encoderRejectsMissingUnknownAndNullFields() {
    PersonaSchema schema = SchemaFixtures.singleField("\"int\"");
    Map<String, Object> unknown = new LinkedHashMap<>();
    unknown.put("v", 1);
    unknown.put("w", 2);
    Map<String, Object> nullValue = new LinkedHashMap<>();
    nullValue.put("v", null);
    Map<String, Object> missingField = SchemaFixtures.mutableKitchenValue();
    missingField.remove("body");
    Map<String, Object> unknownField = SchemaFixtures.mutableKitchenValue();
    unknownField.put("extra", "x");

    assertThatThrownBy(() -> CODEC.encode(schema, Map.of()))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("missing field 'v'");
    assertThatThrownBy(() -> CODEC.encode(schema, unknown))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("unknown field 'w'");
    assertThatThrownBy(() -> CODEC.encode(schema, nullValue))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("value must not be null");
    assertThatThrownBy(() -> CODEC.encode(SchemaFixtures.KITCHEN, missingField))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("missing field 'body'");
    assertThatThrownBy(() -> CODEC.encode(SchemaFixtures.KITCHEN, unknownField))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("unknown field 'extra'");
  }

  @Test
  void encoderRejectsWrongValueTypes() {
    assertTypeRejected("\"string\"", 1, "expected a String");
    assertTypeRejected("\"string\"", true, "expected a String");
    assertTypeRejected("\"boolean\"", "true", "expected a Boolean");
    assertTypeRejected("\"int\"", 1L, "expected an Integer");
    assertTypeRejected("\"int\"", 1.0d, "expected an Integer");
    assertTypeRejected("\"int\"", "1", "expected an Integer");
    assertTypeRejected("\"long\"", 1.0d, "expected a Long or Integer");
    assertTypeRejected("\"long\"", "1", "expected a Long or Integer");
    assertTypeRejected("\"double\"", 1, "expected a Double");
    assertTypeRejected("\"double\"", 1L, "expected a Double");
    assertTypeRejected("\"bytes\"", "AQID", "expected a byte[]");
    assertTypeRejected("\"timestamp-millis\"", 150L, "expected an Instant");
    assertTypeRejected(
        "{\"type\":\"enum\",\"name\":\"M\",\"symbols\":[\"A\"]}", "B", "is not a symbol of enum 'M'");
    assertTypeRejected("{\"type\":\"array\",\"items\":\"int\"}", "nope", "expected a List");
    assertTypeRejected("{\"type\":\"map\",\"values\":\"int\"}", "nope", "expected a Map");
    assertTypeRejected(
        "{\"type\":\"record\",\"name\":\"Inner\",\"fields\":[]}", List.of(), "expected a Map");
    assertTypeRejected("\"envelope\"", "nope", "expected a Map");
    assertTypeRejected("\"multi-envelope\"", List.of(), "expected a Map");
  }

  @Test
  void encoderRejectsNullsInsideCollectionsAndMaps() {
    PersonaSchema arraySchema = SchemaFixtures.singleField("{\"type\":\"array\",\"items\":\"int\"}");
    PersonaSchema mapSchema =
        SchemaFixtures.singleField("{\"type\":\"map\",\"values\":\"int\"}");
    List<Object> items = new ArrayList<>();
    items.add(null);
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("a", null);

    assertThatThrownBy(() -> CODEC.encode(arraySchema, Map.of("v", items)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("value must not be null");
    assertThatThrownBy(() -> CODEC.encode(mapSchema, Map.of("v", entries)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("value must not be null");
  }

  @Test
  @SuppressWarnings("unchecked")
  void encoderRejectsNonStringMapKeys() {
    PersonaSchema schema = SchemaFixtures.singleField("{\"type\":\"map\",\"values\":\"int\"}");
    Map<String, Object> bad = (Map<String, Object>) (Map<?, ?>) Map.of(1, 2);

    assertThatThrownBy(() -> CODEC.encode(schema, Map.of("v", bad)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("map keys must be strings");
  }

  @Test
  void encoderRejectsNonFiniteDoubles() {
    assertTypeRejected("\"double\"", Double.NaN, "non-finite");
    assertTypeRejected("\"double\"", Double.POSITIVE_INFINITY, "non-finite");
    assertTypeRejected("\"double\"", Double.NEGATIVE_INFINITY, "non-finite");
  }

  @Test
  void encoderRejectsSubMillisecondTimestamps() {
    assertTypeRejected(
        "\"timestamp-millis\"",
        Instant.parse("2026-10-10T12:00:00.123456Z"),
        "millisecond precision");
  }

  @Test
  void encoderRejectsTimestampsOutsideTheEpochMillisRange() {
    assertTypeRejected(
        "\"timestamp-millis\"",
        Instant.MAX.truncatedTo(ChronoUnit.SECONDS),
        "outside the range of timestamp-millis");
    assertTypeRejected(
        "\"timestamp-millis\"",
        Instant.MIN.truncatedTo(ChronoUnit.SECONDS),
        "outside the range of timestamp-millis");
  }

  @Test
  void encoderRejectsIllFormedStrings() {
    assertTypeRejected("\"string\"", "\ud83d", "unpaired surrogate");
    PersonaSchema mapSchema =
        SchemaFixtures.singleField("{\"type\":\"map\",\"values\":\"int\"}");
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("\ud83d", 1);

    assertThatThrownBy(() -> CODEC.encode(mapSchema, Map.of("v", entries)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("unpaired surrogate");
  }

  @Test
  void rejectsNullArguments() {
    assertThatThrownBy(() -> CODEC.encode(null, Map.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CODEC.encode(SchemaFixtures.SIMPLE, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CODEC.decode(null, new byte[0]))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> CODEC.decode(SchemaFixtures.SIMPLE, null))
        .isInstanceOf(NullPointerException.class);
  }

  private static String encodeSingle(String typeJson, Object value) {
    PersonaSchema schema = SchemaFixtures.singleField(typeJson);
    return hex(CODEC.encode(schema, Map.of("v", value)));
  }

  private static void assertTypeRejected(String typeJson, Object value, String fragment) {
    PersonaSchema schema = SchemaFixtures.singleField(typeJson);
    assertThatThrownBy(() -> CODEC.encode(schema, Map.of("v", value)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining(fragment);
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return bytes;
  }

  /**
   * An independent encoder of the §3.1 zigzag LEB128 varint, used to build payloads for values
   * whose golden bytes the fixtures do not pin.
   */
  private static byte[] zigzagVarint(long value) {
    long remaining = (value << 1) ^ (value >> 63);
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    while ((remaining & ~0x7fL) != 0) {
      out.write((int) ((remaining & 0x7f) | 0x80));
      remaining >>>= 7;
    }
    out.write((int) remaining);
    return out.toByteArray();
  }

  private static String hex(byte[] bytes) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      if (i > 0) {
        out.append(' ');
      }
      out.append(String.format("%02x", bytes[i]));
    }
    return out.toString();
  }

  private static final String KITCHEN_GOLDEN_HEX =
      "06 67 2d 31 0a 61 6c 69 63 65 06 62 6f 62 0a 68 65 6c 6c 6f 80 b8 f9 da a4 68 54 00 00"
          + " 00 00 00 00 f8 3f 01 08 00 01 02 ff 04 02 61 02 62 00 06 02 61 02 33 02 7a 02 31 04"
          + " c3 a9 02 32 00 02 0e 26 65 78 61 6d 70 6c 65 2e 47 72 65 65 74 69 6e 67 40 76 31 08"
          + " 6a 73 6f 6e 50 36 36 63 64 34 63 63 37 66 38 39 30 66 38 30 63 62 64 61 34 33 66 61"
          + " 35 64 36 39 65 66 65 63 63 61 64 61 64 30 38 33 33 06 01 02 03 0a 61 6c 69 63 65 02"
          + " 1a 53 48 41 32 35 36 77 69 74 68 52 53 41 04 09 08 00";
}
