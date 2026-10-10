package in.yagnyam.persona.resolver;

import in.yagnyam.persona.Persona;
import in.yagnyam.persona.PersonaCertificate;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Validates that a persona's certificate is currently trustworthy
 * (protocol §4 trust model, §11 rules 8 and 11):
 * <ol>
 *   <li>the certificate is within its validity window at the given
 *       instant;</li>
 *   <li>the certificate chains to a trusted Registrar root — in this
 *       in-process model every persona certificate is signed directly by
 *       the root, so one-level verification (signature with the root public
 *       key plus issuer DN match) suffices;</li>
 *   <li>the certificate is not revoked, per the supplied
 *       {@link RevocationCheck}.</li>
 * </ol>
 *
 * <p>Results are {@link TrustResult} values; validation never throws for
 * trust outcomes (the verifier layer decides what to do with failures).
 *
 * <p>Design note: the validity of the pinned root itself is not checked —
 * roots are distributed out-of-band (protocol §3.4 rule 5, §12) and pinning
 * them is the trust decision, not their expiry.
 */
public final class TrustValidator {

  /**
   * Source of revocation status for certificates, e.g.
   * {@code registrar::isRevoked} or a CRL/OCSP-backed lookup
   * (protocol §7.3 step 6, §11 rule 11).
   */
  @FunctionalInterface
  public interface RevocationCheck {

    /**
     * @param certificate certificate to check
     * @return {@code true} if the certificate's persona is revoked
     */
    boolean isRevoked(PersonaCertificate certificate);
  }

  private final List<PersonaCertificate> trustedRoots;
  private final RevocationCheck revocationCheck;

  /**
   * Creates a validator over the trusted roots and revocation source.
   *
   * @param trustedRoots   trusted registrar roots (protocol §3.4 rule 5)
   * @param revocationCheck revocation status provider
   */
  public TrustValidator(List<PersonaCertificate> trustedRoots, RevocationCheck revocationCheck) {
    Objects.requireNonNull(trustedRoots, "trustedRoots");
    Objects.requireNonNull(revocationCheck, "revocationCheck");
    this.trustedRoots = List.copyOf(trustedRoots);
    this.revocationCheck = revocationCheck;
  }

  /**
   * Validates {@code persona}'s certificate at {@code atInstant}.
   *
   * @param persona   persona to validate
   * @param atInstant instant at which the validity window is evaluated
   * @return trust result with a failure reason, never an exception
   */
  public TrustResult validate(Persona persona, Instant atInstant) {
    Objects.requireNonNull(atInstant, "atInstant");
    if (persona == null) {
      return TrustResult.fail("persona is null");
    }
    PersonaCertificate certificate = persona.certificate();
    if (certificate == null) {
      return TrustResult.fail("persona has no certificate");
    }
    if (!persona.isValid()) {
      return TrustResult.fail("persona is not valid");
    }
    if (atInstant.isBefore(certificate.validFrom())) {
      return TrustResult.fail("certificate is not valid yet at " + atInstant
          + " (valid from " + certificate.validFrom() + ")");
    }
    if (atInstant.isAfter(certificate.validTill())) {
      return TrustResult.fail("certificate expired at " + atInstant
          + " (valid till " + certificate.validTill() + ")");
    }
    if (!chainsToTrustedRoot(certificate)) {
      return TrustResult.fail("certificate does not chain to a trusted registrar root");
    }
    if (revocationCheck.isRevoked(certificate)) {
      return TrustResult.fail("certificate is revoked");
    }
    return TrustResult.ok();
  }

  private boolean chainsToTrustedRoot(PersonaCertificate certificate) {
    X509Certificate leaf = certificate.certificate();
    if (leaf == null) {
      return false;
    }
    for (PersonaCertificate root : trustedRoots) {
      X509Certificate rootCertificate = root.certificate();
      if (rootCertificate == null) {
        continue;
      }
      if (!leaf.getIssuerX500Principal().equals(rootCertificate.getSubjectX500Principal())) {
        continue;
      }
      try {
        leaf.verify(rootCertificate.getPublicKey());
        return true;
      } catch (GeneralSecurityException e) {
        // Wrong root or corrupted certificate: keep looking.
      }
    }
    return false;
  }
}
