package in.yagnyam.persona.envelope;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadDecodingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MultiSignedEnvelope} and {@link MultiSignature}
 * (protocol §6.2, §7.4): record invariants, the wire form, encode/decode
 * round trips in both transport codecs, and fail-closed materialization.
 */
class MultiSignedEnvelopeTest {

  private static final JsonPayloadCodec JSON = new JsonPayloadCodec();
  private static final BinaryPayloadCodec BINARY = new BinaryPayloadCodec();

  private static MultiSignedEnvelope envelope() {
    return new MultiSignedEnvelope(
        "example.Charter@v1",
        "json",
        "",
        new byte[] {1, 2, 3},
        List.of(
            new MultiSignature(
                PersonaId.of("p-alice"),
                List.of(new Signature("SHA256withRSA", new byte[] {9, 8}))),
            new MultiSignature(
                PersonaId.of("p-bob"),
                List.of(new Signature("SHA256withRSA", new byte[] {7, 6})))));
  }

  @Test
  void acceptsAValidEnvelope() {
    assertThat(envelope().isValid()).isTrue();
  }

  @Test
  void rejectsStructuralProblemsInIsValid() {
    MultiSignature valid = new MultiSignature(
        PersonaId.of("p-alice"), List.of(new Signature("SHA256withRSA", new byte[] {9})));

    assertThat(new MultiSignedEnvelope("", "json", "", new byte[] {1}, List.of(valid))
        .isValid()).isFalse();
    assertThat(new MultiSignedEnvelope("example.Charter@v1", "", "", new byte[] {1},
        List.of(valid)).isValid()).isFalse();
    assertThat(new MultiSignedEnvelope("example.Charter@v1", "json", "", new byte[] {1},
        List.of()).isValid()).isFalse();
    assertThat(new MultiSignedEnvelope("example.Charter@v1", "json", "", new byte[] {1},
        List.of(new MultiSignature(PersonaId.of("p-alice"), List.of()))).isValid()).isFalse();
  }

  @Test
  void rejectsNullsAtConstruction() {
    MultiSignature entry = new MultiSignature(
        PersonaId.of("p-alice"), List.of(new Signature("SHA256withRSA", new byte[] {9})));
    assertThatThrownBy(() -> new MultiSignedEnvelope(null, "json", "", new byte[] {1},
        List.of(entry)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new MultiSignedEnvelope("example.Charter@v1", "json", "",
        new byte[] {1}, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void multiSignatureValidity() {
    assertThat(new MultiSignature(PersonaId.of("p-alice"),
        List.of(new Signature("SHA256withRSA", new byte[] {9}))).isValid()).isTrue();
    assertThat(new MultiSignature(PersonaId.of("p-alice"),
        List.of(new Signature("SHA256withRSA", new byte[0]))).isValid()).isFalse();
  }

  @Test
  void payloadIsDefensivelyCopied() {
    byte[] payload = new byte[] {1, 2, 3};
    MultiSignedEnvelope envelope = envelope();
    MultiSignedEnvelope withPayload = new MultiSignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(), payload,
        envelope.signatures());
    payload[0] = 99;
    assertThat(withPayload.payload()).containsExactly(1, 2, 3);
  }

  @Test
  void toStringRedactsPayloadBytes() {
    assertThat(envelope().toString())
        .contains("example.Charter@v1")
        .contains("p-alice")
        .contains("p-bob")
        .doesNotContain("AQID");
  }

  @Test
  void encodesTheCanonicalJsonWireFormOfSectionFive() {
    // docs/SCHEMA.md §5: keys sorted by UTF-8 bytes, payload and signature
    // values as base64; an absent schemaId renders as the empty string.
    String json = new String(envelope().encode(JSON), UTF_8);

    assertThat(json).isEqualTo(
        "{\"messageType\":\"example.Charter@v1\","
            + "\"payload\":\"AQID\","
            + "\"payloadFormat\":\"json\","
            + "\"schemaId\":\"\","
            + "\"signatures\":[{"
            + "\"signatures\":[{\"algorithm\":\"SHA256withRSA\",\"value\":\"CQg=\"}],"
            + "\"signedBy\":\"p-alice\"},{"
            + "\"signatures\":[{\"algorithm\":\"SHA256withRSA\",\"value\":\"BwY=\"}],"
            + "\"signedBy\":\"p-bob\"}]}");
  }

  @Test
  void roundTripsThroughBothTransportCodecs() {
    MultiSignedEnvelope envelope = envelope();
    assertThat(MultiSignedEnvelope.decode(envelope.encode(JSON), JSON)).isEqualTo(envelope);
    assertThat(MultiSignedEnvelope.decode(envelope.encode(BINARY), BINARY)).isEqualTo(envelope);
  }

  @Test
  void decodeRejectsMalformedInput() {
    assertThatThrownBy(() -> MultiSignedEnvelope.decode("not json".getBytes(UTF_8), JSON))
        .isInstanceOf(PayloadDecodingException.class);
  }

  @Test
  void fromValueRejectsEveryMalformedShape() {
    Map<String, Object> value = envelope().toValue();
    assertThat(MultiSignedEnvelope.fromValue(value)).isEqualTo(envelope());

    Map<String, Object> withoutSignatures = new LinkedHashMap<>(value);
    withoutSignatures.remove("signatures");
    assertThatThrownBy(() -> MultiSignedEnvelope.fromValue(withoutSignatures))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("signatures");

    Map<String, Object> withBadEntries = new LinkedHashMap<>(value);
    withBadEntries.put("signatures", List.of("not-a-record"));
    assertThatThrownBy(() -> MultiSignedEnvelope.fromValue(withBadEntries))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("record");

    Map<String, Object> withBadSigner = new LinkedHashMap<>(value);
    withBadSigner.put("signatures", List.of(Map.of(
        "signedBy", "p-bad#zz",
        "signatures", List.of(Map.of("algorithm", "SHA256withRSA", "value", new byte[] {1})))));
    assertThatThrownBy(() -> MultiSignedEnvelope.fromValue(withBadSigner))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sha256Thumbprint");

    Map<String, Object> withUnknownField = new LinkedHashMap<>(value);
    withUnknownField.put("extra", "field");
    assertThatThrownBy(() -> MultiSignedEnvelope.fromValue(withUnknownField))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown field");
  }
}
