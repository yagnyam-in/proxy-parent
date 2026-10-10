package in.yagnyam.persona;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PersonaCertificate} (protocol §3.1, §3.2, §9.1): derived
 * fields, thumbprint cross-check, PEM round-trip, validity,
 * certificate-excluded equality, and the handle binding that rejects metadata
 * contradicting the certificate.
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
    // No handle to bind to: the fields are representable (wire tolerance) but
    // the record is invalid, so nothing can trust them.
    PersonaCertificate noHandle = new PersonaCertificate(null, certificate.serialNumber(),
        certificate.owner(), certificate.sha256Thumbprint(), certificate.validFrom(),
        certificate.validTill(), certificate.certificatePem(), certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
    assertThat(noHandle.isValid()).isFalse();
  }

  /**
   * G1 (P1): the certificate handle is authoritative for every
   * security-relevant field, so a copy whose metadata contradicts the handle
   * is rejected at construction — validation (which trusts the metadata) can
   * never disagree with chain verification (which uses the handle).
   */
  @Test
  void rejectsCopyWithExtendedValidTill() {
    PersonaCertificate template = certificate;
    assertThatThrownBy(() -> new PersonaCertificate(x509, template.serialNumber(),
        template.owner(), template.sha256Thumbprint(), template.validFrom(),
        template.validTill().plus(Duration.ofDays(365)), template.certificatePem(),
        template.publicKeyPem(), template.publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("validTill")
        .hasMessageContaining("certificate handle");
  }

  /**
   * G1 (P1): a copy carrying someone else's owner (subject CN) — the field
   * the persona id is bound to — is rejected at construction.
   */
  @Test
  void rejectsCopyWithWrongOwner() {
    PersonaCertificate template = certificate;
    assertThatThrownBy(() -> new PersonaCertificate(x509, template.serialNumber(),
        "p-someone-else", template.sha256Thumbprint(), template.validFrom(),
        template.validTill(), template.certificatePem(), template.publicKeyPem(),
        template.publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("owner")
        .hasMessageContaining("p-someone-else")
        .hasMessageContaining(template.owner())
        .hasMessageContaining("certificate handle");
  }

  /**
   * G1 (P1): every security-relevant field is bound to the handle — here each
   * field is replaced by the corresponding value of a <em>different</em>
   * certificate (the realistic attack: metadata of another certificate
   * attached to this handle).
   */
  @Test
  void rejectsEveryFieldThatContradictsTheCertificateHandle() {
    PersonaCertificate template = certificate;
    PersonaCertificate other = PersonaCertificate.of(TestCertificates.selfSigned(
        TestSupport.rsaKeyPair(), "CN=p-other,O=Persona", Duration.ofDays(30)));

    assertFieldRejected("serialNumber", () -> new PersonaCertificate(x509, other.serialNumber(),
        template.owner(), template.sha256Thumbprint(), template.validFrom(), template.validTill(),
        template.certificatePem(), template.publicKeyPem(),
        template.publicKeySha256Thumbprint()));
    assertFieldRejected("owner", () -> new PersonaCertificate(x509, template.serialNumber(),
        other.owner(), template.sha256Thumbprint(), template.validFrom(), template.validTill(),
        template.certificatePem(), template.publicKeyPem(),
        template.publicKeySha256Thumbprint()));
    assertFieldRejected("sha256Thumbprint", () -> new PersonaCertificate(x509,
        template.serialNumber(), template.owner(), other.sha256Thumbprint(), template.validFrom(),
        template.validTill(), template.certificatePem(), template.publicKeyPem(),
        template.publicKeySha256Thumbprint()));
    // Swapped validity bounds: the metadata would claim a window the
    // certificate never had (and an unordered one at that).
    assertFieldRejected("validFrom", () -> new PersonaCertificate(x509, template.serialNumber(),
        template.owner(), template.sha256Thumbprint(), template.validTill(), template.validTill(),
        template.certificatePem(), template.publicKeyPem(),
        template.publicKeySha256Thumbprint()));
    assertFieldRejected("validTill", () -> new PersonaCertificate(x509, template.serialNumber(),
        template.owner(), template.sha256Thumbprint(), template.validFrom(), template.validFrom(),
        template.certificatePem(), template.publicKeyPem(),
        template.publicKeySha256Thumbprint()));
    assertFieldRejected("certificatePem", () -> new PersonaCertificate(x509,
        template.serialNumber(), template.owner(), template.sha256Thumbprint(),
        template.validFrom(), template.validTill(), other.certificatePem(),
        template.publicKeyPem(), template.publicKeySha256Thumbprint()));
    assertFieldRejected("publicKeyPem", () -> new PersonaCertificate(x509, template.serialNumber(),
        template.owner(), template.sha256Thumbprint(), template.validFrom(), template.validTill(),
        template.certificatePem(), other.publicKeyPem(),
        template.publicKeySha256Thumbprint()));
    assertFieldRejected("publicKeySha256Thumbprint", () -> new PersonaCertificate(x509,
        template.serialNumber(), template.owner(), template.sha256Thumbprint(),
        template.validFrom(), template.validTill(), template.certificatePem(),
        template.publicKeyPem(), other.publicKeySha256Thumbprint()));
  }

  /**
   * A handle-derived record is accepted whatever route created it: the
   * deriving factory and the PEM round-trip both stay consistent with the
   * binding checks (G1 regression guard).
   */
  @Test
  void derivedRecordsSatisfyTheHandleBinding() {
    assertThat(certificate).isEqualTo(PersonaCertificate.of(x509));
    assertThat(certificate.isValid()).isTrue();
  }

  @Test
  void invalidWhenStringFieldsMissing() {
    // Absent fields are representable (wire tolerance) and make the record
    // invalid; they can never contradict the handle, so they are not
    // rejected at construction.
    assertThat(copyWith(owner -> null).isValid()).isFalse();
    assertThat(copyWithFields(certificate.serialNumber(), null).isValid()).isFalse();
    // A non-null field that contradicts the handle is rejected outright.
    assertThatThrownBy(() -> copyWith(owner -> "")).isInstanceOf(IllegalArgumentException.class);
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

  /**
   * Asserts that building {@code copy} fails, naming {@code expectedField} as
   * the field that contradicts the certificate handle.
   */
  private static void assertFieldRejected(String expectedField,
      Supplier<PersonaCertificate> copy) {
    assertThatThrownBy(copy::get)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(expectedField)
        .hasMessageContaining("certificate handle");
  }
}
