package in.yagnyam.persona.schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema documents, parsed schemas and golden values shared by the schema tests.
 *
 * <p>The digests and wire bytes used by the tests were computed independently of this
 * implementation (a Python reference implementation of {@code docs/SCHEMA.md} §2, §3 and §4 driven
 * by the normative documents of §5) so that the tests cross-check the Java code instead of
 * restating it.
 */
final class SchemaFixtures {

  /** The example schema of SCHEMA.md §1. */
  static final String GREETING_SCHEMA_JSON = """
      {
        "type": "record",
        "name": "Greeting",
        "namespace": "example",
        "messageType": "example.Greeting@v1",
        "fields": [
          {"name": "greetingId", "type": "string"},
          {"name": "from", "type": "string"},
          {"name": "to", "type": "string"},
          {"name": "body", "type": "string"},
          {"name": "issuedAt", "type": "timestamp-millis"},
          {"name": "count", "type": "int"},
          {"name": "score", "type": "double"},
          {"name": "verified", "type": "boolean"},
          {"name": "attachment", "type": "bytes"},
          {"name": "tags", "type": {"type": "array", "items": "string"}},
          {"name": "attributes", "type": {"type": "map", "values": "string"}},
          {"name": "mood", "type": {"type": "enum", "name": "Mood", "symbols": ["HAPPY", "NEUTRAL", "SAD"]}},
          {"name": "inner", "type": {"type": "record", "name": "Inner", "fields": [{"name": "x", "type": "long"}]}},
          {"name": "forwarded", "type": "envelope"}
        ]
      }
      """;

  /** The example schema of SCHEMA.md §2.2. */
  static final String SIMPLE_SCHEMA_JSON =
      "{\"type\":\"record\",\"name\":\"Simple\",\"fields\":[{\"name\":\"a\",\"type\":\"string\"}]}";

  /** The canonical form of {@link #SIMPLE_SCHEMA_JSON} (SCHEMA.md §2.2). */
  static final String SIMPLE_CANONICAL_FORM =
      "{\"fields\":[{\"name\":\"a\",\"type\":\"string\"}],\"name\":\"Simple\",\"type\":\"record\"}";

  /** {@code shasum -a 1} of {@link #SIMPLE_CANONICAL_FORM}. */
  static final String SIMPLE_SCHEMA_ID = "66cd4cc7f890f80cbda43fa5d69efeccadad0833";

  /** Digest of the §1 {@code Greeting} example, from the Python reference canonicalizer. */
  static final String GREETING_SCHEMA_ID = "8323e55c17fbbe462f811fa7842d81082263bacd";

  /** Digest of {@link PersonaSchemas#SIGNATURE_SCHEMA_JSON}, from the Python reference. */
  static final String SIGNATURE_SCHEMA_ID = "adca64c4a0622b25cc1ef3b0d28d3364a751a048";

  /** Digest of {@link PersonaSchemas#MULTI_SIGNATURE_SCHEMA_JSON}, from the Python reference. */
  static final String MULTI_SIGNATURE_SCHEMA_ID = "9af008663023932746dbef42c7d10c03fc675b5d";

  /** Digest of {@link PersonaSchemas#SIGNED_ENVELOPE_SCHEMA_JSON}, from the Python reference. */
  static final String SIGNED_ENVELOPE_SCHEMA_ID = "e898ca9bf6c41e10402f92b6a88c226a0475cf61";

  /** Digest of {@link PersonaSchemas#MULTI_SIGNED_ENVELOPE_SCHEMA_JSON}, from the Python reference. */
  static final String MULTI_SIGNED_ENVELOPE_SCHEMA_ID = "fb6a5aba28a95703d152c9620f939db3eebd0508";

  /** A schema exercising every node kind and primitive of SCHEMA.md §1. */
  static final String KITCHEN_SCHEMA_JSON = """
      {"type":"record","name":"Kitchen","namespace":"test","messageType":"test.Kitchen@v1","fields":[
        {"name":"greetingId","type":"string"},
        {"name":"from","type":"string"},
        {"name":"to","type":"string"},
        {"name":"body","type":"string"},
        {"name":"issuedAt","type":"timestamp-millis"},
        {"name":"count","type":"int"},
        {"name":"score","type":"double"},
        {"name":"verified","type":"boolean"},
        {"name":"attachment","type":"bytes"},
        {"name":"tags","type":{"type":"array","items":"string"}},
        {"name":"attributes","type":{"type":"map","values":"string"}},
        {"name":"mood","type":{"type":"enum","name":"Mood","symbols":["HAPPY","NEUTRAL","SAD"]}},
        {"name":"inner","type":{"type":"record","name":"Inner","fields":[{"name":"x","type":"long"}]}},
        {"name":"forwarded","type":"envelope"}]}
      """;

