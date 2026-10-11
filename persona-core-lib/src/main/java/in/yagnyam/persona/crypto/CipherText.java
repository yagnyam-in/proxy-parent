package in.yagnyam.persona.crypto;

import java.util.Objects;
import java.util.Optional;

/**
 * Envelope for encrypted content (protocol §8; {@code SPEC.md} §3.2).
 *
 * <p>Carries the cipher output together with everything needed to decrypt
 * it: the algorithm, the initialization vector (absent for IV-less asymmetric
 * schemes such as RSA-OAEP, mandatory for symmetric schemes), optionally an
 * integrity MAC over the cipher text, and — for asymmetric schemes built on
 * an ephemeral-static key agreement such as v1's
 * {@code X25519/HKDF-SHA256/AES-256-GCM} — the base64-encoded X.509
 * SubjectPublicKeyInfo DER of the sender's ephemeral public key.
 */
public record CipherText(Optional<String> iv, String encryptionAlgorithm, String cipherText,
                         Optional<String> hmacAlgorithm, Optional<String> hmac,
                         Optional<String> ephemeralPublicKey) {

  public CipherText {
    Objects.requireNonNull(iv, "iv");
    Objects.requireNonNull(encryptionAlgorithm, "encryptionAlgorithm");
    Objects.requireNonNull(cipherText, "cipherText");
    Objects.requireNonNull(hmacAlgorithm, "hmacAlgorithm");
    Objects.requireNonNull(hmac, "hmac");
    Objects.requireNonNull(ephemeralPublicKey, "ephemeralPublicKey");
  }

  /**
   * @return {@code true} when algorithm and cipher text are present and the
   *     optional MAC fields are used consistently (both present or both
   *     absent). An empty cipher text is valid only together with an IV: a
   *     zero-length symmetric input (e.g. AES/CTR) encrypts to zero bytes
   *     while still carrying its IV, whereas IV-less asymmetric schemes
   *     (RSA-OAEP) never produce an empty output.
   */
  public boolean isValid() {
    return !encryptionAlgorithm.isEmpty()
        && (!cipherText.isEmpty() || iv.isPresent())
        && hmacAlgorithm.isPresent() == hmac.isPresent();
  }
}
