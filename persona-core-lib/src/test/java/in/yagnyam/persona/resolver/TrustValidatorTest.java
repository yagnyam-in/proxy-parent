package in.yagnyam.persona.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.Persona;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.registrar.InProcessRegistrar;
import in.yagnyam.persona.registrar.PersonaRegistrationFlow;
import in.yagnyam.persona.registrar.RegisteredPersona;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TrustValidator} (protocol §4, §11 rules 8 and 11):
 * valid personas pass; expired, not-yet-valid, revoked and wrongly-rooted
 * personas fail with reasons; results are values, never exceptions.
 */
class TrustValidatorTest {

  @Test
  void trustsFreshlyRegisteredPersona() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona registered = registerPersona(registrar);
    TrustValidator validator = new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);

    TrustResult result = validator.validate(registered.persona(), Instant.now());
    assertThat(result.trusted()).isTrue();
    assertThat(result.reason()).isEmpty();
  }

  @Test
  void rejectsPersonaThatIsNotYetValid() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona registered = registerPersona(registrar);
    TrustValidator validator = new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);

    Instant beforeIssuance = registered.certificate().validFrom().minusSeconds(3600);
    TrustResult result = validator.validate(registered.persona(), beforeIssuance);
    assertThat(result.trusted()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> assertThat(reason).contains("not valid yet"));
  }

  @Test
  void rejectsExpiredPersona() {
    // Registrar whose certificates expire almost immediately; validating at
    // a future instant must report expiry, while the persona record itself
    // remains structurally valid (validFrom <= validTill).
    InProcessRegistrar registrar = new InProcessRegistrar(Duration.ofMillis(500));
    RegisteredPersona registered = registerPersona(registrar);
    TrustValidator validator = new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);
    assertThat(registered.persona().isValid()).isTrue();

    TrustResult result = validator.validate(registered.persona(), Instant.now().plusSeconds(3600));
    assertThat(result.trusted()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> assertThat(reason).contains("expired"));
  }

  @Test
  void rejectsRevokedPersona() throws Exception {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona registered = registerPersona(registrar);
    registrar.revoke(registered.persona().id().id(), "revocation-pass");
    TrustValidator validator = new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);

    TrustResult result = validator.validate(registered.persona(), Instant.now());
    assertThat(result.trusted()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> assertThat(reason).contains("revoked"));
  }

  @Test
  void rejectsPersonaCertifiedByDifferentRoot() {
    InProcessRegistrar issuer = new InProcessRegistrar();
    InProcessRegistrar otherRealm = new InProcessRegistrar();
    RegisteredPersona registered = registerPersona(issuer);
    TrustValidator wrongRootValidator = new TrustValidator(otherRealm.trustedRoots(),
        otherRealm::isRevoked);

    TrustResult result = wrongRootValidator.validate(registered.persona(), Instant.now());
    assertThat(result.trusted()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("does not chain"));
  }

  /**
   * Adversarial case (id↔certificate binding): a Persona that claims
   * p-alice's id but carries p-bob's perfectly valid certificate (same
   * registrar, unrevoked, in-window) is rejected — without the binding check
   * the impostor would pass every other trust rule.
   *
   * <p>Layer that rejects (round-2): {@link Persona}'s canonical constructor
   * now binds {@code id} to the certified {@code (owner, thumbprint)} of its
   * own certificate, so the impostor cannot even be built — the swap is
   * unconstructible and this test pins that boundary. The equivalent owner /
   * pinned-thumbprint checks inside {@link TrustValidator} remain as defense
   * in depth for any other producer of a {@code Persona} value, but they are
   * unreachable through the record's public constructor: every
   * {@code isValid()} persona is id-bound to its certificate by construction
   * (asserted here by {@link #everyValidPersonaIsIdBoundToItsCertificate()}).
   */
  @Test
  void rejectsPersonaCarryingSomeoneElsesCertificateAtConstruction() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona alice = registerPersona(registrar);
    RegisteredPersona bob = registerPersona(registrar);

    assertThatThrownBy(() -> new Persona(PersonaId.of(alice.persona().id().id()), "alice",
        bob.certificate(), bob.certificate().publicKeyPem(),
        bob.certificate().publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match its certificate");
  }

  /**
   * The same boundary spelled with a wrong pin: a Persona whose id pins a
   * thumbprint its certificate does not have is unconstructible.
   */
  @Test
  void rejectsPersonaWithMismatchedPinnedThumbprintAtConstruction() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona alice = registerPersona(registrar);

    assertThatThrownBy(() -> new Persona(
        PersonaId.of(alice.persona().id().id(), "ee".repeat(32)), "alice",
        alice.certificate(), alice.certificate().publicKeyPem(),
        alice.certificate().publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match its certificate");
  }

  /**
   * Proof that the validator's id/pin checks cannot be reached with an
   * inconsistent record: every constructible {@code isValid()} persona is
   * already id-bound (owner + canonical thumbprint) and public-key-bound to
   * its certificate, so the validator's own binding checks are defense in
   * depth rather than the first line of defense.
   */
  @Test
  void everyValidPersonaIsIdBoundToItsCertificate() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona registered = registerPersona(registrar);
    Persona persona = registered.persona();

    assertThat(persona.isValid()).isTrue();
    assertThat(persona.id()).isEqualTo(PersonaId.of(persona.certificate()));
    assertThat(persona.id().sha256Thumbprint())
        .contains(persona.certificate().sha256Thumbprint());
    assertThat(persona.publicKeyEncoded()).isEqualTo(persona.certificate().publicKeyPem());
    assertThat(persona.publicKeySha256Thumbprint())
        .isEqualTo(persona.certificate().publicKeySha256Thumbprint());
  }

  /**
   * Adversarial case (CA as persona): the registrar's own root is
   * self-signed, in-window, unrevoked and chains to itself — every rule
   * except the CA check passes, so only the CA rejection can stop it.
   */
  @Test
  void rejectsRegistrarRootUsedAsPersona() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    TrustValidator validator = new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);

    Persona rootAsPersona = Persona.of(registrar.trustedRoots().get(0));
    assertThat(rootAsPersona.isValid()).isTrue();

    TrustResult result = validator.validate(rootAsPersona, Instant.now());
    assertThat(result.trusted()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason ->
        assertThat(reason).contains("CA certificates are not personas"));
  }

  @Test
  void rejectsPersonaWithoutRootsAtAll() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona registered = registerPersona(registrar);
    TrustValidator noRootsValidator = new TrustValidator(List.of(), registrar::isRevoked);

    assertThat(noRootsValidator.validate(registered.persona(), Instant.now()).trusted()).isFalse();
  }

  @Test
  void rejectsNullPersonaWithReason() {
    TrustValidator validator = new TrustValidator(List.of(), certificate -> false);
    TrustResult result = validator.validate(null, Instant.now());
    assertThat(result.trusted()).isFalse();
    assertThat(result.reason()).contains("persona is null");
  }

  @Test
  void rejectsPersonaWithoutCertificate() {
    Persona certificateLess = new Persona(PersonaId.any(), "name", null, "key", "thumb");
    TrustValidator validator = new TrustValidator(List.of(), certificate -> false);
    assertThat(validator.validate(certificateLess, Instant.now()).trusted()).isFalse();
  }

  @Test
  void trustResultInvariantsRejectInconsistentStates() {
    assertThatThrownBy(() -> new TrustResult(true, Optional.of("why")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TrustResult(false, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> TrustResult.fail(null))
        .isInstanceOf(NullPointerException.class);
  }

  private RegisteredPersona registerPersona(InProcessRegistrar registrar) {
    return PersonaRegistrationFlow.register(registrar, UUID.randomUUID().toString(),
        "revocation-pass");
  }
}
