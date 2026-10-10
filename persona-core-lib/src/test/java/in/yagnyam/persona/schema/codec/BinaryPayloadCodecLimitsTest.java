package in.yagnyam.persona.schema.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.schema.ArrayType;
import in.yagnyam.persona.schema.Field;
import in.yagnyam.persona.schema.MapType;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.PrimitiveType;
import in.yagnyam.persona.schema.RecordType;
import in.yagnyam.persona.schema.SchemaParser;
import in.yagnyam.persona.schema.TypeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SCHEMA.md §3.4 resource limits of {@link BinaryPayloadCodec}: the collection cap
 * on the writer and reader side, the cumulative-across-blocks enforcement, the zero-progress
 * rejection that closes the zero-field-record DoS, and the nesting depth limit.
 *
 * <p>These tests live in this package because the custom-cap constructor is package-private by
 * design: the limits are testable at a small cap without allocating multi-megabyte payloads. The
 * nesting-boundary schemas are built through the model classes rather than parsed, because a
 * 1000-level type tree is deeper than a JSON parser should be fed.
 */
class BinaryPayloadCodecLimitsTest {

  private static final int DEFAULT_CAP = BinaryPayloadCodec.DEFAULT_MAX_COLLECTION_ELEMENTS;

  private final BinaryPayloadCodec codec = new BinaryPayloadCodec();

  @Test
  void arrayAtTheDefaultCapEncodesAndDecodes() {
    PersonaSchema schema = singleField("{\"type\":\"array\",\"items\":\"boolean\"}");
    List<Boolean> items = Collections.nCopies(DEFAULT_CAP, Boolean.TRUE);

    byte[] encoded = codec.encode(schema, Map.of("v", items));

    // block count 2^20 is the 4-byte varint 80 80 80 01, then one byte per item, then terminator
    assertThat(encoded).hasSize(DEFAULT_CAP + 4 + 1);
    assertThat(encoded[0]).isEqualTo((byte) 0x80);
    assertThat(encoded[1]).isEqualTo((byte) 0x80);
    assertThat(encoded[2]).isEqualTo((byte) 0x80);
    assertThat(encoded[3]).isEqualTo((byte) 0x01);
    assertThat(encoded[DEFAULT_CAP + 4]).isEqualTo((byte) 0x00);

    Map<String, Object> decoded = codec.decode(schema, encoded);
    assertThat((List<?>) decoded.get("v")).hasSize(DEFAULT_CAP);
  }

