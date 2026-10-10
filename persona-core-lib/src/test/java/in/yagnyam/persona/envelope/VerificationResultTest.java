package in.yagnyam.persona.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VerificationResult} (protocol §4): a verified result
 * never carries a reason, a rejection always does — mirroring
 * {@code TrustResult}'s invariants.
 */
class VerificationResultTest {

  @Test
  void okIsVerifiedWithoutReason() {
    VerificationResult result = VerificationResult.ok();
    assertThat(result.verified()).isTrue();
    assertThat(result.reason()).isEmpty();
  }

  @Test
  void failCarriesTheReason() {
    VerificationResult result = VerificationResult.fail("quorum not reached");
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValue("quorum not reached");
  }

  @Test
  void inconsistentStatesAreRejected() {
    assertThatThrownBy(() -> new VerificationResult(true, Optional.of("why")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not carry a reason");
    assertThatThrownBy(() -> new VerificationResult(false, Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must carry a reason");
    assertThatThrownBy(() -> VerificationResult.fail(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void toStringNamesTheOutcome() {
    assertThat(VerificationResult.ok().toString()).isEqualTo("VerificationResult.verified");
    assertThat(VerificationResult.fail("bad").toString())
        .isEqualTo("VerificationResult.rejected(bad)");
  }
}
