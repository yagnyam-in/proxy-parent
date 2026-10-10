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
