package in.yagnyam.persona.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link MultiSignableMessage} contract (protocol §5.5, §6.2,
 * §A.3): the quorum members and the completeness check over distinct,
 * authorized signers.
 */
class MultiSignableMessageTest {

  /** The charter payload of protocol §A.3: founders, a quorum and terms. */
  private record TestCharter(
      Set<PersonaId> founders, int minimumSignatures) implements MultiSignableMessage {

    @Override
    public Set<PersonaId> validSigners() {
      return founders;
    }

    @Override
    public int minimumRequiredSignatures() {
      return minimumSignatures;
    }

    @Override
    public boolean isValid() {
      return !founders.isEmpty() && minimumSignatures >= 1
          && minimumSignatures <= founders.size();
    }
  }

  private static TestCharter charter(Set<PersonaId> founders, int minimum) {
    return new TestCharter(founders, minimum);
  }

  private static PersonaId persona(String id) {
    return PersonaId.of(id);
  }

  private static PersonaId pinned(String id) {
    return PersonaId.of(id, "aa".repeat(32));
  }

  @Test
  void quorumReachedWhenEnoughDistinctAuthorizedSignersSigned() {
    TestCharter twoOfThree = charter(
        Set.of(persona("p-alice"), persona("p-bob"), persona("p-carol")), 2);

    assertThat(twoOfThree.hasSufficientSignatures(
        Set.of(persona("p-alice"), persona("p-bob")))).isTrue();
    assertThat(twoOfThree.hasSufficientSignatures(
        Set.of(persona("p-alice")))).isFalse();
    assertThat(twoOfThree.hasSufficientSignatures(Set.of())).isFalse();
  }

  @Test
  void unauthorizedSignersDoNotCountTowardTheQuorum() {
    // §7.3 step 5 per §7.4: only personas that can sign on behalf of some
    // valid signer count; everyone else's signatures are worthless here
    TestCharter oneOfTwo = charter(Set.of(persona("p-alice"), persona("p-bob")), 1);

    assertThat(oneOfTwo.hasSufficientSignatures(
        Set.of(persona("p-mallory"), persona("p-eve")))).isFalse();
    assertThat(oneOfTwo.hasSufficientSignatures(
        Set.of(persona("p-mallory"), persona("p-alice")))).isTrue();
  }

  @Test
  void duplicateSpellingsOfTheSameSignerCountOnce() {
    // protocol §6.2: alice and alice#<thumbprint> are one signer, never two
    TestCharter twoOfTwo = charter(
        Set.of(persona("p-alice"), persona("p-bob")), 2);

    assertThat(twoOfTwo.hasSufficientSignatures(
        Set.of(persona("p-alice"), pinned("p-alice")))).isFalse();
    assertThat(twoOfTwo.hasSufficientSignatures(
        Set.of(pinned("p-alice"), persona("p-alice"), persona("p-bob")))).isTrue();
  }

  @Test
  void twoCertificateInstancesOfTheSameLogicalPersonaCountOnce() {
    // §3.1: re-keyed certificates are different instances of the same
    // logical identity; for quorum purposes the conservative reading of
    // §6.2's "at most once" counts them as one signer
    TestCharter twoOfTwo = charter(
        Set.of(persona("p-alice"), persona("p-bob")), 2);

    PersonaId firstInstance = PersonaId.of("p-alice", "aa".repeat(32));
    PersonaId secondInstance = PersonaId.of("p-alice", "bb".repeat(32));

    assertThat(twoOfTwo.hasSufficientSignatures(
        Set.of(firstInstance, secondInstance))).isFalse();
    assertThat(twoOfTwo.hasSufficientSignatures(
        Set.of(firstInstance, secondInstance, persona("p-bob")))).isTrue();
  }

  @Test
  void aWildcardValidSignerAdmitsAnyone() {
    TestCharter oneOfOne = charter(Set.of(PersonaId.any()), 1);

    assertThat(oneOfOne.hasSufficientSignatures(Set.of(persona("p-anyone")))).isTrue();
  }

  @Test
  void nullSignersAreIgnoredAndNullArgumentsRejected() {
    TestCharter oneOfOne = charter(Set.of(persona("p-alice")), 1);

    Set<PersonaId> withNull = new java.util.HashSet<>();
    withNull.add(null);
    assertThat(oneOfOne.hasSufficientSignatures(withNull)).isFalse();

    withNull.add(persona("p-alice"));
    assertThat(oneOfOne.hasSufficientSignatures(withNull)).isTrue();

    assertThatThrownBy(() -> oneOfOne.hasSufficientSignatures(null))
        .isInstanceOf(NullPointerException.class);
  }
}
