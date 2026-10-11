package in.yagnyam.persona.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaCertificate;
import in.yagnyam.persona.TestCertificates;
import in.yagnyam.persona.TestSupport;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PemService} (protocol §7.1, §9.1): PEM round-trips for
 * public keys, private keys, certificates and CSRs, plus type enforcement.
 */
class PemServiceTest {

  private final PemService pemService = PemService.instance();

  private KeyPair keyPair;
  private X509Certificate certificate;

  @BeforeEach
  void setUp() {
    keyPair = TestSupport.rsaKeyPair();
    certificate = TestCertificates.selfSignedWithCommonName(keyPair, "p-pem-test", Duration.ofDays(1));
  }

  @Test
  void encodesAndDecodesPublicKey() throws Exception {
    String pem = pemService.encodePublicKey(keyPair.getPublic());
    assertThat(pem).contains("BEGIN PUBLIC KEY");
    PublicKey decoded = pemService.decodePublicKey(pem);
    assertThat(Arrays.equals(decoded.getEncoded(), keyPair.getPublic().getEncoded())).isTrue();
  }

  @Test
  void encodesAndDecodesPrivateKey() throws Exception {
    String pem = pemService.encodePrivateKey(keyPair.getPrivate());
    assertThat(pem).contains("BEGIN PRIVATE KEY");
    PrivateKey decoded = pemService.decodePrivateKey(pem);
    assertThat(Arrays.equals(decoded.getEncoded(), keyPair.getPrivate().getEncoded())).isTrue();
  }

  @Test
  void encodesAndDecodesCertificate() throws Exception {
    String pem = pemService.encodeCertificate(certificate);
    assertThat(pem).contains("BEGIN CERTIFICATE");
    X509Certificate decoded = pemService.decodeCertificate(pem);
    assertThat(Arrays.equals(decoded.getEncoded(), certificate.getEncoded())).isTrue();
  }

  @Test
  void encodesAndDecodesCertificateRequest() throws Exception {
    String csrPem = CsrService.instance().buildCsr("p-pem-csr-test", keyPair,
        PersonaVersion.latestVersion().certificateSignatureAlgorithm());
    assertThat(csrPem).contains("BEGIN CERTIFICATE REQUEST");
    PKCS10CertificationRequest decoded = pemService.decodeCertificateRequest(csrPem);
    PKCS10CertificationRequest reEncoded = pemService.decodeCertificateRequest(
        pemService.encodeCertificateRequest(decoded));
    assertThat(Arrays.equals(reEncoded.getEncoded(), decoded.getEncoded())).isTrue();
  }

  @Test
  void rejectsWrongPemTypes() throws Exception {
    String certificatePem = pemService.encodeCertificate(certificate);
    String publicKeyPem = pemService.encodePublicKey(keyPair.getPublic());
    assertThatThrownBy(() -> pemService.decodePublicKey(certificatePem))
        .isInstanceOf(IOException.class);
    assertThatThrownBy(() -> pemService.decodeCertificate(publicKeyPem))
        .isInstanceOf(IOException.class);
  }

  @Test
  void rejectsGarbageInput() {
    assertThatThrownBy(() -> pemService.decodeCertificate("not a pem"))
        .isInstanceOf(IOException.class);
    assertThatThrownBy(() -> pemService.decodePublicKey("-----BEGIN PUBLIC KEY-----\ngarbage\n-----END PUBLIC KEY-----"))
        .isInstanceOf(IOException.class);
  }

  /**
   * A non-empty PEM envelope with an empty DER body holds no certificate: the
   * provider either returns {@code null} (BouncyCastle before 1.86) or throws
   * (BouncyCastle 1.86+), and the decode boundary must reject it as an
   * {@code IOException} either way instead of letting a {@code null} escape
   * (protocol §9.1, §10.1). Found by the conformance vector
   * {@code identity/pem-reject/certificate-header-only}.
   */
  @Test
  void rejectsCertificateWithEmptyBody() {
    String emptyBodyPem = "-----BEGIN CERTIFICATE-----\n-----END CERTIFICATE-----\n";
    assertThatThrownBy(() -> pemService.decodeCertificate(emptyBodyPem))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("does not hold a certificate");
    // The declared contract of the caller is IllegalArgumentException, not NPE.
    assertThatThrownBy(() -> PersonaCertificate.parsePem(emptyBodyPem))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid certificate PEM");
  }

  @Test
  void rejectsNullArguments() {
    assertThatThrownBy(() -> pemService.encodePublicKey(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> pemService.encodePrivateKey(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> pemService.encodeCertificate(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> pemService.encodeCertificateRequest(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> pemService.decodePublicKey(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> pemService.decodePrivateKey(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> pemService.decodeCertificate(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> pemService.decodeCertificateRequest(null))
        .isInstanceOf(NullPointerException.class);
  }
}
