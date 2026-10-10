package in.yagnyam.persona.envelope;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.schema.PersonaSchemas;
import in.yagnyam.persona.schema.SchemaId;
import in.yagnyam.persona.schema.codec.PayloadCodec;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An envelope carrying a payload together with its type, signer and
 * signature set (protocol §6.1).
 *
 * <p>Wire shape: the built-in record {@code persona.SignedEnvelope@v1} of
 * {@code docs/SCHEMA.md} §5, fields in declared order: {@code messageType,
 * payloadFormat, schemaId, payload, signedBy, signatures}. {@link #payload()}
 * is <em>opaque bytes</em>: it was signed exactly once, and verifiers MUST
 * verify those exact bytes, never a re-serialization (protocol §6.1, §11
 * rule 10). {@link #toValue()}/{@link #fromValue(Map)} convert between this
 * record and the codec value model; {@link #encode(PayloadCodec)}/
 * {@link #decode(byte[], PayloadCodec)} convert between this record and
 * transport bytes (canonical JSON per {@code docs/SCHEMA.md} §5, or the
 * binary form of §3.2 when nested inside a binary payload).
 *
 * <p><strong>Representation of an absent {@code schemaId}.</strong> The
 * protocol types {@code schemaId} as optional (protocol §6.1) but the
 * built-in schema declares it a required string, and the canonical wire form
 * renders "schemaId empty string when absent" ({@code docs/SCHEMA.md} §5).
 * This record therefore represents absence as the <em>empty string</em>: on
 * the wire, in the value model, and in the §6.4 signing preimage (§6.4: "an
 * absent schemaId contributes an empty string to the preimage" — here, an
 * empty string contributes the empty string). The two can never diverge
 * because there is no mapping layer between them.
 *
 * <p>{@link #signedBy()} is a parsed {@link PersonaId} rather than a bare
 * string so malformed signer ids fail at materialization time (fail-closed),
 * while rendering to the canonical {@code <id>} or {@code <id>#<thumbprint>}
 * string on the wire.
 *
 * <p>{@code payload} is defensive-copied on construction and on every read.
 */
public record SignedEnvelope(
    String messageType,
    String payloadFormat,
    String schemaId,
    byte[] payload,
    PersonaId signedBy,
    List<Signature> signatures) implements Validatable {

  public SignedEnvelope {
    Objects.requireNonNull(messageType, "messageType");
    Objects.requireNonNull(payloadFormat, "payloadFormat");
    Objects.requireNonNull(schemaId, "schemaId");
    Objects.requireNonNull(payload, "payload");
    Objects.requireNonNull(signedBy, "signedBy");
    Objects.requireNonNull(signatures, "signatures");
    if (!schemaId.isEmpty()) {
      // Grammar check only (40 lowercase hex); "registered for this
      // messageType" is the verifier's step-2 concern.
      SchemaId.of(schemaId);
    }
    payload = payload.clone();
    signatures = List.copyOf(signatures);
  }

  /**
   * Defensive copy of the opaque payload bytes (protocol §6.1).
   *
   * @return a fresh copy of the exact bytes that were signed
   */
  @Override
  public byte[] payload() {
    return payload.clone();
  }

  /**
   * @return {@code true} when the envelope is structurally well-formed:
   *     non-empty {@code messageType} and {@code payloadFormat}, a valid
   *     {@code signedBy} persona id, and a non-empty list of valid
   *     signatures. The {@code schemaId} grammar (empty, i.e. absent, or 40
   *     lowercase hex characters) is enforced by the canonical constructor
   *     and therefore always holds. Version policy — the exact
   *     algorithm-set match of protocol §11 rule 3 — is asserted by the
   *     verifier as step 1 of protocol §7.3, not here.
   */
  @Override
  public boolean isValid() {
    return !messageType.isEmpty()
        && !payloadFormat.isEmpty()
        && signedBy.isValid()
        && !signatures.isEmpty()
        && signatures.stream().allMatch(Signature::isValid);
  }

  /** Structural equality over all fields, with payload compared by content. */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SignedEnvelope other)) {
      return false;
    }
    return messageType.equals(other.messageType)
        && payloadFormat.equals(other.payloadFormat)
        && schemaId.equals(other.schemaId)
        && Arrays.equals(payload, other.payload)
        && signedBy.equals(other.signedBy)
        && signatures.equals(other.signatures);
  }

  @Override
  public int hashCode() {
    int result = messageType.hashCode();
    result = 31 * result + payloadFormat.hashCode();
    result = 31 * result + schemaId.hashCode();
    result = 31 * result + Arrays.hashCode(payload);
    result = 31 * result + signedBy.hashCode();
    result = 31 * result + signatures.hashCode();
    return result;
  }

  /**
   * Never echoes the payload: payloads MAY contain PII and log lines must
   * not leak them (protocol §12).
   */
  @Override
  public String toString() {
    return "SignedEnvelope[messageType=" + messageType
        + ", payloadFormat=" + payloadFormat
        + ", schemaId=" + (schemaId.isEmpty() ? "<absent>" : schemaId)
        + ", payload=<" + payload.length + " bytes>"
        + ", signedBy=" + signedBy.uniqueId()
        + ", signatures=" + signatures + "]";
  }

  /**
   * The wire value of this envelope: the value-model map of the built-in
   * {@code persona.SignedEnvelope@v1} schema, in field-declaration order
   * ({@code docs/SCHEMA.md} §5). Suitable both for transport encoding and
   * for embedding as a nested envelope value inside a larger payload.
   *
   * @return map shaped per the built-in schema
   */
  public Map<String, Object> toValue() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("messageType", messageType);
    value.put("payloadFormat", payloadFormat);
    value.put("schemaId", schemaId);
    value.put("payload", payload.clone());
    value.put("signedBy", signedBy.uniqueId());
    value.put("signatures", signatures.stream().map(Signature::toValue).toList());
    return value;
  }

  /**
   * Materializes an envelope from a decoded wire value, failing closed on
   * any malformed shape (wrong field types, unparsable {@code signedBy}).
   *
   * @param value decoded {@code SignedEnvelope} record value
   * @return the envelope
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if a field is missing, of the wrong
   *     type, or the {@code signedBy} string is not a canonical persona id
   */
  public static SignedEnvelope fromValue(Map<String, Object> value) {
    Objects.requireNonNull(value, "value");
    EnvelopeValues.rejectUnknownFields("SignedEnvelope", value, FIELD_NAMES);
    String messageType = EnvelopeValues.requireText("SignedEnvelope",
        value.get("messageType"), "messageType");
    String payloadFormat = EnvelopeValues.requireText("SignedEnvelope",
        value.get("payloadFormat"), "payloadFormat");
    String schemaId = EnvelopeValues.requireText("SignedEnvelope",
        value.get("schemaId"), "schemaId");
    byte[] payload = EnvelopeValues.requireBytes("SignedEnvelope",
        value.get("payload"), "payload");
    String signedBy = EnvelopeValues.requireText("SignedEnvelope",
        value.get("signedBy"), "signedBy");
    List<Signature> signatures = Signature.listFromValue(
        value.get("signatures"), "signatures");
    return new SignedEnvelope(messageType, payloadFormat, schemaId, payload,
        PersonaId.of(signedBy), signatures);
  }

  /** Field names of the built-in {@code persona.SignedEnvelope@v1} schema (§5). */
  private static final java.util.Set<String> FIELD_NAMES =
      java.util.Set.of("messageType", "payloadFormat", "schemaId", "payload", "signedBy",
          "signatures");

  /**
   * Encodes this envelope with {@code codec} (transport encoding, distinct
   * from {@link #payloadFormat()}, which describes the encoding of the
   * payload <em>inside</em> the envelope). The recommended transport form is
   * canonical JSON ({@code docs/SCHEMA.md} §5) — i.e.
   * {@code encode(new JsonPayloadCodec())}.
   *
   * @param codec transport codec
   * @return the encoded envelope bytes
   */
  public byte[] encode(PayloadCodec codec) {
    Objects.requireNonNull(codec, "codec");
    return codec.encode(PersonaSchemas.signedEnvelopeSchema(), toValue());
  }

  /**
   * Decodes an envelope from transport bytes (see {@link #encode(PayloadCodec)}).
   *
   * @param bytes encoded envelope bytes
   * @param codec transport codec
   * @return the envelope
   * @throws in.yagnyam.persona.schema.codec.PayloadDecodingException if the
   *     bytes are malformed for the codec
   * @throws IllegalArgumentException if the decoded value is not a
   *     well-shaped envelope
   */
  public static SignedEnvelope decode(byte[] bytes, PayloadCodec codec) {
    Objects.requireNonNull(bytes, "bytes");
    Objects.requireNonNull(codec, "codec");
    return fromValue(codec.decode(PersonaSchemas.signedEnvelopeSchema(), bytes));
  }
}
