package in.yagnyam.persona;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Persona} ({@code SPEC.md} §3.1.1): derivation from a
 * certificate, validity per spec, the id/public-key binding to the
 * certificate, unique-id helper and equality.
 */
class PersonaTest {

  private PersonaCertificate certificate;
  private Persona persona;

  @BeforeEach
  void setUp() {
    KeyPair keyPair = TestSupport.rsaKeyPair();
    X509Certificate x509 = TestCertificates.selfSignedWithCommonName(keyPair, "p-persona-test",
        Duration.ofDays(1));
    certificate = PersonaCertificate.of(x509);
    persona = Persona.of(certificate);
  }

  @Test
  void derivesPersonaFromCertificate() {
    assertThat(persona.id()).isEqualTo(PersonaId.of("p-persona-test", certificate.sha256Thumbprint()));
    assertThat(persona.id().sha256Thumbprint()).contains(certificate.sha256Thumbprint());
    assertThat(persona.name()).isEqualTo("p-persona-test");
    assertThat(persona.certificate()).isEqualTo(certificate);
    assertThat(persona.publicKeyEncoded()).isEqualTo(certificate.publicKeyPem());
    assertThat(persona.publicKeySha256Thumbprint()).isEqualTo(certificate.publicKeySha256Thumbprint());
  }

  @Test
  void isValidForCompletePersona() {
    assertThat(persona.isValid()).isTrue();
  }

  @Test
  void getUniqueIdMirrorsId() {
    assertThat(persona.getUniqueId()).isEqualTo(persona.id().uniqueId());
    assertThat(persona.getUniqueId()).contains("#");
  }

