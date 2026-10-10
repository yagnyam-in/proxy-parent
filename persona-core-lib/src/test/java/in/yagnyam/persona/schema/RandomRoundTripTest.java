package in.yagnyam.persona.schema;

import static org.assertj.core.api.Assertions.assertThat;

import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadCodec;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Seeded pseudo-random round-trip tests (SCHEMA.md §6.2, §6.3): {@code decode(encode(v)) == v} and
 * byte determinism for both codecs over values covering every node kind and primitive.
 */
class RandomRoundTripTest {

  private static final int ITERATIONS = 200;

  private static final PayloadCodec BINARY = new BinaryPayloadCodec();

  private static final PayloadCodec JSON = new JsonPayloadCodec();

  @Test
  void binaryRoundTripsRandomValues() {
    randomRoundTrip(BINARY, 20261010L);
  }

  @Test
  void jsonRoundTripsRandomValues() {
    randomRoundTrip(JSON, 4711L);
  }

  @Test
  void bothCodecsAgreeOnTheDecodedValue() {
    Random random = new Random(99L);
    for (int i = 0; i < ITERATIONS; i++) {
      Map<String, Object> value =
          ValueSupport.randomRecord(SchemaFixtures.KITCHEN.rootRecord(), random);
      Map<String, Object> viaBinary =
          BINARY.decode(SchemaFixtures.KITCHEN, BINARY.encode(SchemaFixtures.KITCHEN, value));
      Map<String, Object> viaJson =
          JSON.decode(SchemaFixtures.KITCHEN, JSON.encode(SchemaFixtures.KITCHEN, viaBinary));

      ValueSupport.assertDeepEquals(value, viaJson);
    }
  }

  private static void randomRoundTrip(PayloadCodec codec, long seed) {
    Random random = new Random(seed);
    for (int i = 0; i < ITERATIONS; i++) {
      Map<String, Object> value =
          ValueSupport.randomRecord(SchemaFixtures.KITCHEN.rootRecord(), random);
      byte[] encoded = codec.encode(SchemaFixtures.KITCHEN, value);
      Map<String, Object> decoded = codec.decode(SchemaFixtures.KITCHEN, encoded);

      assertThat(decoded).as("%s iteration %s", codec.format(), i).isNotNull();
      ValueSupport.assertDeepEquals(value, decoded);
      assertThat(codec.encode(SchemaFixtures.KITCHEN, decoded))
          .as("%s iteration %s must be byte-deterministic", codec.format(), i)
          .isEqualTo(encoded);
    }
  }
}
