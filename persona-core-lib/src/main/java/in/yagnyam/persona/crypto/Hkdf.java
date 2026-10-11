package in.yagnyam.persona.crypto;

import java.security.GeneralSecurityException;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * RFC 5869 HKDF over HMAC-SHA256, used by the v1 X25519 key-agreement
 * construction (protocol §8). Package-private on purpose: the derivation
 * parameters (salt, info, length) are policy of that construction, not a
 * public primitive — callers take their parameters from the active
 * {@link PersonaVersion}.
 */
final class Hkdf {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int HASH_LENGTH_BYTES = 32;

  private Hkdf() {}

  /**
   * RFC 5869 extract-then-expand HKDF with HMAC-SHA256.
   *
   * @param ikm    input keying material, never {@code null}
   * @param salt   extraction salt, never {@code null} (RFC 5869: an empty
   *               salt means the all-zeros hash-length key)
   * @param info   context/application binding, never {@code null} (may be
   *               empty)
   * @param length output length in bytes: at least 1, at most
   *               {@code 255 * 32}
   * @return {@code length} bytes of output keying material
   * @throws IllegalArgumentException when {@code length} is out of range
   */
  static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int length) {
    Objects.requireNonNull(ikm, "ikm");
    Objects.requireNonNull(salt, "salt");
    Objects.requireNonNull(info, "info");
    if (length < 1) {
      throw new IllegalArgumentException("HKDF output length must be positive: " + length);
    }
    if (length > 255 * HASH_LENGTH_BYTES) {
      throw new IllegalArgumentException(
          "HKDF output length above " + 255 * HASH_LENGTH_BYTES + " bytes: " + length);
    }
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(
          salt.length == 0 ? new byte[HASH_LENGTH_BYTES] : salt, HMAC_ALGORITHM));
      byte[] pseudoRandomKey = mac.doFinal(ikm);
      mac.init(new SecretKeySpec(pseudoRandomKey, HMAC_ALGORITHM));
      byte[] output = new byte[length];
      byte[] previous = new byte[0];
      int produced = 0;
      for (int counter = 1; produced < length; counter++) {
        mac.reset();
        mac.update(previous);
        mac.update(info);
        mac.update((byte) counter);
        previous = mac.doFinal();
        int chunk = Math.min(HASH_LENGTH_BYTES, length - produced);
        System.arraycopy(previous, 0, output, produced, chunk);
        produced += chunk;
      }
      return output;
    } catch (GeneralSecurityException e) {
      // HmacSHA256 is mandated for every JCA implementation; unreachable.
      throw new IllegalStateException("HmacSHA256 unavailable", e);
    }
  }
}
