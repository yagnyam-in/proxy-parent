package in.yagnyam.persona.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Objects;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/**
 * Builds PKCS#10 certificate signing requests (protocol §7.1 step 2: the
 * client locally generates a key pair and builds a CSR with subject
 * {@code CN=<personaId>} before submitting it to the Registrar).
 */
public final class CsrService {

  private static final CsrService INSTANCE = new CsrService();

  private final PemService pemService = PemService.instance();

  private CsrService() {}

  /**
   * Returns the shared, thread-safe instance.
   *
   * @return singleton CSR service
   */
  public static CsrService instance() {
    return INSTANCE;
  }

  /**
   * Builds a PKCS#10 certificate signing request as PEM, with subject
   * {@code CN=<personaId>} and the public key of {@code keyPair}, signed
   * with the private key of {@code keyPair} using {@code signatureAlgorithm}.
   *
   * <p>The persona id must be non-empty and must not contain {@code #},
   * which the persona-id grammar reserves as the id/thumbprint separator
   * (protocol §3.2) — a CN carrying it would make the issued certificate
   * owner unparseable as a persona id.
   *
   * @param personaId          persona id to bind as subject CN
   * @param keyPair            persona key pair
   * @param signatureAlgorithm CSR signature algorithm (e.g.
   *                            {@code SHA256withRSA})
   * @return PEM encoded CSR ({@code CERTIFICATE REQUEST})
   * @throws GeneralSecurityException if the CSR cannot be built
   */
  public String buildCsr(String personaId, KeyPair keyPair, String signatureAlgorithm)
      throws GeneralSecurityException {
    Objects.requireNonNull(personaId, "personaId");
    Objects.requireNonNull(keyPair, "keyPair");
    Objects.requireNonNull(signatureAlgorithm, "signatureAlgorithm");
    if (personaId.isEmpty()) {
      throw new IllegalArgumentException("personaId must not be empty");
    }
    if (personaId.indexOf('#') >= 0) {
      throw new IllegalArgumentException(
          "personaId must not contain '#' (reserved by the persona-id grammar, protocol §3.2)");
    }
    X500Principal subject = new X500Principal("CN=" + personaId);
    try {
      ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
          .setProvider(BouncyCastle.provider())
          .build(keyPair.getPrivate());
      PKCS10CertificationRequest certificateRequest =
          new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic()).build(signer);
      return pemService.encodeCertificateRequest(certificateRequest);
    } catch (OperatorCreationException | IOException e) {
      throw new GeneralSecurityException("Unable to build CSR for persona " + personaId, e);
    }
  }
}
