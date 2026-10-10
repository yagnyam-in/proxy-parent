package in.yagnyam.persona.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Helpers for the payload codec tests: deep equality over the §4.2 value model and a seeded
 * generator of random values for a schema.
 */
final class ValueSupport {

  private ValueSupport() {
  }

  /**
   * Asserts deep equality of two §4.2 values, comparing {@code byte[]} contents and {@code double}
   * bit patterns (so a lost {@code -0.0} sign is a failure).
   *
   * @param expected the expected value
   * @param actual the actual value
   */
  static void assertDeepEquals(Object expected, Object actual) {
    if (expected instanceof Map<?, ?> expectedMap) {
      assertThat(actual).isInstanceOf(Map.class);
      Map<?, ?> actualMap = (Map<?, ?>) actual;
      assertThat(actualMap.keySet()).isEqualTo(expectedMap.keySet());
      for (Map.Entry<?, ?> entry : expectedMap.entrySet()) {
        assertDeepEquals(entry.getValue(), actualMap.get(entry.getKey()));
      }
      return;
    }
    if (expected instanceof List<?> expectedList) {
      assertThat(actual).isInstanceOf(List.class);
      List<?> actualList = (List<?>) actual;
      assertThat(actualList).hasSameSizeAs(expectedList);
      for (int i = 0; i < expectedList.size(); i++) {
        assertDeepEquals(expectedList.get(i), actualList.get(i));
      }
      return;
    }
    if (expected instanceof byte[] expectedBytes) {
      assertThat(actual).isInstanceOf(byte[].class);
      assertThat((byte[]) actual).isEqualTo(expectedBytes);
      return;
    }
    if (expected instanceof Double expectedDouble) {
      assertThat(actual).isInstanceOf(Double.class);
      assertThat(Double.doubleToRawLongBits((Double) actual))
          .isEqualTo(Double.doubleToRawLongBits(expectedDouble));
      return;
    }
    assertThat(actual).isEqualTo(expected);
  }

  /**
   * Generates a random value of the given type.
   *
   * @param type the declared type
   * @param random the seeded source of randomness
   * @return a value of the §4.2 model
   */
  static Object randomValue(TypeNode type, Random random) {
    return switch (type) {
      case PrimitiveType primitive -> randomPrimitive(primitive, random);
      case EnumType enumType -> enumType.symbols().get(random.nextInt(enumType.symbols().size()));
      case ArrayType arrayType -> {
        List<Object> items = new ArrayList<>();
        int size = random.nextInt(4);
        for (int i = 0; i < size; i++) {
          items.add(randomValue(arrayType.items(), random));
        }
        yield items;
      }
      case MapType mapType -> {
        Map<String, Object> entries = new LinkedHashMap<>();
        int size = random.nextInt(4);
        for (int i = 0; i < size; i++) {
          entries.put(randomString(random, 6), randomValue(mapType.values(), random));
        }
        yield entries;
      }
      case RecordType recordType -> randomRecord(recordType, random);
      case EnvelopeType envelopeType -> randomEnvelope(envelopeType.multi(), random);
    };
  }

  /**
   * Generates a random value for every field of a record.
   *
   * @param record the record type
   * @param random the seeded source of randomness
   * @return a map with one entry per field
   */
  static Map<String, Object> randomRecord(RecordType record, Random random) {
    Map<String, Object> value = new LinkedHashMap<>();
    for (Field field : record.fields()) {
      value.put(field.name(), randomValue(field.type(), random));
    }
    return value;
  }

  /**
   * Generates a random value of a primitive type.
   *
   * @param primitive the primitive type
   * @param random the seeded source of randomness
   * @return a value of the §4.2 model
   */
  private static Object randomPrimitive(PrimitiveType primitive, Random random) {
    return switch (primitive.name()) {
      case PrimitiveType.STRING -> randomString(random, 12);
      case PrimitiveType.BOOLEAN -> random.nextBoolean();
      case PrimitiveType.INT -> random.nextInt();
      case PrimitiveType.LONG -> random.nextLong();
      case PrimitiveType.DOUBLE -> randomDouble(random);
      case PrimitiveType.BYTES -> randomBytes(random, 16);
      case PrimitiveType.TIMESTAMP_MILLIS ->
          // year 1 to year 9999, so that ISO-8601 rendering stays in the four-digit form
          Instant.ofEpochMilli(random.nextLong(-62135596800000L, 253402300800000L));
      default -> throw new IllegalStateException("unknown primitive " + primitive.name());
    };
  }

