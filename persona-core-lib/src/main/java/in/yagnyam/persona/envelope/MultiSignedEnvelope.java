package in.yagnyam.persona.envelope;

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
 * An envelope carrying a payload signed by a quorum of personas (protocol
 * §6.2).
 *
 * <p>Wire shape: the built-in record {@code persona.MultiSignedEnvelope@v1}
 * of {@code docs/SCHEMA.md} §5, fields in declared order: {@code messageType,
 * payloadFormat, schemaId, payload, signatures}, where {@code signatures}
 * is an array of {@link MultiSignature} entries (protocol §7.4: initial
 * signing produces one entry, additional signers extend the array).
 *
 * <p>As with {@link SignedEnvelope}: {@link #payload()} is opaque bytes
 * (protocol §6.1, §11 rule 10) and an absent {@code schemaId} is represented
 * by the empty string everywhere — wire, value model and the §6.4 signing
 * preimage. The preimage domain differs ({@code persona.multi-signed-
 * envelope@v1}), which is what prevents transplanting a single-signed
 * signature into this envelope kind (protocol §6.4).
 *
 * <p>Quorum (protocol §6.2): {@code hasSufficientSignatures()} is evaluated
 * against the set of distinct, verified signer identities, deduplicated by
 * the resolved certificate thumbprint — {@code alice} and
 * {@code alice#<thumbprint>} resolving to the same certificate are one
 * signer, never two. Signature order MUST NOT affect the outcome (protocol
 * §7.4).
 *
 * <p>{@code payload} is defensive-copied on construction and on every read.
 */
public record MultiSignedEnvelope(
    String messageType,
    String payloadFormat,
    String schemaId,
    byte[] payload,
    List<MultiSignature> signatures) implements Validatable {

  public MultiSignedEnvelope {
    Objects.requireNonNull(messageType, "messageType");
    Objects.requireNonNull(payloadFormat, "payloadFormat");
    Objects.requireNonNull(schemaId, "schemaId");
    Objects.requireNonNull(payload, "payload");
    Objects.requireNonNull(signatures, "signatures");
    if (!schemaId.isEmpty()) {
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
   *     non-empty {@code messageType} and {@code payloadFormat} and a
   *     non-empty list of valid signature entries. The {@code schemaId}
   *     grammar is enforced by the canonical constructor; the exact
   *     algorithm-set match per entry (protocol §11 rule 3) is the
   *     verifier's step-1 policy check.
   */
  @Override
  public boolean isValid() {
    return !messageType.isEmpty()
        && !payloadFormat.isEmpty()
        && !signatures.isEmpty()
        && signatures.stream().allMatch(MultiSignature::isValid);
  }

  /** Structural equality over all fields, with payload compared by content. */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MultiSignedEnvelope other)) {
      return false;
    }
    return messageType.equals(other.messageType)
        && payloadFormat.equals(other.payloadFormat)
        && schemaId.equals(other.schemaId)
        && Arrays.equals(payload, other.payload)
        && signatures.equals(other.signatures);
  }

  @Override
  public int hashCode() {
    int result = messageType.hashCode();
    result = 31 * result + payloadFormat.hashCode();
    result = 31 * result + schemaId.hashCode();
    result = 31 * result + Arrays.hashCode(payload);
    result = 31 * result + signatures.hashCode();
    return result;
  }

  /**
   * Never echoes the payload (protocol §12).
   */
  @Override
  public String toString() {
    return "MultiSignedEnvelope[messageType=" + messageType
        + ", payloadFormat=" + payloadFormat
        + ", schemaId=" + (schemaId.isEmpty() ? "<absent>" : schemaId)
        + ", payload=<" + payload.length + " bytes>"
        + ", signatures=" + signatures + "]";
  }

  /**
   * The wire value of this envelope: the value-model map of the built-in
   * {@code persona.MultiSignedEnvelope@v1} schema, in field-declaration
   * order ({@code docs/SCHEMA.md} §5).
   *
   * @return map shaped per the built-in schema
   */
  public Map<String, Object> toValue() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("messageType", messageType);
    value.put("payloadFormat", payloadFormat);
    value.put("schemaId", schemaId);
    value.put("payload", payload.clone());
    value.put("signatures", signatures.stream().map(MultiSignature::toValue).toList());
    return value;
  }

  /**
   * Materializes an envelope from a decoded wire value, failing closed on
   * any malformed shape.
   *
   * @param value decoded {@code MultiSignedEnvelope} record value
   * @return the envelope
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if a field is missing, of the wrong
   *     type, or a {@code signedBy} string is not a canonical persona id
   */
  public static MultiSignedEnvelope fromValue(Map<String, Object> value) {
    Objects.requireNonNull(value, "value");
    EnvelopeValues.rejectUnknownFields("MultiSignedEnvelope", value, FIELD_NAMES);
    String messageType = EnvelopeValues.requireText("MultiSignedEnvelope",
        value.get("messageType"), "messageType");
    String payloadFormat = EnvelopeValues.requireText("MultiSignedEnvelope",
        value.get("payloadFormat"), "payloadFormat");
    String schemaId = EnvelopeValues.requireText("MultiSignedEnvelope",
        value.get("schemaId"), "schemaId");
    byte[] payload = EnvelopeValues.requireBytes("MultiSignedEnvelope",
        value.get("payload"), "payload");
    List<?> entries = EnvelopeValues.requireList("MultiSignedEnvelope",
        value.get("signatures"), "signatures");
    List<MultiSignature> signatures = entries.stream()
        .map(entry -> EnvelopeValues.requireMap("MultiSignedEnvelope", entry, "signatures"))
        .map(MultiSignature::fromValue)
        .toList();
    return new MultiSignedEnvelope(messageType, payloadFormat, schemaId, payload, signatures);
  }

  /** Field names of the built-in {@code persona.MultiSignedEnvelope@v1} schema (§5). */
  private static final java.util.Set<String> FIELD_NAMES =
      java.util.Set.of("messageType", "payloadFormat", "schemaId", "payload", "signatures");

  /**
   * Encodes this envelope with {@code codec} (transport encoding; see
   * {@link SignedEnvelope#encode(PayloadCodec)}).
   *
   * @param codec transport codec
   * @return the encoded envelope bytes
   */
  public byte[] encode(PayloadCodec codec) {
    Objects.requireNonNull(codec, "codec");
    return codec.encode(PersonaSchemas.multiSignedEnvelopeSchema(), toValue());
  }

  /**
   * Decodes an envelope from transport bytes (see
   * {@link SignedEnvelope#decode(byte[], PayloadCodec)}).
   *
   * @param bytes encoded envelope bytes
   * @param codec transport codec
   * @return the envelope
   * @throws in.yagnyam.persona.schema.codec.PayloadDecodingException if the
   *     bytes are malformed for the codec
   * @throws IllegalArgumentException if the decoded value is not a
   *     well-shaped envelope
   */
  public static MultiSignedEnvelope decode(byte[] bytes, PayloadCodec codec) {
    Objects.requireNonNull(bytes, "bytes");
    Objects.requireNonNull(codec, "codec");
    return fromValue(codec.decode(PersonaSchemas.multiSignedEnvelopeSchema(), bytes));
  }
}
