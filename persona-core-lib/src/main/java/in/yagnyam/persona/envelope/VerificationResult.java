package in.yagnyam.persona.envelope;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of envelope verification: verified/rejected plus a human-readable
 * reason when the verification failed (protocol §4, §7.3). Verification
 * results are values, never exceptions — mirroring
 * {@code in.yagnyam.persona.resolver.TrustResult} — and every failure
 * carries an informative reason: "Instrument verification failures — they
 * are the earliest indicator of protocol drift or compromise" (protocol
 * §14 step 9).
 *
 * <p>A verified result never carries a reason; a failed result always does
 * (there is no such thing as an unexplained rejection).
 */
public record VerificationResult(boolean verified, Optional<String> reason) {

  public VerificationResult {
    Objects.requireNonNull(reason, "reason");
    if (verified && reason.isPresent()) {
      throw new IllegalArgumentException("A verified result must not carry a reason");
    }
    if (!verified && reason.isEmpty()) {
      throw new IllegalArgumentException("A rejected result must carry a reason");
    }
  }

  /**
   * @return successful verification (protocol §7.3 step 8: "Mark E as
   *     verified")
   */
  public static VerificationResult ok() {
    return new VerificationResult(true, Optional.empty());
  }

  /**
   * @param reason why the envelope was rejected (protocol §4: failure of
   *               any step is a hard rejection)
   * @return failed verification
   */
  public static VerificationResult fail(String reason) {
    return new VerificationResult(false, Optional.of(Objects.requireNonNull(reason, "reason")));
  }

  @Override
  public String toString() {
    return verified
        ? "VerificationResult.verified"
        : "VerificationResult.rejected(" + reason.orElse("?") + ")";
  }
}
