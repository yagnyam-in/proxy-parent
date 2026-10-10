package in.yagnyam.persona.resolver;

import in.yagnyam.persona.Persona;
import in.yagnyam.persona.PersonaId;
import java.util.Optional;

/**
 * Persona resolution service (protocol §9.2): maps a persona id to its
 * persona, including the current certificate.
 *
 * <p>Verification (protocol §7.3 step 6) resolves the signer here; the
 * resolver filters out revoked certificates and, for pinned ids, only ever
 * answers with the exact pinned certificate.
 */
public interface PersonaResolver {

  /**
   * Resolves a persona id to its persona.
   *
   * @param personaId persona id (pinned or unpinned)
   * @return the persona, or empty when the id is unknown, revoked, or the
   *     pin does not match any certificate
   */
  Optional<Persona> resolvePersona(PersonaId personaId);
}
