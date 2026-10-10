package in.yagnyam.persona.message;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import java.util.Set;

/**
 * Base contract of every payload that is signed (protocol §5.1).
 *
 * <p>A signable payload provides the four members of §5.1:
 * {@link #messageType()} — a stable, namespaced wire identifier, never a
 * programming-language class name (protocol §11 rule 1); {@link #signer()} —
 * the canonical persona expected to sign; {@link #validSigners()} —
 * defaulting to {@code {signer()}}, MAY include delegates; and
 * {@link #isValid()} — the self-check of internal invariants (inherited from
 * {@link Validatable}) that MUST hold both before signing and after
 * deserialization (protocol §11 rule 4).
 *
 * <p>The contract is encoding- and transport-agnostic: it says nothing about
 * how a payload travels on the wire. Serialization is the envelope layer's
 * concern (protocol §6, §10); the signer serializes the payload exactly once
 * (protocol §7.2 step 2).
 */
public interface SignableMessage extends Validatable {

  /**
   * The stable, namespaced message-type identifier of this payload, e.g.
   * {@code payments.PaymentAuthorization@v2} (protocol §5.1, §11 rule 1).
   *
   * <p>Never derived from a Java class name; peers resolve the payload
   * schema from this value (protocol §6.1).
   *
   * @return message type, never {@code null}
   */
  String messageType();

  /**
   * The canonical persona expected to sign this payload (protocol §5.1).
   *
   * @return signer persona id, never {@code null} for a valid payload
   */
  PersonaId signer();

  /**
   * The personas allowed to sign this payload (protocol §5.1); defaults to
   * {@code {signer()}} and MAY include delegates.
   *
   * @return valid signers; an empty set (returned when {@link #signer()} is
   *     {@code null}, i.e. for a payload that is not valid) authorizes
   *     nobody — the check fails closed
   */
  default Set<PersonaId> validSigners() {
    return signer() == null ? Set.of() : Set.of(signer());
  }

  /**
   * The §5.1 authorization predicate: a signer {@code S} may sign this
   * payload iff {@code S.canSignOnBehalfOf(v)} holds for some
   * {@code v ∈ validSigners()}.
   *
   * <p>The delegation rule itself (wildcard, unpinned same-id, exact
   * equality) is defined by {@link PersonaId#canSignOnBehalfOf(PersonaId)}
   * (protocol §3.2). The envelope verifier asserts this predicate before
   * accepting an envelope (protocol §7.3 step 5, §11 rule 2).
   *
   * @param candidate persona id of the purported signer, may be {@code null}
   * @return {@code true} if {@code candidate} is authorized to sign this
   *     payload
   */
  default boolean canBeSignedBy(PersonaId candidate) {
    if (candidate == null) {
      return false;
    }
    Set<PersonaId> valid = validSigners();
    if (valid == null) {
      return false;
    }
    for (PersonaId allowed : valid) {
      if (candidate.canSignOnBehalfOf(allowed)) {
        return true;
      }
    }
    return false;
  }
}