  @Test
  void arrayAboveTheDefaultCapIsRejectedAtEncode() {
    PersonaSchema schema = singleField("{\"type\":\"array\",\"items\":\"boolean\"}");
    List<Boolean> items = Collections.nCopies(DEFAULT_CAP + 1, Boolean.TRUE);

    assertThatThrownBy(() -> codec.encode(schema, Map.of("v", items)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("field 'v'")
        .hasMessageContaining("more than the maximum of " + DEFAULT_CAP);
  }

  @Test
  void mapCapIsEnforcedAtEncode() {
    PersonaSchema schema = singleField("{\"type\":\"map\",\"values\":\"int\"}");
    BinaryPayloadCodec small = new BinaryPayloadCodec(8);

    assertThatThrownBy(() -> small.encode(schema, Map.of("v", mapOf(9))))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("field 'v'")
        .hasMessageContaining("more than the maximum of 8");

    // exactly at the cap is fine
    Map<String, Object> atCap = mapOf(8);
    Map<String, Object> decoded = small.decode(schema, small.encode(schema, Map.of("v", atCap)));
    assertThat(decoded.get("v")).isEqualTo(atCap);
  }

  @Test
  void capIsCumulativeAcrossBlocks() {
    PersonaSchema schema = singleField("{\"type\":\"array\",\"items\":\"int\"}");
    BinaryPayloadCodec small = new BinaryPayloadCodec(8);

    // two blocks of 5 items each: the second block must be rejected by the running total
    // (0x0a = zigzag(5), then an int 0 per item; rejection happens at the second block count)
    assertThatThrownBy(() -> small.decode(schema, bytes(0x0a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0a)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("cumulative element count 10 exceeds the implementation limit of 8");

    // blocks summing exactly to the cap are fine (block of 5, then a block of 3)
    Map<String, Object> decoded =
        small.decode(
            schema,
            bytes(0x0a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x00));
    assertThat(decoded.get("v")).isEqualTo(Collections.nCopies(8, 0));
  }

  @Test
  void mapCapIsCumulativeAcrossBlocks() {
    PersonaSchema schema = singleField("{\"type\":\"map\",\"values\":\"int\"}");
    BinaryPayloadCodec small = new BinaryPayloadCodec(8);

    // block of 5 entries (key "a".."e", int 0 each: 02 <char> 00), then a second block of 5
    assertThatThrownBy(
            () ->
                small.decode(
                    schema,
                    bytes(
                        0x0a,
                        0x02, 0x61, 0x00,
                        0x02, 0x62, 0x00,
                        0x02, 0x63, 0x00,
                        0x02, 0x64, 0x00,
                        0x02, 0x65, 0x00,
                        0x0a)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("cumulative element count 10 exceeds the implementation limit of 8");
  }

  @Test
  void zeroFieldRecordDosPayloadIsRejectedEagerly() {
    PersonaSchema schema =
        singleField(
            "{\"type\":\"array\",\"items\":"
                + "{\"type\":\"record\",\"name\":\"Empty\",\"fields\":[]}}");

    // the review's DoS payload: a block count of 2^20 (varint 80 80 80 01) whose items are
    // zero-field records costing 0 bytes each, repeated so the allocation is unbounded
    List<Integer> payloadBytes = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      payloadBytes.add(0x80);
      payloadBytes.add(0x80);
      payloadBytes.add(0x80);
      payloadBytes.add(0x01);
    }
    payloadBytes.add(0x00);
    byte[] payload = toByteArray(payloadBytes);

    // the default codec rejects on the first item: an element that consumes no byte is
    // zero progress, so the rejection is O(1) rather than 2^20 allocations
    long start = System.nanoTime();
    assertThatThrownBy(() -> codec.decode(schema, payload))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("without consuming any input byte");
    assertThat(System.nanoTime() - start).isLessThan(10_000_000_000L);

    // a small-cap codec rejects even earlier, at the oversized block count
    BinaryPayloadCodec small = new BinaryPayloadCodec(8);
    assertThatThrownBy(() -> small.decode(schema, payload))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("exceeds the implementation limit of 8");
  }

  @Test
  void boundedArrayOfZeroFieldRecordsIsRejectedToo() {
    PersonaSchema schema =
        singleField(
            "{\"type\":\"array\",\"items\":"
                + "{\"type\":\"record\",\"name\":\"Empty\",\"fields\":[]}}");

    // a count of 2 zero-field records is bounded and carries no information; it is rejected by
    // the same zero-progress rule, so the strictness is not size-dependent
    assertThatThrownBy(() -> codec.decode(schema, bytes(0x04, 0x00)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("without consuming any input byte");
  }

  @Test
  void singleZeroFieldRecordValueStillDecodes() {
    PersonaSchema schema = singleField("{\"type\":\"record\",\"name\":\"Empty\",\"fields\":[]}");

    // the strictness is scoped to collections: a lone zero-field record round-trips fine
    byte[] encoded = codec.encode(schema, Map.of("v", Map.of()));
    assertThat(encoded).isEmpty();
    assertThat(codec.decode(schema, encoded)).isEqualTo(Map.of("v", Map.of()));
  }

  @Test
  void cyclicValueGraphIsRejectedAtEncode() {
    PersonaSchema mapOfMaps =
        singleField("{\"type\":\"map\",\"values\":{\"type\":\"map\",\"values\":\"int\"}}");
    PersonaSchema arrayOfArrays =
        singleField("{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":\"int\"}}");
    Map<String, Object> cyclicMap = new LinkedHashMap<>();
    cyclicMap.put("a", cyclicMap);
    List<Object> cyclicList = new ArrayList<>();
    cyclicList.add(cyclicList);

    // a cycle through a shallow schema is cut off by the schema's own depth (the leaf type
    // mismatches the map): a PayloadEncodingException, never a StackOverflowError
    assertThatThrownBy(() -> codec.encode(mapOfMaps, Map.of("v", cyclicMap)))
        .isInstanceOf(PayloadEncodingException.class);
    assertThatThrownBy(() -> codec.encode(arrayOfArrays, Map.of("v", cyclicList)))
        .isInstanceOf(PayloadEncodingException.class);

    // a cycle through a schema deeper than the nesting limit is caught by the §3.4 depth cap
    Map<String, Object> cyclicDeep = new LinkedHashMap<>();
    cyclicDeep.put("a", cyclicDeep);
    assertThatThrownBy(() -> codec.encode(singleFieldOfMaps(1500), Map.of("v", cyclicDeep)))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("nested more than " + BinaryPayloadCodec.MAX_NESTING_DEPTH);
  }

  @Test
  void nestingAtTheLimitEncodesAndDeeperNestingIsRejected() {
    int limit = BinaryPayloadCodec.MAX_NESTING_DEPTH;
    PersonaSchema atLimit = singleFieldOfMaps(limit);
    PersonaSchema beyond = singleFieldOfMaps(limit + 1);

    // exactly 1000 nested maps encode and decode
    Map<String, Object> atLimitValue = nestedMapValue(limit);
    Map<String, Object> decoded =
        codec.decode(atLimit, codec.encode(atLimit, Map.of("v", atLimitValue)));
    assertThat(decoded.get("v")).isEqualTo(atLimitValue);

    // 1001 nested maps are rejected on encode
    assertThatThrownBy(() -> codec.encode(beyond, Map.of("v", nestedMapValue(limit + 1))))
        .isInstanceOf(PayloadEncodingException.class)
        .hasMessageContaining("nested more than " + limit);
  }

  @Test
  void deeplyNestedInputIsRejectedOnDecode() {
    PersonaSchema atLimit = singleFieldOfArrays(1000);
    PersonaSchema beyond = singleFieldOfArrays(1002);

    // 1000 nested arrays of one item each decode fine (02 = zigzag(1), 01 = true, 00 = terminator)
    assertThat(codec.decode(atLimit, nestedArrayBytes(1000))).isNotNull();

    // 1002 nested arrays are rejected by the reader-side depth limit instead of a stack overflow
    assertThatThrownBy(() -> codec.decode(beyond, nestedArrayBytes(1002)))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("nested more than " + BinaryPayloadCodec.MAX_NESTING_DEPTH);
  }

  /**
   * Builds a one-field record schema whose field is {@code depth} nested map levels over an int.
   */
  private static PersonaSchema singleFieldOfMaps(int depth) {
    TypeNode type = new PrimitiveType("int");
    for (int i = 0; i < depth; i++) {
      type = new MapType(type);
    }
    return singleFieldOf(type);
  }

  /**
   * Builds a one-field record schema whose field is {@code depth} nested array levels over a
   * boolean.
   */
  private static PersonaSchema singleFieldOfArrays(int depth) {
    TypeNode type = new PrimitiveType("boolean");
    for (int i = 0; i < depth; i++) {
      type = new ArrayType(type);
    }
    return singleFieldOf(type);
  }

  /** Builds a one-field record schema around a ready-made type node. */
  private static PersonaSchema singleFieldOf(TypeNode type) {
    return new PersonaSchema(
        new RecordType(
            "Single", Optional.empty(), Optional.empty(), List.of(new Field("v", type))));
  }

  /** Builds a value nested {@code depth} map levels deep around an int. */
  private static Map<String, Object> nestedMapValue(int depth) {
    Object value = 0;
    for (int i = 0; i < depth; i++) {
      value = Map.of("k", value);
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> outer = (Map<String, Object>) value;
    return outer;
  }

  /** Builds a payload of {@code depth} arrays nested inside each other, each holding one item. */
  private static byte[] nestedArrayBytes(int depth) {
    // innermost: one boolean true; every outer level: count 1, inner content, terminator
    byte[] payload = new byte[2 * depth + 1];
    for (int i = 0; i < depth - 1; i++) {
      payload[i] = 0x02;
    }
    payload[depth - 1] = 0x02;
    payload[depth] = 0x01;
    payload[depth + 1] = 0x00;
    for (int i = 0; i < depth - 1; i++) {
      payload[depth + 2 + i] = 0x00;
    }
    return payload;
  }

  /** Parses a one-field record schema wrapping the given type JSON. */
  private static PersonaSchema singleField(String typeJson) {
    return SchemaParser.parse(
        "{\"type\":\"record\",\"name\":\"Single\",\"fields\":[{\"name\":\"v\",\"type\":"
            + typeJson
            + "}]}");
  }

  /** Builds a map with {@code count} entries k0..k(count-1), each mapped to its index. */
  private static Map<String, Object> mapOf(int count) {
    Map<String, Object> entries = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) {
      entries.put("k" + i, i);
    }
    return entries;
  }

  private static byte[] bytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return bytes;
  }

  private static byte[] toByteArray(List<Integer> values) {
    byte[] bytes = new byte[values.size()];
    for (int i = 0; i < values.size(); i++) {
      bytes[i] = (byte) values.get(i).intValue();
    }
    return bytes;
  }
}
