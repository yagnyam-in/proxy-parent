package in.yagnyam.persona.envelope;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One signer's entry of a {@link MultiSignedEnvelope}: the signer together
 * with that signer's full signature set (protocol §6.2).
 *
 * <p>Wire shape: the built-in record {@code persona.MultiSignature@v1} of
 * {@code docs/SCHEMA.md} §5 with fields {@code signedBy} (string) and
 * {@code signatures} (array of {@code Signature}). Verification runs
 * independently against each entry (protocol §7.4); the quorum is counted
 * over the distinct, verified entries deduplicated by resolved certificate
 * thumbprint (protocol §6.2).
 */
public record MultiSignature(PersonaId signedBy, List<Signature> signatures) implements Validatable {

  public MultiSignature {
    Objects.requireNonNull(signedBy, "signedBy");
    Objects.requireNonNull(signatures, "signatures");
    signatures = List.copyOf(signatures);
  }

  /**
   * @return {@code true} when the entry is structurally well-formed: a
   *     valid {@code signedBy} persona id and a non-empty list of valid
   *     signatures. As with {@link SignedEnvelope}, the exact algorithm-set
   *     match (protocol §11 rule 3) is the verifier's step-1 policy check.
   */
  @Override
  public boolean isValid() {
    return signedBy.isValid()
        && !signatures.isEmpty()
        && signatures.stream().allMatch(Signature::isValid);
  }

  @Override
  public String toString() {
    return "MultiSignature[signedBy=" + signedBy.uniqueId()
        + ", signatures=" + signatures + "]";
  }

  /**
   * The wire value of this entry: the value-model map of the inline
   * {@code MultiSignature} record of the built-in envelope schemas
   * ({@code docs/SCHEMA.md} §5).
   *
   * @return map with {@code signedBy} and {@code signatures} entries
   */
  public Map<String, Object> toValue() {
    Map<String, Object> value = new java.util.LinkedHashMap<>();
    value.put("signedBy", signedBy.uniqueId());
    value.put("signatures", signatures.stream().map(Signature::toValue).toList());
    return value;
  }

  /**
   * Materializes an entry from a decoded wire value, failing closed on any
   * malformed shape.
   *
   * @param value decoded {@code MultiSignature} record value
   * @return the entry
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if a field is missing, of the wrong
   *     type, or the {@code signedBy} string is not a canonical persona id
   */
  public static MultiSignature fromValue(Map<String, Object> value) {
    Objects.requireNonNull(value, "value");
    EnvelopeValues.rejectUnknownFields("MultiSignature", value,
        java.util.Set.of("signedBy", "signatures"));
    String signedBy = EnvelopeValues.requireText("MultiSignature",
        value.get("signedBy"), "signedBy");
    List<Signature> signatures = Signature.listFromValue(
        "MultiSignature", value.get("signatures"), "signatures");
    return new MultiSignature(PersonaId.of(signedBy), signatures);
  }
}
