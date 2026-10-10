package in.yagnyam.persona.registrar;

import in.yagnyam.persona.Persona;
import in.yagnyam.persona.PersonaCertificate;
import java.util.List;
import java.util.Objects;

/**
 * Result of persona registration (protocol §7.1 step 3 response).
 *
 * @param persona     the registered persona
 * @param certificate the freshly issued certificate
 * @param chain       certificate chain, leaf first, ending at the registrar
 *                    root
 */
public record RegisteredPersona(
    Persona persona,
    PersonaCertificate certificate,
    List<PersonaCertificate> chain) {

  public RegisteredPersona {
    Objects.requireNonNull(persona, "persona");
    Objects.requireNonNull(certificate, "certificate");
    Objects.requireNonNull(chain, "chain");
    chain = List.copyOf(chain);
  }
}
