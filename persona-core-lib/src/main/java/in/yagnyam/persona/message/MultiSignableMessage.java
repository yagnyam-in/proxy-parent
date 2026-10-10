package in.yagnyam.persona.message;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A payload requiring signatures from multiple personas, e.g. escrow
 * proposals (protocol §5.5).
 *
 * <p>Members of §5.5:
 * <ul>
 *   <li>{@link #validSigners()} — the personas allowed to sign;</li>
 *   <li>{@link #minimumRequiredSignatures()} — the quorum threshold;</li>
 *   <li>{@link #hasSufficientSignatures(Set)} — the completeness check,
 *       evaluated against the <em>distinct, verified</em> signer identities
 *       (protocol §6.2).</li>
 * </ul>
 *
 * <p>Multi-signable payloads MAY be signed incrementally: each additional
 * signature yields a new envelope that supersedes the previous one (protocol
 * §5.5, §7.4). The payload extends {@link Validatable} because its
 * {@code isValid()} is asserted at verification time like any other
 * payload's (protocol §7.3 step 3, §11 rule 4); it deliberately does not
 * extend {@link SignableMessage} — a quorum payload has no single canonical
 * signer (§5.5 lists none).
 */
public interface MultiSignableMessage extends Validatable {

  /**
   * The personas allowed to sign this payload (protocol §5.5).
   *
   * @return valid signers, never {@code null} for a valid payload
   */
  Set<PersonaId> validSigners();

  /**
   * The quorum threshold: how many distinct signers must have signed before
   * the payload is complete (protocol §5.5).
   *
   * @return minimum number of distinct signatures, at least 1
   */
  int minimumRequiredSignatures();

  /**
   * Completeness check (protocol §5.5), evaluated against the set of
   * distinct, verified signer identities (protocol §6.2).
   *
   * <p>Counting rules of this default implementation:
   * <ul>
   *   <li>a signer counts only if it is authorized, i.e. iff
   *       {@code signer.canSignOnBehalfOf(v)} holds for some
   *       {@code v ∈ validSigners()} — the §5.1 predicate applied to the
   *       §5.5 signer set, which is what protocol §7.3 step 5 asserts for
   *       every entry of a {@code MultiSignedEnvelope} (protocol §7.4);</li>
   *   <li>each distinct signer counts at most once (protocol §6.2). The
   *       envelope verifier already deduplicates the spellings
   *       {@code alice} and {@code alice#<thumbprint>} by the resolved
   *       certificate thumbprint before calling this method; this check
   *       deduplicates further by the bare persona id, so two certificate
   *       instances of the same logical persona (protocol §3.1 re-keying)
   *       also count as one signer. Both deduplications are conservative
   *       readings of §6.2's "at most once": counting fewer signers can only
   *       withhold a quorum, never grant one.</li>
   * </ul>
   *
   * @param signers the distinct signers that have been verified
   * @return {@code true} if the quorum is reached
   */
  default boolean hasSufficientSignatures(Set<PersonaId> signers) {
    Objects.requireNonNull(signers, "signers");
    Set<String> distinctIds = new HashSet<>();
    for (PersonaId signer : signers) {
      if (signer != null && signer.isValid() && authorized(signer)) {
        distinctIds.add(signer.id());
      }
    }
    return distinctIds.size() >= minimumRequiredSignatures();
  }

  /**
   * The §5.1 authorization predicate applied to the §5.5 signer set: a
   * signer {@code S} may co-sign this payload iff
   * {@code S.canSignOnBehalfOf(v)} for some {@code v ∈ validSigners()}.
   *
   * <p>This is a private member of the completeness check rather than a
   * public {@code canBeSignedBy} on purpose: §5.5 defines exactly three
   * members, and the envelope verifier asserts entry authorization itself
   * (protocol §7.3 step 5 per entry, §7.4), so payloads implementing both
   * this and {@link SignableMessage} inherit only one
   * {@code canBeSignedBy} and never need to resolve a diamond.
   *
   * @param candidate persona id of the purported co-signer
   * @return {@code true} if {@code candidate} is authorized to co-sign
   */
  private boolean authorized(PersonaId candidate) {
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
