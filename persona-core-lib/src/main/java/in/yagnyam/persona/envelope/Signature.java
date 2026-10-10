package in.yagnyam.persona.envelope;

import in.yagnyam.persona.Validatable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One concrete signature of a signature set (protocol §6.1).
 *
 * <p>Wire shape: the built-in record {@code persona.Signature@v1} of
 * {@code docs/SCHEMA.md} §5 with fields {@code algorithm} (string) and
 * {@code value} (bytes; base64 in textual encodings). A single logical
 * signature is a <em>set</em> of these records, one per algorithm of the
 * active algorithm set (protocol §6.1 design rule 3); the set as a whole
 * MUST match one of the sets declared by the active Persona Version
 * (protocol §11 rule 3).
 *
 * <p>The algorithm name is a caller-supplied wire string; accepted values
 * are pinned by the active {@code PersonaVersion}, never here.
 *
 * <p>{@code value} is defensive-copied on construction and on every read, so
 * a {@code Signature} value cannot be mutated through a shared reference.
 */
public record Signature(String algorithm, byte[] value) implements Validatable {

  public Signature {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(value, "value");
    value = value.clone();
  }

  /**
   * Defensive copy of the signature bytes.
   *
   * @return a fresh copy of the signature value
   */
  @Override
  public byte[] value() {
    return value.clone();
  }

  /**
   * @return {@code true} when the algorithm name and the signature bytes
   *     are both non-empty. Membership of {@link #algorithm()} in a
   *     version-sanctioned algorithm set is a policy check performed by the
   *     signer and verifier (protocol §7.3 step 1), not a structural one.
   */
  @Override
  public boolean isValid() {
    return !algorithm.isEmpty() && value.length > 0;
  }

  /** Structural equality over algorithm and signature bytes. */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Signature other)) {
      return false;
    }
    return algorithm.equals(other.algorithm) && Arrays.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    int result = algorithm.hashCode();
    result = 31 * result + Arrays.hashCode(value);
    return result;
  }

  @Override
  public String toString() {
    return "Signature[algorithm=" + algorithm + ", value=<" + value.length + " bytes>]";
  }

  /**
   * The wire value of this signature: the value-model map of the inline
   * {@code Signature} record of the built-in envelope schemas
   * ({@code docs/SCHEMA.md} §5).
   *
   * @return map with {@code algorithm} and {@code value} entries
   */
  public Map<String, Object> toValue() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("algorithm", algorithm);
    value.put("value", this.value.clone());
    return value;
  }

  /**
   * Materializes a signature from a decoded wire value, failing closed on
   * any malformed shape.
   *
   * @param value decoded {@code Signature} record value
   * @return the signature
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if a field is missing or of the wrong
   *     type
   */
  public static Signature fromValue(Map<String, Object> value) {
    Objects.requireNonNull(value, "value");
    String algorithm = EnvelopeValues.requireText("Signature", value.get("algorithm"), "algorithm");
    byte[] signatureBytes = EnvelopeValues.requireBytes("Signature", value.get("value"), "value");
    return new Signature(algorithm, signatureBytes);
  }

  /**
   * Materializes a signature list from a decoded wire value.
   *
   * @param value decoded {@code signatures} array value
   * @param field name of the field the array was read from, used in error
   *              messages
   * @return the signatures in wire order
   * @throws IllegalArgumentException if the value is not a list of
   *     well-shaped signature records
   */
  static List<Signature> listFromValue(Object value, String field) {
    List<?> items = EnvelopeValues.requireList("SignedEnvelope", value, field);
    return items.stream()
        .map(item -> EnvelopeValues.requireMap("SignedEnvelope", item, field))
        .map(Signature::fromValue)
        .toList();
  }
}
