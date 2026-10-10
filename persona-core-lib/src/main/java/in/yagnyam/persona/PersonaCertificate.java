package in.yagnyam.persona;

import in.yagnyam.persona.crypto.Digests;
import in.yagnyam.persona.crypto.PemService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Objects;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

/**
 * X.509 certificate binding a persona id (subject CN) to a public key
 * (protocol §3.1 facts 2-4, §3.2; {@code SPEC.md} §3.1.3, was
 * {@code Certificate}).
 *
 * <p>The certificate identity is its {@link #sha256Thumbprint()} — the
 * lowercase-hex SHA-256 digest of the DER encoding — so the
 * {@link #certificate()} field is excluded from {@link #equals(Object)} and
 * {@link #hashCode()}; it is carried along for verification use.
 *
 * <p>The certificate handle is authoritative for every security-relevant
 * field (protocol §3.1 facts 2-4): when {@link #certificate()} is non-null,
 * the canonical constructor verifies that each field equals the value derived
 * from that handle and throws {@link IllegalArgumentException} naming the
 * field otherwise. Metadata that contradicts the handle — an extended
 * {@code validTill}, a different {@code owner}, a foreign
 * {@code sha256Thumbprint} — is therefore not representable, so validation
 * (which trusts the metadata) can never disagree with chain verification
 * (which uses the handle).
 *
 * <p>Absent ({@code null}) fields are still representable — the record stays a
 * wire-tolerant data carrier — but they never contradict the handle and
 * {@link #isValid()} rejects the record, so nothing downstream can consume
 * them as trusted facts.
 */
