package in.yagnyam.persona;

import in.yagnyam.persona.crypto.BouncyCastle;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Builds self-signed X.509 certificates directly via BouncyCastle for tests
 * that need a certificate without going through the registrar.
 */
public final class TestCertificates {

  private TestCertificates() {}

  /**
   * Builds a self-signed certificate with the given RFC 2253 subject.
   *
   * @param keyPair  signing key pair
   * @param subject  RFC 2253 subject DN (e.g. {@code CN=p-test,O=Persona})
   * @param validity validity window
   * @return self-signed X.509 v3 certificate
   */
  public static X509Certificate selfSigned(KeyPair keyPair, String subject, Duration validity) {
    try {
      X500Name subjectName = new X500Name(subject);
      Date notBefore = Date.from(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
      Date notAfter = Date.from(notBefore.toInstant().plus(validity));
      ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
          .setProvider(BouncyCastle.provider())
          .build(keyPair.getPrivate());
      X509CertificateHolder holder = new X509v3CertificateBuilder(
              subjectName, BigInteger.valueOf(System.nanoTime()), notBefore, notAfter,
              subjectName, SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()))
          .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
          .build(signer);
      return new JcaX509CertificateConverter()
          .setProvider(BouncyCastle.provider())
          .getCertificate(holder);
    } catch (GeneralSecurityException | IOException | OperatorCreationException e) {
      throw new IllegalStateException("Unable to build test certificate", e);
    }
  }

  /**
   * Builds a self-signed certificate with subject {@code CN=<commonName>}.
   *
   * @param keyPair    signing key pair
   * @param commonName subject CN
   * @param validity  validity window
   * @return self-signed X.509 v3 certificate
   */
  public static X509Certificate selfSignedWithCommonName(KeyPair keyPair, String commonName, Duration validity) {
    return selfSigned(keyPair, "CN=" + commonName, validity);
  }
}
