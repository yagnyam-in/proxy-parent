package in.yagnyam.persona;

import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.crypto.PersonaVersionV0;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Shared test utilities: independent digest computation (cross-checking the
 * production {@code Digests} code path) and key pairs per Persona Version.
 */
public final class TestSupport {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  private TestSupport() {}

  /**
   * Generates an RSA 2048 key pair per the v0 key generation policy.
   *
   * @return fresh key pair
   */
  public static KeyPair rsaKeyPair() {
    return signingKeyPair(PersonaVersionV0.instance());
  }

  /**
   * Generates a signing key pair per {@code version}'s key generation policy
   * (RSA 2048 under v0, Ed25519 under v1).
   *
   * @param version persona version
   * @return fresh key pair
   */
  public static KeyPair signingKeyPair(PersonaVersion version) {
    try {
      return CRYPTO.generateKeyPair(version.keyGenerationAlgorithm(), version.keySize());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Generates an X25519 key-agreement key pair (v1 encryption key family).
   *
   * @return fresh key pair
   */
  public static KeyPair x25519KeyPair() {
    try {
      return CRYPTO.generateKeyPair("X25519", 0);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * SHA-256 hex digest computed independently of the production
   * {@code Digests}/{@code HexFormat} code path, so tests cross-check the
   * class under test rather than repeating its implementation (equivalent to
   * {@code shasum -a 256} over the DER bytes).
   *
   * @param input bytes to digest
   * @return 64-character lowercase hex digest
   */
  public static String sha256Hex(byte[] input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(input);
      StringBuilder hex = new StringBuilder(hashed.length * 2);
      for (byte b : hashed) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
