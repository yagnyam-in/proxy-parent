package in.yagnyam.persona;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PersonaCertificate} (protocol §3.1, §3.2, §9.1): derived
 * fields, thumbprint cross-check, PEM round-trip, validity and
 * certificate-excluded equality.
 */
class PersonaCertificateTest {

  private KeyPair keyPair;
  private X509Certificate x509;
  private PersonaCertificate certificate;

  @BeforeEach
  void setUp() {
    keyPair = TestSupport.rsaKeyPair();
    x509 = TestCertificates.selfSignedWithCommonName(keyPair, "p-cert-test", Duration.ofDays(1));
    certificate = PersonaCertificate.of(x509);
  }

  @Test
  void derivesOwnerFromSubjectCommonName() {
    assertThat(certificate.owner()).isEqualTo("p-cert-test");
  }

  @Test
  void derivesSerialNumberAsDecimalString() {
    assertThat(certificate.serialNumber()).isEqualTo(x509.getSerialNumber().toString());
    assertThat(certificate.serialNumber()).matches("\\d+");
  }

  @Test
  void thumbprintMatchesIndependentSha256OfDer() throws Exception {
    // Independent computation path (TestSupport), equivalent to
    // `shasum -a 256` over the DER encoding.
    assertThat(certificate.sha256Thumbprint())
        .isEqualTo(TestSupport.sha256Hex(x509.getEncoded()))
        .matches("[0-9a-f]{64}");
  }

  @Test
  void derivesValidityWindowFromCertificate() {
    assertThat(certificate.validFrom()).isEqualTo(x509.getNotBefore().toInstant());
    assertThat(certificate.validTill()).isEqualTo(x509.getNotAfter().toInstant());
    assertThat(certificate.validTill()).isAfter(certificate.validFrom());
  }

  @Test
  void publicKeyThumbprintMatchesIndependentSha256OfEncodedKey() throws Exception {
    assertThat(certificate.publicKeySha256Thumbprint())
        .isEqualTo(TestSupport.sha256Hex(x509.getPublicKey().getEncoded()));
  }

  @Test
  void encodesCertificateAndPublicKeyAsPem() {
    assertThat(certificate.certificatePem()).contains("BEGIN CERTIFICATE");
    assertThat(certificate.certificatePem()).isEqualTo(PersonaCertificate.encodePem(x509));
    assertThat(certificate.publicKeyPem()).contains("BEGIN PUBLIC KEY");
  }

  @Test
  void parsesPemRoundTrip() throws Exception {
    PersonaCertificate parsed = PersonaCertificate.parsePem(certificate.certificatePem());
    assertThat(parsed).isEqualTo(certificate);
    assertThat(Arrays.equals(parsed.certificate().getEncoded(), x509.getEncoded())).isTrue();
    assertThat(parsed.sha256Thumbprint()).isEqualTo(certificate.sha256Thumbprint());
  }

  @Test
  void extractsCommonNameFromMultiAttributeSubject() {
    X509Certificate multiCn = TestCertificates.selfSigned(keyPair, "CN=p-multi,O=Persona Tests",
        Duration.ofDays(1));
    assertThat(PersonaCertificate.of(multiCn).owner()).isEqualTo("p-multi");
  }

  @Test
  void isValidForWellFormedCertificate() {
    assertThat(certificate.isValid()).isTrue();
  }

  @Test
  void invalidWhenCertificateHandleMissing() {
    PersonaCertificate noHandle = new PersonaCertificate(null, certificate.serialNumber(),
        certificate.owner(), certificate.sha256Thumbprint(), certificate.validFrom(),
        certificate.validTill(), certificate.certificatePem(), certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
    assertThat(noHandle.isValid()).isFalse();
  }

  @Test
  void invalidWhenValidityWindowDisordered() {
    PersonaCertificate swapped = new PersonaCertificate(x509, certificate.serialNumber(),
        certificate.owner(), certificate.sha256Thumbprint(), certificate.validTill(),
        certificate.validFrom(), certificate.certificatePem(), certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
    assertThat(swapped.isValid()).isFalse();
  }

  @Test
  void invalidWhenStringFieldsMissing() {
    assertThat(copyWith(owner -> null).isValid()).isFalse();
    assertThat(copyWith(owner -> "").isValid()).isFalse();
    assertThat(copyWithFields(certificate.serialNumber(), null).isValid()).isFalse();
  }

  @Test
  void equalityExcludesCertificateHandle() {
    PersonaCertificate copyWithoutHandle = new PersonaCertificate(null, certificate.serialNumber(),
        certificate.owner(), certificate.sha256Thumbprint(), certificate.validFrom(),
        certificate.validTill(), certificate.certificatePem(), certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
    assertThat(copyWithoutHandle).isEqualTo(certificate);
    assertThat(copyWithoutHandle).hasSameHashCodeAs(certificate);
  }

  @Test
  void toStringAvoidsPemBodies() {
    assertThat(certificate.toString()).contains(certificate.sha256Thumbprint());
    assertThat(certificate.toString()).doesNotContain("BEGIN CERTIFICATE");
  }

  @Test
  void rejectsInvalidPem() {
    assertThatThrownBy(() -> PersonaCertificate.parsePem("not a pem"))
        .isInstanceOf(IllegalArgumentException.class);
    // A certificate PEM is not a public key PEM, and vice versa.
    assertThatThrownBy(() -> PersonaCertificate.parsePem(certificate.publicKeyPem()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullArguments() {
    assertThatThrownBy(() -> PersonaCertificate.of(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> PersonaCertificate.parsePem(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> PersonaCertificate.encodePem(null))
        .isInstanceOf(NullPointerException.class);
  }

  private PersonaCertificate copyWith(UnaryOperator<String> ownerTransform) {
    return new PersonaCertificate(x509, certificate.serialNumber(), ownerTransform.apply(certificate.owner()),
        certificate.sha256Thumbprint(), certificate.validFrom(), certificate.validTill(),
        certificate.certificatePem(), certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
  }

  private PersonaCertificate copyWithFields(String serialNumber, String owner) {
    return new PersonaCertificate(x509, serialNumber, owner, certificate.sha256Thumbprint(),
        certificate.validFrom(), certificate.validTill(), certificate.certificatePem(),
        certificate.publicKeyPem(), certificate.publicKeySha256Thumbprint());
  }
}