public record PersonaCertificate(
    X509Certificate certificate,
    String serialNumber,
    String owner,
    String sha256Thumbprint,
    Instant validFrom,
    Instant validTill,
    String certificatePem,
    String publicKeyPem,
    String publicKeySha256Thumbprint) implements Validatable {

  /**
   * Binds every metadata field to the certificate handle (see the class
   * javadoc): a non-null field that contradicts the handle is rejected here,
   * before any consumer can trust it.
   *
   * @throws IllegalArgumentException when a non-null field differs from the
   *     value derived from {@link #certificate()} (the field name is in the
   *     message), or when the handle cannot be encoded at all
   */
  public PersonaCertificate {
    if (certificate != null) {
      // Parameters, not fields: a compact constructor's field assignments
      // happen after this body, so reading the fields here would read null.
      requireHandleField("serialNumber", serialNumber, certificate.getSerialNumber().toString());
      requireHandleField("owner", owner, commonName(certificate.getSubjectX500Principal()));
      requireHandleField("validFrom", validFrom, certificate.getNotBefore().toInstant());
      requireHandleField("validTill", validTill, certificate.getNotAfter().toInstant());
      try {
        PemService pemService = PemService.instance();
        requireHandleField("sha256Thumbprint", sha256Thumbprint,
            Digests.sha256Hex(certificate.getEncoded()));
        requireHandleField("certificatePem", certificatePem,
            pemService.encodeCertificate(certificate));
        requireHandleField("publicKeyPem", publicKeyPem,
            pemService.encodePublicKey(certificate.getPublicKey()));
        requireHandleField("publicKeySha256Thumbprint", publicKeySha256Thumbprint,
            Digests.sha256Hex(certificate.getPublicKey().getEncoded()));
      } catch (GeneralSecurityException | IOException e) {
        throw new IllegalArgumentException(
            "Unable to derive the fields of this record from its certificate handle", e);
      }
    }
  }

  /**
   * Derives every field from {@code certificate}: owner = subject CN,
   * serialNumber = decimal serial, sha256Thumbprint = SHA-256 hex of the DER
   * encoding, validity window from notBefore/notAfter, plus PEM encodings and
   * the public-key thumbprint.
   *
   * @param certificate X.509 certificate, never {@code null}
   * @return derived certificate record
   */
  public static PersonaCertificate of(X509Certificate certificate) {
    Objects.requireNonNull(certificate, "certificate");
    try {
      PemService pemService = PemService.instance();
      return new PersonaCertificate(
          certificate,
          certificate.getSerialNumber().toString(),
          commonName(certificate.getSubjectX500Principal()),
          Digests.sha256Hex(certificate.getEncoded()),
          certificate.getNotBefore().toInstant(),
          certificate.getNotAfter().toInstant(),
          pemService.encodeCertificate(certificate),
          pemService.encodePublicKey(certificate.getPublicKey()),
          Digests.sha256Hex(certificate.getPublicKey().getEncoded()));
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("Unable to derive certificate fields", e);
    }
  }

  /**
   * Parses a PEM encoded certificate (protocol §9.1: certificates travel as
   * PEM).
   *
   * @param certificatePem PEM encoded certificate
   * @return derived certificate record
   * @throws IllegalArgumentException if the input is not a valid certificate
   *     PEM
   */
  public static PersonaCertificate parsePem(String certificatePem) {
    Objects.requireNonNull(certificatePem, "certificatePem");
    try {
      return of(PemService.instance().decodeCertificate(certificatePem));
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalArgumentException("Invalid certificate PEM", e);
    }
  }

  /**
   * Encodes {@code certificate} as PEM.
   *
   * @param certificate X.509 certificate
   * @return PEM string
   * @throws IllegalStateException if encoding fails
   */
  public static String encodePem(X509Certificate certificate) {
    Objects.requireNonNull(certificate, "certificate");
    try {
      return PemService.instance().encodeCertificate(certificate);
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("Unable to encode certificate", e);
    }
  }

  /**
   * @return {@code true} when all string fields are present, the validity
   *     window is ordered ({@code validFrom <= validTill}) and the underlying
   *     {@link X509Certificate} handle is present
   */
  @Override
  public boolean isValid() {
    return certificate != null
        && isPresent(serialNumber)
        && isPresent(owner)
        && isPresent(sha256Thumbprint)
        && validFrom != null
        && validTill != null
        && !validFrom.isAfter(validTill)
        && isPresent(certificatePem)
        && isPresent(publicKeyPem)
        && isPresent(publicKeySha256Thumbprint);
  }

  /**
   * Structural equality over all fields except the {@link #certificate()}
   * handle — the thumbprint is the certificate identity (protocol §3.1: two
   * personas are equal iff they share id and certificate thumbprint).
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PersonaCertificate other)) {
      return false;
    }
    return Objects.equals(serialNumber, other.serialNumber)
        && Objects.equals(owner, other.owner)
        && Objects.equals(sha256Thumbprint, other.sha256Thumbprint)
        && Objects.equals(validFrom, other.validFrom)
        && Objects.equals(validTill, other.validTill)
        && Objects.equals(certificatePem, other.certificatePem)
        && Objects.equals(publicKeyPem, other.publicKeyPem)
        && Objects.equals(publicKeySha256Thumbprint, other.publicKeySha256Thumbprint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serialNumber, owner, sha256Thumbprint, validFrom, validTill,
        certificatePem, publicKeyPem, publicKeySha256Thumbprint);
  }

  @Override
  public String toString() {
    return "PersonaCertificate[serialNumber=" + serialNumber
        + ", owner=" + owner
        + ", sha256Thumbprint=" + sha256Thumbprint + "]";
  }

  private static boolean isPresent(String value) {
    return value != null && !value.isEmpty();
  }

  /**
   * @param field    field name, named in the failure message
   * @param actual   field value as supplied by the caller
   * @param expected value derived from the certificate handle
   * @throws IllegalArgumentException when {@code actual} is non-null and
   *     differs from {@code expected}
   */
  private static void requireHandleField(String field, Object actual, Object expected) {
    if (actual != null && !actual.equals(expected)) {
      throw new IllegalArgumentException("PersonaCertificate " + field
          + " does not match the certificate handle (protocol §3.1: the certificate is"
          + " authoritative for every security-relevant field): field is "
          + describe(actual) + ", certificate-derived value is " + describe(expected));
    }
  }

  /**
   * Renders a field for a failure message without ever echoing PEM bodies or
   * long material (see {@link #toString()}).
   */
  private static String describe(Object value) {
    if (value instanceof String text) {
      return text.length() > 64 ? "<" + text.length() + "-character string>" : "'" + text + "'";
    }
    return String.valueOf(value);
  }

  private static String commonName(X500Principal subject) {
    try {
      LdapName ldapName = new LdapName(subject.getName());
      for (Rdn rdn : ldapName.getRdns()) {
        if ("CN".equalsIgnoreCase(rdn.getType())) {
          return rdn.getValue().toString();
        }
      }
    } catch (InvalidNameException e) {
      throw new IllegalStateException("Unable to extract CN from subject " + subject, e);
    }
    throw new IllegalArgumentException("Certificate subject has no CN: " + subject);
  }
}