  /** The canonical JSON encoding of {@link #kitchenValue()} against {@link #KITCHEN_SCHEMA_JSON}. */
  static final String KITCHEN_CANONICAL_JSON = """
      {"attachment":"AAEC/w==","attributes":{"a":"3","z":"1","é":"2"},"body":"hello","count":42,"forwarded":{"messageType":"example.Greeting@v1","payload":"AQID","payloadFormat":"json","schemaId":"66cd4cc7f890f80cbda43fa5d69efeccadad0833","signatures":[{"algorithm":"SHA256withRSA","value":"CQg="}],"signedBy":"alice"},"from":"alice","greetingId":"g-1","inner":{"x":7},"issuedAt":"2026-10-10T12:00:00.000Z","mood":"NEUTRAL","score":1.5,"tags":["a","b"],"to":"bob","verified":true}""";

  static final PersonaSchema GREETING = SchemaParser.parse(GREETING_SCHEMA_JSON);

  static final PersonaSchema SIMPLE = SchemaParser.parse(SIMPLE_SCHEMA_JSON);

  static final PersonaSchema KITCHEN = SchemaParser.parse(KITCHEN_SCHEMA_JSON);

  private SchemaFixtures() {
  }

  /**
   * Builds a one-field record schema wrapping the given type JSON.
   *
   * @param typeJson the JSON of the field's type (a string literal or an object)
   * @return the schema document
   */
  static String singleFieldSchema(String typeJson) {
    return "{\"type\":\"record\",\"name\":\"Single\",\"fields\":[{\"name\":\"v\",\"type\":"
        + typeJson
        + "}]}";
  }

  /**
   * Parses a one-field record schema.
   *
   * @param typeJson the JSON of the field's type
   * @return the schema
   */
  static PersonaSchema singleField(String typeJson) {
    return SchemaParser.parse(singleFieldSchema(typeJson));
  }

  /**
   * The golden {@code SignedEnvelope} value used by the tests.
   *
   * @return a mutable map shaped per {@code persona.SignedEnvelope@v1}
   */
  static Map<String, Object> signedEnvelopeValue() {
    Map<String, Object> signature = new LinkedHashMap<>();
    signature.put("algorithm", "SHA256withRSA");
    signature.put("value", new byte[] {9, 8});
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("messageType", "example.Greeting@v1");
    envelope.put("payloadFormat", "json");
    envelope.put("schemaId", SIMPLE_SCHEMA_ID);
    envelope.put("payload", new byte[] {1, 2, 3});
    envelope.put("signedBy", "alice");
    envelope.put("signatures", new ArrayList<>(List.of(signature)));
    return envelope;
  }

  /**
   * The golden value for {@link #KITCHEN_SCHEMA_JSON}, covering every node kind and primitive.
   *
   * @return a mutable map
   */
  static Map<String, Object> kitchenValue() {
    Map<String, Object> inner = new LinkedHashMap<>();
    inner.put("x", 7L);
    Map<String, Object> attributes = new LinkedHashMap<>();
    // deliberately not in UTF-8 byte order: the encoders must sort them
    attributes.put("z", "1");
    attributes.put("é", "2");
    attributes.put("a", "3");
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("greetingId", "g-1");
    value.put("from", "alice");
    value.put("to", "bob");
    value.put("body", "hello");
    value.put("issuedAt", Instant.parse("2026-10-10T12:00:00.000Z"));
    value.put("count", 42);
    value.put("score", 1.5d);
    value.put("verified", true);
    value.put("attachment", new byte[] {0, 1, 2, (byte) 255});
    value.put("tags", new ArrayList<>(List.of("a", "b")));
    value.put("attributes", attributes);
    value.put("mood", "NEUTRAL");
    value.put("inner", inner);
    value.put("forwarded", signedEnvelopeValue());
    return value;
  }

  /**
   * Returns a deep copy of {@link #kitchenValue()}.
   *
   * @return a mutable map that callers may mutate
   */
  static Map<String, Object> mutableKitchenValue() {
    return deepCopy(kitchenValue());
  }

  /**
   * Deep-copies the §4.2 value model so tests can mutate a value without affecting the fixtures.
   *
   * @param value a value of the §4.2 model
   * @return an independent copy
   */
  @SuppressWarnings("unchecked")
  static <T> T deepCopy(T value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> copy = new LinkedHashMap<>();
      map.forEach((key, entry) -> copy.put((String) key, deepCopy(entry)));
      return (T) copy;
    }
    if (value instanceof List<?> list) {
      List<Object> copy = new ArrayList<>();
      list.forEach(entry -> copy.add(deepCopy(entry)));
      return (T) copy;
    }
    if (value instanceof byte[] bytes) {
      return (T) bytes.clone();
    }
    return value;
  }
}
