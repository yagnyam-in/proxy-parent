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
 * <p>The certified identity is the certificate: when {@link #certificate()} is
 * non-null, the canonical constructor requires {@link #id()} to equal
 * {@link PersonaId#of(PersonaCertificate)} and the public-key fields to equal
 * the certificate's, throwing {@link IllegalArgumentException} otherwise — a
 * Persona claiming an id or a public key its certificate does not certify is
 * not representable. Only {@link #name()} is a free display label
 * (protocol §3.1; {@code SPEC.md} §3.1.1).
 *
 * <p>Absent ({@code null}) components remain representable (the record stays a
 * wire-tolerant data carrier) but never contradict the certificate, and
 * {@link #isValid()} rejects the record.
 */
public record Persona(
    PersonaId id,
    String name,
    PersonaCertificate certificate,
    String publicKeyEncoded,
    String publicKeySha256Thumbprint) implements Validatable {

  /**
   * Binds {@link #id()} and the public-key fields to the certificate
   * (see the class javadoc).
   *
   * @throws IllegalArgumentException when the id or a non-null public-key
   *     field does not match the certificate's certified identity
   */
  public Persona {
    if (certificate != null) {
      PersonaId certifiedId = certifiedIdOf(certificate);
      if (id != null && certifiedId != null && !id.equals(certifiedId)) {
        throw new IllegalArgumentException("Persona id " + id.uniqueId()
            + " does not match its certificate (expected " + certifiedId.uniqueId()
            + "; protocol §3.1: the certified identity is the certificate owner plus"
            + " thumbprint, and only the display name is a free label)");
      }
      requireCertificateField("publicKeyEncoded", publicKeyEncoded, certificate.publicKeyPem());
      requireCertificateField("publicKeySha256Thumbprint", publicKeySha256Thumbprint,
          certificate.publicKeySha256Thumbprint());
    }
  }

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
   * @return {@code true} when the id is valid and bound to the certificate
   *     (certified id, certified public key — see the class javadoc), the
   *     certificate is valid and the public key material is present
   *     ({@code SPEC.md} §3.1.1; the name is a human-readable label and is
   *     deliberately not required)
   */
  @Override
  public boolean isValid() {
    return id != null
        && id.isValid()
        && certificate != null
        && certificate.isValid()
        && isPresent(publicKeyEncoded)
        && isPresent(publicKeySha256Thumbprint)
        && id.equals(certifiedIdOf(certificate))
        && publicKeyEncoded.equals(certificate.publicKeyPem())
        && publicKeySha256Thumbprint.equals(certificate.publicKeySha256Thumbprint());
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

  /**
   * The <em>certified</em> id of {@code certificate}: owner (subject CN)
   * pinned to the certificate thumbprint, or {@code null} when the certificate
   * record carries no usable certified identity (in which case the record is
   * invalid, never trusted — see {@link #isValid()}).
   */
  private static PersonaId certifiedIdOf(PersonaCertificate certificate) {
    if (certificate == null
        || certificate.owner() == null
        || certificate.owner().isEmpty()
        || !PersonaId.isCanonicalThumbprint(certificate.sha256Thumbprint())) {
      return null;
    }
    return PersonaId.of(certificate.owner(), certificate.sha256Thumbprint());
  }

  /**
   * @param field    field name, named in the failure message
   * @param actual   field value as supplied by the caller
   * @param expected value taken from the certificate
   * @throws IllegalArgumentException when {@code actual} is non-null and
   *     differs from {@code expected}
   */
  private static void requireCertificateField(String field, String actual, String expected) {
    if (actual != null && !actual.equals(expected)) {
      throw new IllegalArgumentException("Persona " + field
          + " does not match its certificate (protocol §3.1 fact 3: the certificate certifies"
          + " the public key): field is "
          + describe(actual) + ", certificate-derived value is " + describe(expected));
    }
  }

  /**
   * Renders a field for a failure message without echoing PEM bodies.
   */
  private static String describe(String value) {
    if (value == null) {
      return "null";
    }
    return value.length() > 64 ? "<" + value.length() + "-character string>" : "'" + value + "'";
  }
}
