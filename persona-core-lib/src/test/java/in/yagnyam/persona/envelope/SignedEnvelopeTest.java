package in.yagnyam.persona.envelope;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadDecodingException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SignedEnvelope} (protocol §6.1): record invariants, the
 * canonical JSON wire form of {@code docs/SCHEMA.md} §5, encode/decode
 * round trips in both transport codecs, and fail-closed materialization of
 * malformed wire values.
 */
class SignedEnvelopeTest {

  private static final JsonPayloadCodec JSON = new JsonPayloadCodec();
  private static final BinaryPayloadCodec BINARY = new BinaryPayloadCodec();

  private static SignedEnvelope envelope() {
    return new SignedEnvelope(
        "example.Greeting@v1",
        "json",
        "0123456789012345678901234567890123456789",
        new byte[] {1, 2, 3},
        PersonaId.of("p-alice"),
        List.of(new Signature("SHA256withRSA", new byte[] {9, 8})));
  }

  @Test
  void acceptsAValidEnvelope() {
    assertThat(envelope().isValid()).isTrue();
  }

  @Test
  void rejectsStructuralProblemsInIsValid() {
    Signature validSignature = new Signature("SHA256withRSA", new byte[] {9});
    PersonaId signer = PersonaId.of("p-alice");

    assertThat(new SignedEnvelope("", "json", "", new byte[] {1}, signer,
        List.of(validSignature)).isValid()).isFalse();
    assertThat(new SignedEnvelope("example.Greeting@v1", "", "", new byte[] {1}, signer,
        List.of(validSignature)).isValid()).isFalse();
    assertThat(new SignedEnvelope("example.Greeting@v1", "json", "", new byte[] {1}, signer,
        List.of()).isValid()).isFalse();
    assertThat(new SignedEnvelope("example.Greeting@v1", "json", "", new byte[] {1}, signer,
        List.of(new Signature("", new byte[] {9}))).isValid()).isFalse();
  }

