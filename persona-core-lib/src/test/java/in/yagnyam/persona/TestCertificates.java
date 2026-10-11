package in.yagnyam.persona;

import in.yagnyam.persona.crypto.BouncyCastle;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Builds X.509 certificates directly via BouncyCastle for tests that need a
 * certificate without going through the registrar. The certificate signature
 * algorithm follows the signing key's family ({@code SHA256withRSA} for RSA,
 * {@code Ed25519} for Ed25519).
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
      ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithmFor(keyPair))
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

  /**
   * Builds a self-signed CA root certificate: subject {@code CN=<commonName>},
   * {@code basicConstraints = CA:TRUE} and a critical {@code keyUsage}
   * asserting only {@code keyCertSign | cRLSign} — the ordinary shape of a CA
   * root, and the shape a JCA {@code Signature.initVerify(Certificate)}
   * refuses because it does not assert {@code digitalSignature}.
   *
   * @param keyPair    signing key pair
   * @param commonName subject CN
   * @param validity  validity window
   * @return self-signed X.509 v3 CA certificate
   */
  public static X509Certificate selfSignedCaRoot(KeyPair keyPair, String commonName, Duration validity) {
    try {
      X500Name subjectName = new X500Name("CN=" + commonName);
      Date notBefore = Date.from(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
      Date notAfter = Date.from(notBefore.toInstant().plus(validity));
      ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithmFor(keyPair))
          .setProvider(BouncyCastle.provider())
          .build(keyPair.getPrivate());
      X509CertificateHolder holder = new X509v3CertificateBuilder(
              subjectName, BigInteger.valueOf(System.nanoTime()), notBefore, notAfter,
              subjectName, SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()))
          .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
          .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign))
          .build(signer);
      return new JcaX509CertificateConverter()
          .setProvider(BouncyCastle.provider())
          .getCertificate(holder);
    } catch (GeneralSecurityException | IOException | OperatorCreationException e) {
      throw new IllegalStateException("Unable to build test CA certificate", e);
    }
  }

  /**
   * Builds an end-entity certificate {@code CN=<commonName>} for
   * {@code subjectKey}, signed by {@code issuer} — for subject keys that
   * cannot sign themselves (X25519).
   *
   * @param subjectKey public key to certify
   * @param commonName subject CN
   * @param issuer     issuing key pair (RSA or Ed25519)
   * @param validity   validity window
   * @return issued X.509 v3 certificate with a critical keyAgreement keyUsage
   */
  public static X509Certificate issuedBy(PublicKey subjectKey, String commonName, KeyPair issuer,
      Duration validity) {
    try {
      X500Name issuerName = new X500Name("CN=test-issuer");
      Date notBefore = Date.from(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
      Date notAfter = Date.from(notBefore.toInstant().plus(validity));
      ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithmFor(issuer))
          .setProvider(BouncyCastle.provider())
          .build(issuer.getPrivate());
      X509CertificateHolder holder = new X509v3CertificateBuilder(
              issuerName, BigInteger.valueOf(System.nanoTime()), notBefore, notAfter,
              new X500Name("CN=" + commonName), SubjectPublicKeyInfo.getInstance(subjectKey.getEncoded()))
          .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
          .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyAgreement))
          .build(signer);
      return new JcaX509CertificateConverter()
          .setProvider(BouncyCastle.provider())
          .getCertificate(holder);
    } catch (GeneralSecurityException | IOException | OperatorCreationException e) {
      throw new IllegalStateException("Unable to build issued test certificate", e);
    }
  }

  private static String signatureAlgorithmFor(KeyPair keyPair) {
    String family = keyPair.getPrivate().getAlgorithm();
    return switch (family) {
      case "RSA" -> "SHA256withRSA";
      case "Ed25519", "EdDSA" -> "Ed25519";
      default -> throw new IllegalArgumentException("No test signature algorithm for key family " + family);
    };
  }
}
