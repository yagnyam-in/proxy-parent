package in.yagnyam.persona;

import java.util.Objects;

/**
 * A pseudonymous identity: persona id, human-readable name, certificate and
 * public key material (protocol §3.1; {@code SPEC.md} §3.1.1, was
 * {@code Proxy}).
 *
 * <p>A persona is authoritative because its certificate is authoritative. Two
 * personas with the same id but different certificate thumbprints are
 * distinct instances (e.g. after re-keying) and can be disambiguated via the
 * pinned {@link PersonaId}.
 *
 * <p>The canonical constructor is permissive (a wire-tolerant data carrier):
 * {@link #isValid()} is the validator, per {@code SPEC.md} §3.1.1.
 */
public record Persona(
    PersonaId id,
    String name,
    PersonaCertificate certificate,
    String publicKeyEncoded,
    String publicKeySha256Thumbprint) implements Validatable {

  /**
   * Derives a persona from its certificate: id pinned to the certificate
   * (owner + thumbprint), name mirroring the certificate owner, public key
   * PEM and public-key thumbprint from the certificate.
   *
   * @param certificate persona certificate, never {@code null}
   * @return derived persona
   */
  public static Persona of(PersonaCertificate certificate) {
    Objects.requireNonNull(certificate, "certificate");
    return new Persona(
        PersonaId.of(certificate),
        certificate.owner(),
        certificate,
        certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
  }

  /**
   * @return canonical {@link PersonaId#uniqueId()} form, or {@code null}
   *     when the id is absent
   */
  public String getUniqueId() {
    return id == null ? null : id.uniqueId();
  }

  /**
   * @return {@code true} when the id is valid, the certificate is valid and
   *     the public key material is present ({@code SPEC.md} §3.1.1; the name
   *     is a human-readable label and is deliberately not required)
   */
  @Override
  public boolean isValid() {
    return id != null
        && id.isValid()
        && certificate != null
        && certificate.isValid()
        && isPresent(publicKeyEncoded)
        && isPresent(publicKeySha256Thumbprint);
  }

  @Override
  public String toString() {
    return "Persona[id=" + (id == null ? null : id.uniqueId())
        + ", name=" + name
        + ", certificateThumbprint=" + (certificate == null ? null : certificate.sha256Thumbprint())
        + ", publicKeySha256Thumbprint=" + publicKeySha256Thumbprint + "]";
  }

  /**
   * Protocol §3.1 equality: two personas are equal iff they share both
   * {@code id} and certificate thumbprint. {@link PersonaCertificate}
   * equality already excludes the decoded certificate handle (the
   * thumbprint is the certificate identity), so comparing
   * {@code (id, certificate)} is exactly that rule. The display
   * {@link #name} is a human-readable label and is deliberately excluded
   * (so is the public-key material, which a certificate already pins);
   * two personas with the same id and certificate are the same persona
   * regardless of what each side happens to call it.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Persona other)) {
      return false;
    }
    return Objects.equals(id, other.id) && Objects.equals(certificate, other.certificate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, certificate);
  }

  private static boolean isPresent(String value) {
    return value != null && !value.isEmpty();
  }
}
