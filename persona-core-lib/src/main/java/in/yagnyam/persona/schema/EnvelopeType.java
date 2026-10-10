package in.yagnyam.persona.schema;

/**
 * An {@code envelope} or {@code multi-envelope} node ({@code docs/SCHEMA.md} §1.1): a nested
 * self-describing {@code SignedEnvelope} or {@code MultiSignedEnvelope} carried inside a payload.
 *
 * <p>Canonical form (§2.2): the bare JSON string {@code "envelope"} or {@code "multi-envelope"}.
 * A value of this type is encoded as a record per the corresponding built-in schema of §5 (see
 * {@link #record()}).
 *
 * @param multi {@code false} for {@code SignedEnvelope}, {@code true} for
 *     {@code MultiSignedEnvelope}
 */
public record EnvelopeType(boolean multi) implements TypeNode {

  /** The schema keyword denoting a nested {@code SignedEnvelope} (§1.1). */
  public static final String ENVELOPE = "envelope";

  /** The schema keyword denoting a nested {@code MultiSignedEnvelope} (§1.1). */
  public static final String MULTI_ENVELOPE = "multi-envelope";

  /**
   * The schema keyword of this node.
   *
   * @return {@link #ENVELOPE} or {@link #MULTI_ENVELOPE}
   */
  public String keyword() {
    return multi ? MULTI_ENVELOPE : ENVELOPE;
  }

  /**
   * The built-in schema node that defines the wire shape of a value of this type
   * ({@code docs/SCHEMA.md} §5).
   *
   * @return the root record of {@code persona.SignedEnvelope@v1} or
   *     {@code persona.MultiSignedEnvelope@v1}
   */
  public RecordType record() {
    PersonaSchema envelope =
        multi ? PersonaSchemas.multiSignedEnvelopeSchema() : PersonaSchemas.signedEnvelopeSchema();
    return envelope.rootRecord();
  }
}