  /**
   * Generates a finite double, including subnormals, {@code -0.0} and values whose shortest decimal
   * form is long.
   *
   * @param random the seeded source of randomness
   * @return a finite double
   */
  private static double randomDouble(Random random) {
    while (true) {
      double value = Double.longBitsToDouble(random.nextLong());
      if (Double.isFinite(value)) {
        return value;
      }
    }
  }

  /**
   * Generates random bytes.
   *
   * @param random the seeded source of randomness
   * @param maxLength the exclusive upper bound on the length
   * @return the bytes
   */
  static byte[] randomBytes(Random random, int maxLength) {
    byte[] bytes = new byte[random.nextInt(maxLength + 1)];
    random.nextBytes(bytes);
    return bytes;
  }

  /**
   * Generates a random string mixing ASCII, control characters, BMP text and a supplementary
   * code point, always well-formed UTF-16.
   *
   * @param random the seeded source of randomness
   * @param maxLength the exclusive upper bound on the code point count
   * @return the string
   */
  static String randomString(Random random, int maxLength) {
    StringBuilder out = new StringBuilder();
    int length = random.nextInt(maxLength + 1);
    for (int i = 0; i < length; i++) {
      switch (random.nextInt(6)) {
        case 0 -> out.appendCodePoint(random.nextInt(0x20));
        case 1 -> out.appendCodePoint(random.nextInt(0x20, 0x7f));
        case 2 -> out.appendCodePoint(random.nextInt(0x7f, 0x800));
        case 3 -> out.appendCodePoint(random.nextInt(0x800, 0xd800));
        case 4 -> out.appendCodePoint(random.nextInt(0xe000, 0x10000));
        default -> out.appendCodePoint(random.nextInt(0x10000, 0x110000));
      }
    }
    return out.toString();
  }

  /**
   * Generates a random value of a built-in envelope type.
   *
   * @param multi {@code true} for a multi-signed envelope
   * @param random the seeded source of randomness
   * @return a map shaped per the corresponding §5 schema
   */
  private static Map<String, Object> randomEnvelope(boolean multi, Random random) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("messageType", "example." + randomString(random, 6) + "@v1");
    value.put("payloadFormat", "json");
    value.put("schemaId", SchemaFixtures.SIMPLE_SCHEMA_ID);
    value.put("payload", randomBytes(random, 8));
    if (multi) {
      List<Object> multiSignatures = new ArrayList<>();
      int count = random.nextInt(3);
      for (int i = 0; i < count; i++) {
        Map<String, Object> multiSignature = new LinkedHashMap<>();
        multiSignature.put("signedBy", randomString(random, 6));
        multiSignature.put("signatures", randomSignatures(random));
        multiSignatures.add(multiSignature);
      }
      value.put("signatures", multiSignatures);
    } else {
      value.put("signedBy", randomString(random, 6));
      value.put("signatures", randomSignatures(random));
    }
    return value;
  }

  /**
   * Generates a random list of signature records.
   *
   * @param random the seeded source of randomness
   * @return a list of maps shaped per the inline {@code Signature} record of §5
   */
  private static List<Object> randomSignatures(Random random) {
    List<Object> signatures = new ArrayList<>();
    int count = random.nextInt(3);
    for (int i = 0; i < count; i++) {
      Map<String, Object> signature = new LinkedHashMap<>();
      signature.put("algorithm", "SHA256withRSA");
      signature.put("value", randomBytes(random, 12));
      signatures.add(signature);
    }
    return signatures;
  }
}
