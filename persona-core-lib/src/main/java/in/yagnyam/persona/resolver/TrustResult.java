package in.yagnyam.persona.resolver;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of trust validation: trusted/failed plus a human-readable reason
 * when the validation failed (protocol §4). Validation results are values,
 * never exceptions — the verifier layer decides what to do with failures.
 */
public record TrustResult(boolean trusted, Optional<String> reason) {

  public TrustResult {
    Objects.requireNonNull(reason, "reason");
    if (trusted && reason.isPresent()) {
      throw new IllegalArgumentException("A trusted result must not carry a reason");
    }
    if (!trusted && reason.isEmpty()) {
      throw new IllegalArgumentException("A failed result must carry a reason");
    }
  }

  /**
   * @return successful validation
   */
  public static TrustResult ok() {
    return new TrustResult(true, Optional.empty());
  }

  /**
   * @param reason why validation failed
   * @return failed validation
   */
  public static TrustResult fail(String reason) {
    return new TrustResult(false, Optional.of(Objects.requireNonNull(reason, "reason")));
  }

  @Override
  public String toString() {
    return trusted ? "TrustResult.trusted" : "TrustResult.untrusted(" + reason.orElse("?") + ")";
  }
}
