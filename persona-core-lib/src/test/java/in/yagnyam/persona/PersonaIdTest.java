package in.yagnyam.persona;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PersonaId} (protocol §3.2 parsing grammar, wildcard
 * semantics, delegation rule, structural equality).
 */
class PersonaIdTest {

  private static final String THUMBPRINT = "ab".repeat(32);
  private static final String OTHER_THUMBPRINT = "cd".repeat(32);

  @Nested
  class Parsing {

    @Test
    void parsesUnpinnedId() {
      PersonaId personaId = PersonaId.of("p-alice");
      assertThat(personaId.id()).isEqualTo("p-alice");
      assertThat(personaId.sha256Thumbprint()).isEmpty();
      assertThat(personaId.uniqueId()).isEqualTo("p-alice");
      assertThat(personaId.toString()).isEqualTo("p-alice");
      assertThat(personaId.isWildcard()).isFalse();
      assertThat(personaId.isValid()).isTrue();
    }

    @Test
    void parsesPinnedId() {
      PersonaId personaId = PersonaId.of("p-alice#" + THUMBPRINT);
      assertThat(personaId.id()).isEqualTo("p-alice");
      assertThat(personaId.sha256Thumbprint()).contains(THUMBPRINT);
      assertThat(personaId.uniqueId()).isEqualTo("p-alice#" + THUMBPRINT);
      assertThat(personaId.isWildcard()).isFalse();
    }

