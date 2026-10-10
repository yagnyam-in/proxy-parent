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
 * <p>The canonical constructor is permissive (a wire-tolerant data carrier):
 * use {@link #isValid()} to validate; {@code null} certificate components are
 * representable and simply make the record invalid.
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
