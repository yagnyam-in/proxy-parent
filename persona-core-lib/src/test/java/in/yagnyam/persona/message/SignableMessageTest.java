package in.yagnyam.persona.message;

import static org.assertj.core.api.Assertions.assertThat;

import in.yagnyam.persona.PersonaId;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link SignableMessage} contract defaults (protocol §5.1):
 * the {@code validSigners()} default and the authorization predicate
 * "S may sign P iff S.canSignOnBehalfOf(v) for some v ∈ P.validSigners()"
 * over the §3.2 delegation rule.
 */
class SignableMessageTest {

  /** Minimal §5.1 payload for the contract tests. */
  private record TestPayload(String messageType, PersonaId signer) implements SignableMessage {

    @Override
    public boolean isValid() {
      return signer != null && signer.isValid();
    }
  }

  /** §5.1 payload with an explicit signer set (delegates). */
  private record TestDelegatingPayload(
      PersonaId signer, Set<PersonaId> delegates) implements SignableMessage {

    @Override
    public String messageType() {
      return "example.Delegating@v1";
    }

    @Override
    public PersonaId signer() {
      return signer;
    }

    @Override
    public Set<PersonaId> validSigners() {
      return delegates;
    }

    @Override
    public boolean isValid() {
      return signer != null && delegates != null && !delegates.isEmpty();
    }
  }

  @Test
  void validSignersDefaultsToTheSignerAlone() {
    PersonaId alice = PersonaId.of("p-alice");
    TestPayload payload = new TestPayload("example.Payload@v1", alice);

    assertThat(payload.validSigners()).containsExactly(alice);
  }

  @Test
  void theSignerItselfIsAuthorized() {
    PersonaId alice = PersonaId.of("p-alice#"
        + "aa".repeat(32));
    TestPayload payload = new TestPayload("example.Payload@v1", alice);

    assertThat(payload.canBeSignedBy(alice)).isTrue();
  }

  @Test
  void anyCertificateOfAnUnpinnedValidSignerIsAuthorized() {
    // §3.2 delegation rule, second bullet: an unpinned valid signer admits
    // any certificate of the same id — the common case of a payload saying
    // "p-alice must sign" and alice signing with whichever certificate is
    // current
    PersonaId aliceUnpinned = PersonaId.of("p-alice");
    PersonaId alicePinned = PersonaId.of("p-alice", "aa".repeat(32));
    TestPayload payload = new TestPayload("example.Payload@v1", aliceUnpinned);

    assertThat(payload.canBeSignedBy(aliceUnpinned)).isTrue();
    assertThat(payload.canBeSignedBy(alicePinned)).isTrue();
  }

  @Test
  void aPinnedValidSignerAdmitsOnlyThatVeryCertificate() {
    // The direction matters: a payload pinning its valid signer to one
    // certificate is NOT satisfied by another spelling of the same id —
    // neither the unpinned rule (the valid signer is pinned) nor equality
    // applies
    PersonaId alicePinned = PersonaId.of("p-alice", "aa".repeat(32));
    TestPayload payload = new TestPayload("example.Payload@v1", alicePinned);

    assertThat(payload.canBeSignedBy(PersonaId.of("p-alice"))).isFalse();
    assertThat(payload.canBeSignedBy(alicePinned)).isTrue();
  }

  @Test
  void aPinnedSignerIsNotAuthorizedForSomebodyElsesPin() {
    // X pinned to a different certificate than the valid signer's pin:
    // neither the unpinned rule nor equality applies
    PersonaId validSigner = PersonaId.of("p-alice", "aa".repeat(32));
    PersonaId otherInstance = PersonaId.of("p-alice", "bb".repeat(32));
    TestPayload payload = new TestPayload("example.Payload@v1", validSigner);

    assertThat(payload.canBeSignedBy(otherInstance)).isFalse();
  }

  @Test
  void aForeignPersonaIsRejected() {
    PersonaId alice = PersonaId.of("p-alice");
    PersonaId bob = PersonaId.of("p-bob");
    TestPayload payload = new TestPayload("example.Payload@v1", alice);

    assertThat(payload.canBeSignedBy(bob)).isFalse();
  }

  @Test
  void nullAndWildcardCandidatesAreHandledPerTheDelegationRule() {
    PersonaId alice = PersonaId.of("p-alice");
    TestPayload payload = new TestPayload("example.Payload@v1", alice);

    assertThat(payload.canBeSignedBy(null)).isFalse();
    // The wildcard SIGNER cannot sign for a concrete persona: only a
    // wildcard valid signer admits anyone (§3.2)
    assertThat(payload.canBeSignedBy(PersonaId.any())).isFalse();
    // ... and a payload whose valid signer IS the wildcard accepts anyone
    TestPayload openPayload = new TestPayload("example.Payload@v1", PersonaId.any());
    assertThat(openPayload.canBeSignedBy(alice)).isTrue();
    assertThat(openPayload.canBeSignedBy(PersonaId.any())).isTrue();
  }

  @Test
  void delegatesAreAuthorizedWhenTheyCanSignOnBehalfOfSomeValidSigner() {
    PersonaId alice = PersonaId.of("p-alice");
    PersonaId bob = PersonaId.of("p-bob");
    PersonaId mallory = PersonaId.of("p-mallory");
    TestDelegatingPayload payload = new TestDelegatingPayload(
        alice, Set.of(alice, bob));

    assertThat(payload.canBeSignedBy(alice)).isTrue();
    assertThat(payload.canBeSignedBy(bob)).isTrue();
    assertThat(payload.canBeSignedBy(mallory)).isFalse();
  }
}