    @Test
    void rejectsEmptyId() {
      assertThatThrownBy(() -> PersonaId.of("")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> PersonaId.of("", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLeadingHash() {
      assertThatThrownBy(() -> PersonaId.of("#" + THUMBPRINT))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMoreThanOneHash() {
      assertThatThrownBy(() -> PersonaId.of("p-alice#" + THUMBPRINT + "#" + OTHER_THUMBPRINT))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> PersonaId.of("p-alice#" + THUMBPRINT + "#x"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidThumbprintFormat() {
      assertThatThrownBy(() -> PersonaId.of("p-alice#short")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> PersonaId.of("p-alice#" + THUMBPRINT.toUpperCase()))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> PersonaId.of("p-alice#" + "a".repeat(63)))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> PersonaId.of("p-alice#" + "a".repeat(65)))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> PersonaId.of("p-alice#" + "g".repeat(64)))
          .isInstanceOf(IllegalArgumentException.class);
      // Empty (but present) thumbprint is rejected rather than silently
      // coerced to "unpinned".
      assertThatThrownBy(() -> PersonaId.of("p-alice#")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> PersonaId.of("p-alice", "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonHexWildcardThumbprintOutsideWildcard() {
      assertThatThrownBy(() -> PersonaId.of("p-alice#any"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> PersonaId.of("p-alice", "any"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullArguments() {
      assertThatThrownBy(() -> PersonaId.of((String) null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> PersonaId.of(null, null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> PersonaId.of((PersonaCertificate) null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsHashInsideIdComponent() {
      assertThatThrownBy(() -> PersonaId.of("p-a#b", null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new PersonaId("p-a#b", Optional.empty()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void derivesFromCertificate() throws Exception {
      var keyPair = TestSupport.rsaKeyPair();
      var certificate = PersonaCertificate.of(
          TestCertificates.selfSignedWithCommonName(keyPair, "p-cert-owner", java.time.Duration.ofDays(1)));
      PersonaId personaId = PersonaId.of(certificate);
      assertThat(personaId.id()).isEqualTo("p-cert-owner");
      assertThat(personaId.sha256Thumbprint()).contains(certificate.sha256Thumbprint());
    }
  }

  @Nested
  class Wildcard {

    @Test
    void anyConstantIsWildcard() {
      PersonaId any = PersonaId.any();
      assertThat(any.id()).isEqualTo("any");
      assertThat(any.sha256Thumbprint()).contains("any");
      assertThat(any.uniqueId()).isEqualTo("any#any");
      assertThat(any.isWildcard()).isTrue();
      assertThat(any.isValid()).isTrue();
    }

    @Test
    void parsesBothWildcardSpellings() {
      // "any" (unpinned) and "any#any" (pinned) are both wildcards, but they
      // are different structural spellings (protocol §3.2).
      assertThat(PersonaId.of("any").isWildcard()).isTrue();
      assertThat(PersonaId.of("any").uniqueId()).isEqualTo("any");
      assertThat(PersonaId.of("any#any")).isEqualTo(PersonaId.any());
      assertThat(PersonaId.of("any#any").isWildcard()).isTrue();
    }

    @Test
    void unpinnedAnyAndPinnedAnyAreDifferentSpellings() {
      // Structural equality over both fields: "any" != "any#any".
      assertThat(PersonaId.of("any")).isNotEqualTo(PersonaId.any());
    }
  }

  @Nested
  class Delegation {

    @Test
    void wildcardTargetAcceptsAnySigner() {
      assertThat(PersonaId.of("p-alice").canSignOnBehalfOf(PersonaId.any())).isTrue();
      assertThat(PersonaId.of("p-alice#" + THUMBPRINT).canSignOnBehalfOf(PersonaId.any())).isTrue();
      assertThat(PersonaId.any().canSignOnBehalfOf(PersonaId.any())).isTrue();
    }

    @Test
    void wildcardSignerDoesNotCoverNonWildcardTarget() {
      assertThat(PersonaId.any().canSignOnBehalfOf(PersonaId.of("p-alice"))).isFalse();
      assertThat(PersonaId.any().canSignOnBehalfOf(PersonaId.of("p-alice#" + THUMBPRINT))).isFalse();
    }

    @Test
    void pinnedSignerCoversUnpinnedSameIdTarget() {
      assertThat(PersonaId.of("p-alice#" + THUMBPRINT).canSignOnBehalfOf(PersonaId.of("p-alice")))
          .isTrue();
      assertThat(PersonaId.of("p-alice").canSignOnBehalfOf(PersonaId.of("p-alice"))).isTrue();
    }

    @Test
    void pinnedSignerCoversExactSamePinnedTarget() {
      assertThat(PersonaId.of("p-alice#" + THUMBPRINT)
          .canSignOnBehalfOf(PersonaId.of("p-alice#" + THUMBPRINT))).isTrue();
    }

    @Test
    void pinnedSignerRejectsDifferentThumbprintTarget() {
      assertThat(PersonaId.of("p-alice#" + THUMBPRINT)
          .canSignOnBehalfOf(PersonaId.of("p-alice#" + OTHER_THUMBPRINT))).isFalse();
    }

    @Test
    void unpinnedSignerRejectsPinnedTarget() {
      assertThat(PersonaId.of("p-alice").canSignOnBehalfOf(PersonaId.of("p-alice#" + THUMBPRINT)))
          .isFalse();
    }

    @Test
    void differentIdsRejectEachOther() {
      assertThat(PersonaId.of("p-bob").canSignOnBehalfOf(PersonaId.of("p-alice"))).isFalse();
      assertThat(PersonaId.of("p-bob#" + THUMBPRINT)
          .canSignOnBehalfOf(PersonaId.of("p-alice#" + THUMBPRINT))).isFalse();
      assertThat(PersonaId.of("p-bob#" + THUMBPRINT).canSignOnBehalfOf(PersonaId.of("p-alice")))
          .isFalse();
    }

    @Test
    void nullTargetIsRejected() {
      assertThat(PersonaId.of("p-alice").canSignOnBehalfOf(null)).isFalse();
      assertThat(PersonaId.any().canSignOnBehalfOf(null)).isFalse();
    }
  }

  @Nested
  class Equality {

    @Test
    void equalityIsStructuralOverBothFields() {
      assertThat(PersonaId.of("p-alice")).isEqualTo(PersonaId.of("p-alice"));
      assertThat(PersonaId.of("p-alice")).hasSameHashCodeAs(PersonaId.of("p-alice"));
      assertThat(PersonaId.of("p-alice#" + THUMBPRINT))
          .isEqualTo(PersonaId.of("p-alice#" + THUMBPRINT));
      assertThat(PersonaId.of("p-alice#" + THUMBPRINT))
          .hasSameHashCodeAs(PersonaId.of("p-alice#" + THUMBPRINT));

      assertThat(PersonaId.of("p-alice")).isNotEqualTo(PersonaId.of("p-alice#" + THUMBPRINT));
      assertThat(PersonaId.of("p-alice")).isNotEqualTo(PersonaId.of("p-bob"));
      assertThat(PersonaId.of("p-alice#" + THUMBPRINT))
          .isNotEqualTo(PersonaId.of("p-alice#" + OTHER_THUMBPRINT));
      assertThat(PersonaId.of("p-alice")).isNotEqualTo(null);
      assertThat(PersonaId.of("p-alice")).isNotEqualTo("p-alice");
    }

    @Test
    void nullThumbprintIsUnpinned() {
      PersonaId unpinned = PersonaId.of("p-alice", null);
      assertThat(unpinned.sha256Thumbprint()).isEmpty();
      assertThat(unpinned).isEqualTo(PersonaId.of("p-alice"));
    }
  }
}
