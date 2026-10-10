package in.yagnyam.persona.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.Persona;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.registrar.InProcessRegistrar;
import in.yagnyam.persona.registrar.PersonaRegistrationFlow;
import in.yagnyam.persona.registrar.RegisteredPersona;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemoryPersonaResolver} (protocol §9.2): pinned, unpinned,
 * revoked and unknown ids.
 */
class InMemoryPersonaResolverTest {

  private InProcessRegistrar registrar;
  private InMemoryPersonaResolver resolver;
  private RegisteredPersona registered;

  @BeforeEach
  void setUp() {
    registrar = new InProcessRegistrar();
    resolver = new InMemoryPersonaResolver(registrar);
    registered = PersonaRegistrationFlow.register(registrar, UUID.randomUUID().toString(),
        "revocation-pass");
  }

  @Test
  void resolvesUnpinnedIdToRegisteredPersona() {
    Optional<Persona> resolved = resolver.resolvePersona(PersonaId.of(registered.persona().id().id()));
    assertThat(resolved).isPresent();
    assertThat(resolved.get().certificate().sha256Thumbprint())
        .isEqualTo(registered.certificate().sha256Thumbprint());
    assertThat(resolved.get().isValid()).isTrue();
  }

  @Test
  void resolvesPinnedIdWhenThumbprintMatches() {
    Optional<Persona> resolved = resolver.resolvePersona(registered.persona().id());
    assertThat(resolved).isPresent();
    assertThat(resolved.get().id()).isEqualTo(registered.persona().id());
  }

  @Test
  void returnsEmptyForPinnedMismatch() {
    PersonaId wrongPin = PersonaId.of(registered.persona().id().id(), "ee".repeat(32));
    assertThat(resolver.resolvePersona(wrongPin)).isEmpty();
  }

  @Test
  void hidesRevokedPersona() throws Exception {
    registrar.revoke(registered.persona().id().id(), "revocation-pass");
    assertThat(resolver.resolvePersona(registered.persona().id())).isEmpty();
    assertThat(resolver.resolvePersona(PersonaId.of(registered.persona().id().id()))).isEmpty();
  }

  @Test
  void returnsEmptyForUnknownId() {
    assertThat(resolver.resolvePersona(PersonaId.of("p-never-registered"))).isEmpty();
  }

  @Test
  void returnsEmptyForWildcardId() {
    assertThat(resolver.resolvePersona(PersonaId.any())).isEmpty();
  }

  @Test
  void rejectsNullArguments() {
    assertThatThrownBy(() -> resolver.resolvePersona(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new InMemoryPersonaResolver(null))
        .isInstanceOf(NullPointerException.class);
  }
}
