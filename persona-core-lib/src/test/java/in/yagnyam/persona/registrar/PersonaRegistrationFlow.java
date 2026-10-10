package in.yagnyam.persona.registrar;

import in.yagnyam.persona.crypto.BouncyCastle;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.CsrService;
import in.yagnyam.persona.crypto.Hash;
import in.yagnyam.persona.crypto.PemService;
import in.yagnyam.persona.crypto.PersonaVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Objects;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/**
 * Test helper running the full two-step registration flow (protocol §7.1)
 * against a registrar: issue id, generate key pair, build CSR, hash the
 * revocation passphrase, register.
 */
public final class PersonaRegistrationFlow {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  private PersonaRegistrationFlow() {}

  /**
   * Registers a fresh persona under {@code requestId}.
   *
   * @param registrar            target registrar
   * @param requestId           unique request id
   * @param revocationPassPhrase revocation passphrase to register
   * @return the registered persona
   */
  public static RegisteredPersona register(PersonaRegistrar registrar, String requestId,
      String revocationPassPhrase) {
    String personaId = registrar.issuePersonaId(Objects.requireNonNull(requestId, "requestId"));
    try {
      PersonaVersion version = PersonaVersion.latestVersion();
      KeyPair keyPair = CRYPTO.generateKeyPair(version.keyGenerationAlgorithm(), version.keySize());
      Hash passPhraseHash = CRYPTO.getHash(version.preferredHashAlgorithm(),
          revocationPassPhrase.getBytes(StandardCharsets.UTF_8));
      String csr = CsrService.instance().buildCsr(personaId, keyPair,
          version.certificateSignatureAlgorithm());
      return registrar.registerPersona(requestId, personaId, passPhraseHash, csr);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Registration flow failed", e);
    }
  }

  /**
   * Builds a CSR PEM whose embedded public key does not match its signing
   * key — what a man-in-the-middle swapping the CSR public key would
   * produce. Its signature never verifies.
   *
   * @param personaId     subject CN
   * @param signingKeyPair key pair whose private part signs the CSR
   * @param publicKey     public key embedded in the CSR (different from the signer's)
   * @return CSR PEM whose signature does not verify
   */
  public static String csrWithSwappedPublicKey(String personaId, KeyPair signingKeyPair,
      PublicKey publicKey) {
    try {
      PersonaVersion version = PersonaVersion.latestVersion();
      ContentSigner signer = new JcaContentSignerBuilder(version.certificateSignatureAlgorithm())
          .setProvider(BouncyCastle.provider())
          .build(signingKeyPair.getPrivate());
      PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(
          new X500Principal("CN=" + personaId), publicKey).build(signer);
      return PemService.instance().encodeCertificateRequest(csr);
    } catch (OperatorCreationException | IOException e) {
      throw new IllegalStateException("Unable to build tampered CSR", e);
    }
  }

  /**
   * Builds a CSR PEM with an arbitrary subject DN (for adversarial subject
   * tests: multi-valued RDNs, extra attributes, a missing CN), signed with
   * the active version's certificate signature algorithm, so the CSR
   * signature itself is valid and only the subject shape is at issue.
   *
   * @param subject CSR subject
   * @param keyPair key pair whose public part is certified and whose private
   *                part signs the CSR
   * @return CSR PEM
   */
  public static String csrWithSubject(X500Name subject, KeyPair keyPair) {
    try {
      PersonaVersion version = PersonaVersion.latestVersion();
      ContentSigner signer = new JcaContentSignerBuilder(version.certificateSignatureAlgorithm())
          .setProvider(BouncyCastle.provider())
          .build(keyPair.getPrivate());
      PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(
          subject, keyPair.getPublic()).build(signer);
      return PemService.instance().encodeCertificateRequest(csr);
    } catch (OperatorCreationException | IOException e) {
      throw new IllegalStateException("Unable to build CSR with subject " + subject, e);
    }
  }
}
