package in.yagnyam.persona.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Digest helpers for certificate and key thumbprints (protocol §3.2, §8).
 *
 * <p>SHA-256 is the canonical hash algorithm of Persona Version v0 (§8), and
 * the persona-id thumbprint grammar itself is defined over 64-character
 * lowercase-hex SHA-256 digests (§3.2); this class is therefore structural
 * rather than policy-driven. The active {@link PersonaVersion} remains the
 * policy source for new hash uses, such as revocation-passphrase hashing,
 * which always flows through {@link CryptographyService#getHash}.
 */
public final class Digests {

  private Digests() {}

  /**
   * SHA-256 digest of {@code input} as lowercase hex.
   *
   * @param input bytes to digest, never {@code null}
   * @return 64-character lowercase hex digest
   */
  public static String sha256Hex(byte[] input) {
    Objects.requireNonNull(input, "input");
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(input));
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandated for every JCA implementation; unreachable.
      throw new IllegalStateException("SHA-256 digest unavailable", e);
    }
  }
}
