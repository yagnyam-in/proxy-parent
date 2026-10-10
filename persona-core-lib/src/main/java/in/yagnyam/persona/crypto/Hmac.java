package in.yagnyam.persona.crypto;

import java.util.Objects;

/**
 * Keyed hash (HMAC) with a random salt prefix (protocol §8).
 *
 * <p>An {@code Hmac} is {@code algorithm(key, iv || message)} where {@code iv}
 * is a fresh random salt, base64 encoded, and {@code key} is a shared secret.
 * Verification re-computes the MAC with the same key and salt (see
 * {@link CryptographyService#verifyHmac}).
 *
 * <p>This record intentionally does not implement
 * {@code in.yagnyam.persona.Validatable}: the crypto package stays free of
 * dependencies on the root package so it can be reused stand-alone. It
 * exposes {@link #isValid()} with the same semantics.
 */
public record Hmac(String algorithm, String iv, String hmac) {

  public Hmac {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(iv, "iv");
    Objects.requireNonNull(hmac, "hmac");
  }

  /**
   * @return {@code true} when algorithm, salt and MAC are all present
   *     (non-empty)
   */
  public boolean isValid() {
    return !algorithm.isEmpty() && !iv.isEmpty() && !hmac.isEmpty();
  }
}
