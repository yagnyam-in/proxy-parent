package in.yagnyam.persona.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadDecodingException;
import in.yagnyam.persona.schema.codec.PayloadEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JsonPayloadCodec}: exact canonical JSON output (SCHEMA.md §4), round trips, and
 * rejection of malformed payloads.
 */
class JsonPayloadCodecTest {

  private static final PayloadCodec CODEC = new JsonPayloadCodec();

  @Test
  void formatIsJson() {
    assertThat(CODEC.format()).isEqualTo("json");
  }

  @Test
  void encodesCanonicalJsonWithSortedKeysAndNoWhitespace() {
    PersonaSchema schema =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"R\",\"fields\":["
                + "{\"name\":\"z\",\"type\":\"string\"},{\"name\":\"a\",\"type\":\"int\"}]}");
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("z", "x");
    value.put("a", 1);

    assertThat(encode(schema, value)).isEqualTo("{\"a\":1,\"z\":\"x\"}");
  }

  @Test
  void encodesTheKitchenSinkExactly() {
    assertThat(encode(SchemaFixtures.KITCHEN, SchemaFixtures.kitchenValue()))
        .isEqualTo(SchemaFixtures.KITCHEN_CANONICAL_JSON);
  }

  @Test
  void rendersPrimitivesPerSectionFourOne() {
    assertThat(encodeSingle("\"string\"", "a\"b\\c\n")).isEqualTo("{\"v\":\"a\\\"b\\\\c\\n\"}");
    assertThat(encodeSingle("\"boolean\"", true)).isEqualTo("{\"v\":true}");
    assertThat(encodeSingle("\"boolean\"", false)).isEqualTo("{\"v\":false}");
    assertThat(encodeSingle("\"int\"", -42)).isEqualTo("{\"v\":-42}");
    assertThat(encodeSingle("\"long\"", 42L)).isEqualTo("{\"v\":42}");
    assertThat(encodeSingle("\"double\"", 1.5d)).isEqualTo("{\"v\":1.5}");
    assertThat(encodeSingle("\"double\"", -0.0d)).isEqualTo("{\"v\":-0.0}");
    assertThat(encodeSingle("\"double\"", 1.0d)).isEqualTo("{\"v\":1.0}");
    assertThat(encodeSingle("\"double\"", 1e308d))
        .isEqualTo("{\"v\":" + Double.toString(1e308d) + "}");
    assertThat(encodeSingle("\"bytes\"", new byte[] {0, 1, 2, (byte) 255}))
        .isEqualTo("{\"v\":\"AAEC/w==\"}");
    assertThat(encodeSingle("\"bytes\"", new byte[0])).isEqualTo("{\"v\":\"\"}");
    assertThat(
            encodeSingle(
                "\"timestamp-millis\"", Instant.parse("2026-10-10T12:00:00.000Z")))
        .isEqualTo("{\"v\":\"2026-10-10T12:00:00.000Z\"}");
    assertThat(encodeSingle("\"timestamp-millis\"", Instant.ofEpochMilli(0)))
        .isEqualTo("{\"v\":\"1970-01-01T00:00:00.000Z\"}");
    assertThat(encodeSingle("\"timestamp-millis\"", Instant.ofEpochMilli(1500)))
        .isEqualTo("{\"v\":\"1970-01-01T00:00:01.500Z\"}");
    assertThat(encodeSingle("{\"type\":\"enum\",\"name\":\"M\",\"symbols\":[\"A\",\"B\"]}", "B"))
        .isEqualTo("{\"v\":\"B\"}");
  }

  @Test
  void rendersEnvelopeFieldsAsNestedObjects() {
    assertThat(encodeSingle("\"envelope\"", SchemaFixtures.signedEnvelopeValue()))
        .isEqualTo(
            "{\"v\":{\"messageType\":\"example.Greeting@v1\",\"payload\":\"AQID\","
                + "\"payloadFormat\":\"json\",\"schemaId\":\""
                + SchemaFixtures.SIMPLE_SCHEMA_ID
                + "\",\"signatures\":[{\"algorithm\":\"SHA256withRSA\",\"value\":\"CQg=\"}],"
                + "\"signedBy\":\"alice\"}}");
  }

  @Test
  void sortsMapKeysByUtf8Bytes() {
    PersonaSchema schema =
        SchemaFixtures.singleField("{\"type\":\"map\",\"values\":\"string\"}");
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("z", "1");
    entries.put("é", "2");
    entries.put("a", "3");

    assertThat(encode(schema, Map.of("v", entries))).isEqualTo("{\"v\":{\"a\":\"3\",\"z\":\"1\",\"é\":\"2\"}}");
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

    assertThat(encode(schema, value)).isEqualTo("{\"a\":[],\"m\":{}}");
    ValueSupport.assertDeepEquals(value, CODEC.decode(schema, CODEC.encode(schema, value)));
  }

  @Test
  void roundTripsMultiEnvelopeValues() {
    PersonaSchema schema = SchemaFixtures.singleField("\"multi-envelope\"");
    Map<String, Object> value = new LinkedHashMap<>();
    Map<String, Object> multiSignature = new LinkedHashMap<>();
    multiSignature.put("signedBy", "alice");
    multiSignature.put("signatures", List.of(Map.of("algorithm", "SHA256withRSA", "value", new byte[] {7})));
    value.put("messageType", "example.Charter@v1");
    value.put("payloadFormat", "json");
    value.put("schemaId", SchemaFixtures.SIMPLE_SCHEMA_ID);
    value.put("payload", new byte[] {1, 2});
    value.put("signatures", List.of(multiSignature));

    byte[] encoded = CODEC.encode(schema, Map.of("v", value));

    assertThat(new String(encoded, StandardCharsets.UTF_8))
        .isEqualTo(
            "{\"v\":{\"messageType\":\"example.Charter@v1\",\"payload\":\"AQI=\","
                + "\"payloadFormat\":\"json\",\"schemaId\":\""
                + SchemaFixtures.SIMPLE_SCHEMA_ID
                + "\",\"signatures\":[{\"signatures\":[{"
                + "\"algorithm\":\"SHA256withRSA\",\"value\":\"Bw==\"}],\"signedBy\":\"alice\"}]}}");
    ValueSupport.assertDeepEquals(Map.of("v", value), CODEC.decode(schema, encoded));
  }

  @Test
  void decoderRejectsMalformedDocuments() {
    PersonaSchema schema = SchemaFixtures.singleField("\"int\"");

    assertThatThrownBy(() -> CODEC.decode(schema, new byte[0]))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("payload is empty");
    assertThatThrownBy(() -> CODEC.decode(schema, utf8("{,}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not valid JSON");
    assertThatThrownBy(() -> CODEC.decode(schema, utf8("{\"v\":1} {}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("trailing content");
    assertThatThrownBy(() -> CODEC.decode(schema, utf8("[]")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("payload root must be a JSON object");
    assertThatThrownBy(() -> CODEC.decode(schema, utf8("{\"v\":1,\"v\":2}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("Duplicate field");
    assertThatThrownBy(() -> CODEC.decode(schema, new byte[] {(byte) 0xff, (byte) 0xfe}))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not valid UTF-8");
  }

  @Test
  void decoderRejectsUnknownAndMissingFields() {
    PersonaSchema schema = SchemaFixtures.singleField("\"int\"");

    assertThatThrownBy(() -> CODEC.decode(schema, utf8("{\"v\":1,\"w\":2}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("unknown field 'w'");
    assertThatThrownBy(() -> CODEC.decode(schema, utf8("{}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("missing field 'v'");
    assertThatThrownBy(() -> CODEC.decode(schema, utf8("{\"v\":null}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("value must not be null");
  }

  @Test
  void decoderRejectsWrongJsonTypes() {
    assertDecodeRejected("\"string\"", "1", "expected a JSON string");
    assertDecodeRejected("\"boolean\"", "1", "expected a JSON boolean");
    assertDecodeRejected("\"int\"", "\"1\"", "expected a JSON integer");
    assertDecodeRejected("\"int\"", "1.5", "expected a JSON integer");
    assertDecodeRejected("\"int\"", "2147483648", "expected a JSON integer");
    assertDecodeRejected("\"long\"", "1.5", "expected a JSON integer");
    assertDecodeRejected("\"long\"", "\"1\"", "expected a JSON integer");
    assertDecodeRejected("\"double\"", "\"1.5\"", "expected a JSON number");
    assertDecodeRejected("\"double\"", "1e999", "non-finite");
    assertDecodeRejected("\"bytes\"", "\"not base64!\"", "not standard base64");
    assertDecodeRejected("\"bytes\"", "1", "expected a JSON string");
    assertDecodeRejected("\"timestamp-millis\"", "\"nope\"", "not an ISO-8601 UTC instant");
    assertDecodeRejected(
        "\"timestamp-millis\"", "\"2026-10-10T12:00:00.123456Z\"", "millisecond precision");
    assertDecodeRejected(
        "{\"type\":\"enum\",\"name\":\"M\",\"symbols\":[\"A\"]}", "\"B\"", "is not a symbol");
    assertDecodeRejected("{\"type\":\"array\",\"items\":\"int\"}", "{}", "expected a JSON array");
    assertDecodeRejected("{\"type\":\"map\",\"values\":\"int\"}", "[]", "expected a JSON object");
    assertDecodeRejected(
        "{\"type\":\"record\",\"name\":\"Inner\",\"fields\":[]}", "[]", "expected a JSON object");
    assertDecodeRejected("\"envelope\"", "[]", "expected a JSON object");
  }

  @Test
  void decoderRejectsMalformedEnvelopes() {
    PersonaSchema schema = SchemaFixtures.singleField("\"envelope\"");
    Map<String, Object> envelope = SchemaFixtures.signedEnvelopeValue();
    envelope.remove("signedBy");

    assertThatThrownBy(() -> CODEC.decode(schema, utf8("{\"v\":" + json(envelope) + "}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("missing field 'signedBy'");
    assertThatThrownBy(
            () ->
                CODEC.decode(
                    schema,
                    utf8(
                        "{\"v\":{\"messageType\":\"m\",\"payloadFormat\":\"json\","
                            + "\"schemaId\":\"\",\"payload\":\"!!\",\"signedBy\":\"a\","
                            + "\"signatures\":[]}}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not standard base64");
  }

  @Test
  void encoderRejectsMissingUnknownAndWronglyTypedFields() {
    PersonaSchema schema = SchemaFixtures.singleField("\"int\"");
    Map<String, Object> unknown = new LinkedHashMap<>();
    unknown.put("v", 1);
    unknown.put("w", 2);

    assertThatThrownBy(() -> CODEC.encode(schema, Map.of()))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("missing field 'v'");
    assertThatThrownBy(() -> CODEC.encode(schema, unknown))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("unknown field 'w'");
    assertThatThrownBy(() -> CODEC.encode(schema, Map.of("v", 1L)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("expected an Integer");
    assertThatThrownBy(() -> CODEC.encode(schema, Map.of("v", Double.NaN)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("expected an Integer");
    assertThatThrownBy(() -> CODEC.encode(schema, Map.of("v", Double.POSITIVE_INFINITY)))
        .isInstanceOf(PayloadEncodingException.class);
    assertThatThrownBy(
            () -> CODEC.encode(SchemaFixtures.singleField("\"double\""), Map.of("v", Double.NaN)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("non-finite");
    assertThatThrownBy(
            () ->
                CODEC.encode(
                    SchemaFixtures.singleField("\"timestamp-millis\""),
                    Map.of("v", Instant.parse("2026-10-10T12:00:00.123456Z"))))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("millisecond precision");
    assertThatThrownBy(
            () ->
                CODEC.encode(
                    SchemaFixtures.singleField(
                        "{\"type\":\"enum\",\"name\":\"M\",\"symbols\":[\"A\"]}"),
                    Map.of("v", "B")))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("is not a symbol");
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

  private static String encode(PersonaSchema schema, Map<String, Object> value) {
    return new String(CODEC.encode(schema, value), StandardCharsets.UTF_8);
  }

  private static String encodeSingle(String typeJson, Object value) {
    return encode(SchemaFixtures.singleField(typeJson), Map.of("v", value));
  }

  private static void assertDecodeRejected(String typeJson, String fieldJson, String fragment) {
    PersonaSchema schema = SchemaFixtures.singleField(typeJson);
    assertThatThrownBy(() -> CODEC.decode(schema, utf8("{\"v\":" + fieldJson + "}")))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining(fragment);
  }

  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  /** Renders a value map as plain (non-canonical) JSON so that a decoder can be fed it. */
  private static String json(Object value) {
    if (value instanceof Map<?, ?> map) {
      List<String> members = new ArrayList<>();
      map.forEach(
          (key, entry) ->
              members.add("\"" + key + "\":" + json(entry)));
      return "{" + String.join(",", members) + "}";
    }
    if (value instanceof List<?> list) {
      List<String> items = new ArrayList<>();
      list.forEach(item -> items.add(json(item)));
      return "[" + String.join(",", items) + "]";
    }
    if (value instanceof byte[] bytes) {
      return "\"" + java.util.Base64.getEncoder().encodeToString(bytes) + "\"";
    }
    if (value instanceof String text) {
      return "\"" + text + "\"";
    }
    return String.valueOf(value);
  }
}