  @Test
  void invalidWhenComponentsMissing() {
    // Absent components are representable (wire tolerance) but make the
    // record invalid; absent components never contradict the certificate.
    assertThat(new Persona(null, "name", certificate, certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint()).isValid()).isFalse();
    assertThat(new Persona(persona.id(), "name", null, null, null).isValid()).isFalse();
    assertThat(new Persona(persona.id(), "name", certificate, null,
        certificate.publicKeySha256Thumbprint()).isValid()).isFalse();
    assertThat(new Persona(persona.id(), "name", certificate, certificate.publicKeyPem(), null)
        .isValid()).isFalse();
    // A non-null field that contradicts the certificate is not representable
    // at all (G2).
    assertThatThrownBy(() -> new Persona(persona.id(), "name", certificate, "",
        certificate.publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("publicKeyEncoded");
  }

  @Test
  void invalidWhenCertificateInvalid() {
    PersonaCertificate invalidCertificate = new PersonaCertificate(null, certificate.serialNumber(),
        certificate.owner(), certificate.sha256Thumbprint(), certificate.validFrom(),
        certificate.validTill(), certificate.certificatePem(), certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
    assertThat(new Persona(persona.id(), "name", invalidCertificate,
        invalidCertificate.publicKeyPem(), invalidCertificate.publicKeySha256Thumbprint())
        .isValid())
        .isFalse();
  }

  @Test
  void nameIsOptionalLabelNotValidityRequirement() {
    // SPEC.md §3.1.1: only id/certificate/public-key material are required.
    assertThat(new Persona(persona.id(), null, certificate, certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint()).isValid()).isTrue();
    assertThat(new Persona(persona.id(), "", certificate, certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint()).isValid()).isTrue();
  }

  /**
   * G2 (P1): the certified identity is the certificate — a Persona carrying
   * an id its certificate does not certify is rejected at construction, so
   * the metadata validation path can never disagree with the certificate used
   * for chain verification. Only {@link Persona#name()} is a free label.
   */
  @Test
  void rejectsIdNotCertifiedByTheCertificate() {
    // Someone else's id with a perfectly valid certificate.
    assertThatThrownBy(() -> new Persona(PersonaId.of("p-alice"), "alice", certificate,
        certificate.publicKeyPem(), certificate.publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match its certificate")
        .hasMessageContaining("p-alice")
        .hasMessageContaining(persona.id().uniqueId());

    // Even an unpinned spelling of the *right* owner is not the certified id
    // (which is the owner pinned to the certificate thumbprint).
    assertThatThrownBy(() -> new Persona(PersonaId.of(certificate.owner()), "alice", certificate,
        certificate.publicKeyPem(), certificate.publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match its certificate");
  }

  /**
   * G2 (P1): the public key is certified by the certificate — public-key
   * fields that the certificate does not carry are rejected at construction.
   */
  @Test
  void rejectsPublicKeyNotCertifiedByTheCertificate() {
    assertThatThrownBy(() -> new Persona(persona.id(), "name", certificate,
        "-----BEGIN PUBLIC KEY-----\nnot-the-certified-key\n-----END PUBLIC KEY-----",
        certificate.publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("publicKeyEncoded");
    assertThatThrownBy(() -> new Persona(persona.id(), "name", certificate,
        certificate.publicKeyPem(), "ee".repeat(32)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("publicKeySha256Thumbprint");
  }

  /**
   * Protocol §3.1: two personas are equal iff they share both id and
   * certificate thumbprint — the display name is a label and must not
   * participate in equality.
   */
  @Test
  void equalWhenSameIdAndCertificateRegardlessOfName() {
    Persona asAlice = new Persona(persona.id(), "alice", certificate, certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
    Persona asBob = new Persona(persona.id(), "bob", certificate, certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
    assertThat(asAlice).isEqualTo(asBob);
    assertThat(asBob).isEqualTo(asAlice);
    assertThat(asAlice.hashCode()).isEqualTo(asBob.hashCode());

    // Same rule for the derived form: Persona.of(cert) equals an explicitly
    // constructed persona with the same id + certificate.
    assertThat(new Persona(persona.id(), "any-display-name", certificate,
        certificate.publicKeyPem(), certificate.publicKeySha256Thumbprint()))
        .isEqualTo(persona);
  }

  /**
   * Protocol §3.1: a different certificate thumbprint makes personas unequal
   * — the re-issued certificate is a different instance of the same logical
   * identity (same owner, different thumbprint). The adversarial spelling of
   * this rule (the old persona's id attached to the new certificate) is not
   * representable; see {@link #rejectsIdNotCertifiedByTheCertificate()}.
   */
  @Test
  void unequalWhenCertificateDiffersEvenWithSameOwnerAndName() {
    KeyPair otherKeyPair = TestSupport.rsaKeyPair();
    X509Certificate otherX509 = TestCertificates.selfSignedWithCommonName(
        otherKeyPair, "p-persona-test", Duration.ofDays(1));
    PersonaCertificate otherCertificate = PersonaCertificate.of(otherX509);
    assertThat(otherCertificate.sha256Thumbprint())
        .isNotEqualTo(certificate.sha256Thumbprint());

    Persona reIssued = Persona.of(otherCertificate);
    assertThat(reIssued.id().id()).as("same logical identity").isEqualTo(persona.id().id());
    assertThat(reIssued.id()).isNotEqualTo(persona.id());
    assertThat(reIssued).isNotEqualTo(persona);
    assertThat(persona).isNotEqualTo(reIssued);
  }

  @Test
  void unequalToNullOrOtherTypes() {
    assertThat(persona.equals(null)).isFalse();
    assertThat(persona).isNotEqualTo("not a persona");
  }

  @Test
  void toStringAvoidsPemBodies() {
    assertThat(persona.toString()).contains(persona.getUniqueId());
    assertThat(persona.toString()).doesNotContain("BEGIN PUBLIC KEY");
  }

  @Test
  void rejectsNullCertificateInFactory() {
    assertThatThrownBy(() -> Persona.of(null)).isInstanceOf(NullPointerException.class);
  }
}
