package in.yagnyam.persona.resolver;

import in.yagnyam.persona.Persona;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.registrar.PersonaRegistrar;
import java.util.Objects;
import java.util.Optional;

/**
 * In-process {@link PersonaResolver} backed by a {@link PersonaRegistrar}
 * (protocol §9.1-§9.2): a thin wrapper over the registrar's certificate
 * store, attaching the derived persona to the resolved certificate.
 *
 * <p>Resolution semantics (protocol §3.2, §9.1):
 * <ul>
 *   <li>Pinned id: exact thumbprint match, empty on mismatch.</li>
 *   <li>Unpinned id: latest certificate issued for that id.</li>
 *   <li>Revoked personas never resolve (protocol §11 rule 11).</li>
 * </ul>
 */
public final class InMemoryPersonaResolver implements PersonaResolver {

  private final PersonaRegistrar registrar;

  /**
   * Creates a resolver over {@code registrar}.
   *
   * @param registrar registrar providing certificate lookups
   */
  public InMemoryPersonaResolver(PersonaRegistrar registrar) {
    this.registrar = Objects.requireNonNull(registrar, "registrar");
  }

  @Override
  public Optional<Persona> resolvePersona(PersonaId personaId) {
    Objects.requireNonNull(personaId, "personaId");
    return registrar.certificateOf(personaId).map(Persona::of);
  }
}
