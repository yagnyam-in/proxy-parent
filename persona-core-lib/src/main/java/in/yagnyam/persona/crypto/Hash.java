package in.yagnyam.persona.crypto;

import java.util.Objects;

/**
 * Salted cryptographic hash (protocol §8).
 *
 * <p>A {@code Hash} is the digest of {@code algorithm(iv || message)} where
 * {@code iv} is a fresh random salt, base64 encoded. The salt makes the same
 * message hash differently every time (so passphrase hashes cannot be
 * compared by pre-computation) while still allowing verification by
 * re-hashing with the stored salt. This is how revocation passphrases are
 * registered with the Registrar (protocol §3.3, §7.1: the passphrase MUST be
 * hashed with a per-Persona salt before submission).
 *
 * <p>This record intentionally does not implement
 * {@code in.yagnyam.persona.Validatable}: the crypto package stays free of
 * dependencies on the root package so it can be reused stand-alone. It
 * exposes {@link #isValid()} with the same semantics.
 */
public record Hash(String algorithm, String iv, String hash) {

  public Hash {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(iv, "iv");
    Objects.requireNonNull(hash, "hash");
  }

  /**
   * Factory for a pre-computed hash.
   *
   * @param algorithm hash algorithm name (e.g. {@code SHA-256})
   * @param ivBase64  base64 of the random salt
   * @param hashBase64 base64 of the resulting digest
   * @return hash record
   */
  public static Hash of(String algorithm, String ivBase64, String hashBase64) {
    return new Hash(algorithm, ivBase64, hashBase64);
  }

  /**
   * @return {@code true} when algorithm, salt and digest are all present
   *     (non-empty)
   */
  public boolean isValid() {
    return !algorithm.isEmpty() && !iv.isEmpty() && !hash.isEmpty();
  }
}
