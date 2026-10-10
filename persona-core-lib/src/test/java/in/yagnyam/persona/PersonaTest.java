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
 * certificate, validity per spec, unique-id helper.
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
  void invalidWhenFieldsMissing() {
    assertThat(new Persona(null, "name", certificate, "key", "thumb").isValid()).isFalse();
    assertThat(new Persona(persona.id(), "name", null, "key", "thumb").isValid()).isFalse();
    assertThat(new Persona(persona.id(), "name", certificate, null, "thumb").isValid()).isFalse();
    assertThat(new Persona(persona.id(), "name", certificate, "key", null).isValid()).isFalse();
    assertThat(new Persona(persona.id(), "name", certificate, "", "thumb").isValid()).isFalse();
  }

  @Test
  void invalidWhenCertificateInvalid() {
    PersonaCertificate invalidCertificate = new PersonaCertificate(null, certificate.serialNumber(),
        certificate.owner(), certificate.sha256Thumbprint(), certificate.validFrom(),
        certificate.validTill(), certificate.certificatePem(), certificate.publicKeyPem(),
        certificate.publicKeySha256Thumbprint());
    assertThat(new Persona(persona.id(), "name", invalidCertificate, "key", "thumb").isValid())
        .isFalse();
  }

  @Test
  void nameIsOptionalLabelNotValidityRequirement() {
    // SPEC.md §3.1.1: only id/certificate/public-key material are required.
    assertThat(new Persona(persona.id(), null, certificate, "key", "thumb").isValid()).isTrue();
    assertThat(new Persona(persona.id(), "", certificate, "key", "thumb").isValid()).isTrue();
  }

  /**
   * Protocol §3.1: two personas are equal iff they share both id and
   * certificate thumbprint — the display name is a label and must not
   * participate in equality.
   */
  @Test
  void equalWhenSameIdAndCertificateRegardlessOfName() {
    Persona asAlice = new Persona(persona.id(), "alice", certificate, "pub", "thumb");
    Persona asBob = new Persona(persona.id(), "bob", certificate, "pub", "thumb");
    assertThat(asAlice).isEqualTo(asBob);
    assertThat(asBob).isEqualTo(asAlice);
    assertThat(asAlice.hashCode()).isEqualTo(asBob.hashCode());

    // Same rule for the derived form: Persona.of(cert) equals an explicitly
    // constructed persona with the same id + certificate.
    assertThat(new Persona(persona.id(), "any-display-name", certificate,
        certificate.publicKeyPem(), certificate.publicKeySha256Thumbprint()))
        .isEqualTo(persona);
  }

  /** Protocol §3.1: a different certificate thumbprint makes personas unequal. */
  @Test
  void unequalWhenCertificateDiffersEvenWithSameIdAndName() {
    KeyPair otherKeyPair = TestSupport.rsaKeyPair();
    X509Certificate otherX509 = TestCertificates.selfSignedWithCommonName(
        otherKeyPair, "p-persona-test", Duration.ofDays(1));
    PersonaCertificate otherCertificate = PersonaCertificate.of(otherX509);
    assertThat(otherCertificate.sha256Thumbprint())
        .isNotEqualTo(certificate.sha256Thumbprint());

    Persona sameIdDifferentCertificate = new Persona(persona.id(), persona.name(),
        otherCertificate, "pub", "thumb");
    assertThat(sameIdDifferentCertificate).isNotEqualTo(persona);
    assertThat(persona).isNotEqualTo(sameIdDifferentCertificate);
  }

  /** Protocol §3.1: a different id makes personas unequal even with the same certificate. */
  @Test
  void unequalWhenIdDiffersEvenWithSameCertificate() {
    PersonaCertificate reIssued = PersonaCertificate.of(
        TestCertificates.selfSignedWithCommonName(TestSupport.rsaKeyPair(), "p-persona-test",
            Duration.ofDays(1)));
    Persona otherId = new Persona(PersonaId.of("p-other"), persona.name(),
        reIssued, "pub", "thumb");
    Persona sameId = new Persona(PersonaId.of("p-other"), "different-name",
        reIssued, "pub", "thumb");
    assertThat(otherId).isEqualTo(sameId);
    assertThat(otherId).isNotEqualTo(persona);
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
