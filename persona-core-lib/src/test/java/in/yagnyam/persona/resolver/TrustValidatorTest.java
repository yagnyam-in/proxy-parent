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
   * registrar, unrevoked, in-window) must be rejected with the binding
   * reason — without the binding check the impostor would pass every other
   * trust rule.
   */
  @Test
  void rejectsPersonaCarryingSomeoneElsesCertificate() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona alice = registerPersona(registrar);
    RegisteredPersona bob = registerPersona(registrar);
    TrustValidator validator = new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);

    Persona impostor = new Persona(PersonaId.of(alice.persona().id().id()), "alice",
        bob.certificate(), bob.certificate().publicKeyPem(),
        bob.certificate().publicKeySha256Thumbprint());
    assertThat(impostor.isValid()).isTrue();

    TrustResult result = validator.validate(impostor, Instant.now());
    assertThat(result.trusted()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> assertThat(reason)
        .contains("does not match certificate owner")
        .contains(alice.persona().id().id())
        .contains(bob.certificate().owner()));
  }

  /** Id↔certificate binding: a pinned thumbprint must equal the certificate's. */
  @Test
  void rejectsPersonaWithMismatchedPinnedThumbprint() {
    InProcessRegistrar registrar = new InProcessRegistrar();
    RegisteredPersona alice = registerPersona(registrar);
    TrustValidator validator = new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);

    Persona wronglyPinned = new Persona(
        PersonaId.of(alice.persona().id().id(), "ee".repeat(32)), "alice",
        alice.certificate(), alice.certificate().publicKeyPem(),
        alice.certificate().publicKeySha256Thumbprint());
    assertThat(wronglyPinned.isValid()).isTrue();

    TrustResult result = validator.validate(wronglyPinned, Instant.now());
    assertThat(result.trusted()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("pinned thumbprint"));
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