  @Test
  void rejectsNullsAtConstruction() {
    Signature signature = new Signature("SHA256withRSA", new byte[] {9});
    PersonaId signer = PersonaId.of("p-alice");
    assertThatThrownBy(() -> new SignedEnvelope(null, "json", "", new byte[] {1}, signer,
        List.of(signature)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new SignedEnvelope("example.Greeting@v1", "json", "", null, signer,
        List.of(signature)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new SignedEnvelope("example.Greeting@v1", "json", "", new byte[] {1},
        null, List.of(signature)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsMalformedSchemaIdAtConstruction() {
    assertThatThrownBy(() -> new SignedEnvelope("example.Greeting@v1", "json", "not-hex",
        new byte[] {1}, PersonaId.of("p-alice"),
        List.of(new Signature("SHA256withRSA", new byte[] {9}))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("schemaId");
  }

  @Test
  void payloadIsDefensivelyCopied() {
    byte[] payload = new byte[] {1, 2, 3};
    SignedEnvelope envelope = new SignedEnvelope("example.Greeting@v1", "json", "", payload,
        PersonaId.of("p-alice"), List.of(new Signature("SHA256withRSA", new byte[] {9})));
    payload[0] = 99;
    assertThat(envelope.payload()).containsExactly(1, 2, 3);

    envelope.payload()[0] = 99;
    assertThat(envelope.payload()).containsExactly(1, 2, 3);
  }

  @Test
  void equalsIsContentBasedIncludingThePayloadBytes() {
    SignedEnvelope first = envelope();
    SignedEnvelope same = envelope();
    SignedEnvelope differentPayload = new SignedEnvelope(
        first.messageType(), first.payloadFormat(), first.schemaId(), new byte[] {4, 5, 6},
        first.signedBy(), first.signatures());

    assertThat(first).isEqualTo(same);
    assertThat(first).hasSameHashCodeAs(same);
    assertThat(first).isNotEqualTo(differentPayload);
  }

  @Test
  void toStringRedactsPayloadAndSignatureBytes() {
    SignedEnvelope envelope = new SignedEnvelope("example.Greeting@v1", "json", "", new byte[64],
        PersonaId.of("p-alice"), List.of(new Signature("SHA256withRSA", new byte[256])));

    assertThat(envelope.toString())
        .contains("example.Greeting@v1")
        .contains("p-alice")
        .contains("64 bytes")
        .contains("<absent>")
        .doesNotContain(new String(new byte[] {1, 2, 3}, UTF_8));
  }

  @Test
  void encodesTheCanonicalJsonWireFormOfSectionFive() {
    // docs/SCHEMA.md §5: canonical JSON of the envelope record, keys sorted
    // by UTF-8 bytes, payload and signature values rendered as base64.
    String json = new String(envelope().encode(JSON), UTF_8);

    assertThat(json).isEqualTo(
        "{\"messageType\":\"example.Greeting@v1\","
            + "\"payload\":\"AQID\","
            + "\"payloadFormat\":\"json\","
            + "\"schemaId\":\"0123456789012345678901234567890123456789\","
            + "\"signatures\":[{\"algorithm\":\"SHA256withRSA\",\"value\":\"CQg=\"}],"
            + "\"signedBy\":\"p-alice\"}");
  }

  @Test
  void roundTripsThroughTheJsonTransportCodec() {
    SignedEnvelope envelope = envelope();
    assertThat(SignedEnvelope.decode(envelope.encode(JSON), JSON)).isEqualTo(envelope);
  }

  @Test
  void roundTripsThroughTheBinaryTransportCodec() {
    SignedEnvelope envelope = envelope();
    assertThat(SignedEnvelope.decode(envelope.encode(BINARY), BINARY)).isEqualTo(envelope);
  }

  @Test
  void decodeRejectsMalformedInput() {
    SignedEnvelope envelope = envelope();
    assertThatThrownBy(() -> SignedEnvelope.decode("not json".getBytes(UTF_8), JSON))
        .isInstanceOf(PayloadDecodingException.class);
    assertThatThrownBy(() -> SignedEnvelope.decode(new byte[0], JSON))
        .isInstanceOf(PayloadDecodingException.class);
    assertThatThrownBy(() -> SignedEnvelope.decode(new byte[] {1, 2, 3}, BINARY))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void decodeRejectsTrailingAndMalformedInput() {
    SignedEnvelope envelope = envelope();
    byte[] encoded = envelope.encode(JSON);
    byte[] withTrailing = new byte[encoded.length + 3];
    System.arraycopy(encoded, 0, withTrailing, 0, encoded.length);
    withTrailing[encoded.length] = 'x';

    assertThatThrownBy(() -> SignedEnvelope.decode(withTrailing, JSON))
        .isInstanceOf(PayloadDecodingException.class)
        .hasMessageContaining("not valid JSON");
    assertThatThrownBy(() -> SignedEnvelope.decode(new byte[0], JSON))
        .isInstanceOf(PayloadDecodingException.class);
  }

  @Test
  void fromValueRejectsEveryMalformedShape() {
    Map<String, Object> value = envelope().toValue();

    assertThat(SignedEnvelope.fromValue(value)).isEqualTo(envelope());

    assertThatThrownBy(() -> SignedEnvelope.fromValue(remove(value, "messageType")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("messageType");
    assertThatThrownBy(() -> SignedEnvelope.fromValue(with(value, "payload", "not-bytes")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("payload")
        .hasMessageContaining("bytes");
    assertThatThrownBy(() -> SignedEnvelope.fromValue(with(value, "signatures", "not-a-list")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("signatures")
        .hasMessageContaining("array");
    assertThatThrownBy(() -> SignedEnvelope.fromValue(with(value, "signer", "intruder")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown field");
    // A malformed signer string must fail at materialization, not at use
    assertThatThrownBy(() -> SignedEnvelope.fromValue(with(value, "signedBy", "p-bad#zz")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sha256Thumbprint");
  }

  private static Map<String, Object> remove(Map<String, Object> value, String key) {
    Map<String, Object> copy = new java.util.LinkedHashMap<>(value);
    copy.remove(key);
    return copy;
  }

  private static Map<String, Object> with(Map<String, Object> value, String key, Object entry) {
    Map<String, Object> copy = new java.util.LinkedHashMap<>(value);
    copy.put(key, entry);
    return copy;
  }
}
