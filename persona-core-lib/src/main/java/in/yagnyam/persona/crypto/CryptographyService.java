package in.yagnyam.persona.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cryptographic primitives of the Persona Protocol (protocol §8).
 *
 * <p>Unlike the legacy String-based service (see {@code SPEC.md} §3.2), all
 * payloads are {@code byte[]}: envelopes carry opaque bytes that are signed
 * exactly once and verified as the same exact bytes (protocol §6.1, §11
 * rule 10), so the crypto layer must never force a text round-trip.
 *
 * <p>Algorithm names are always caller-supplied strings; the accepted values
 * are pinned by the active {@link PersonaVersion}, never by this interface.
 */
public interface CryptographyService {

  /**
   * Signs {@code input} with {@code key} using {@code algorithm}.
   *
   * @param algorithm signature algorithm (e.g. {@code SHA256withRSA})
   * @param key       signing key
   * @param input     bytes to sign
   * @return signature bytes
   * @throws GeneralSecurityException if signing fails
   */
  byte[] getSignature(String algorithm, PrivateKey key, byte[] input) throws GeneralSecurityException;

  /**
   * Verifies {@code signature} over {@code input} with the public key of
   * {@code certificate}.
   *
   * <p>Note: X.509 {@code keyUsage} applies here. A JCA
   * {@code Signature.initVerify(Certificate)} refuses a certificate whose
   * critical {@code keyUsage} does not assert {@code digitalSignature} — the
   * shape of an ordinary CA root, which asserts {@code keyCertSign|cRLSign}
   * only. Use {@link #verifySignature(String, PublicKey, byte[], byte[])}
   * whenever the verification must be decided on cryptography alone (for
   * example proving that a private key matches a CA root certificate).
   *
   * @param algorithm   signature algorithm
   * @param certificate certificate carrying the verification key
   * @param input       the signed bytes
   * @param signature   signature to verify
   * @return {@code true} if the signature verifies
   * @throws GeneralSecurityException if verification cannot be performed
   */
  boolean verifySignature(String algorithm, X509Certificate certificate, byte[] input, byte[] signature)
      throws GeneralSecurityException;

  /**
   * Verifies {@code signature} over {@code input} with the bare public key
   * {@code key}, bypassing certificate {@code keyUsage} semantics (see
   * {@link #verifySignature(String, X509Certificate, byte[], byte[])}).
   *
   * @param algorithm signature algorithm
   * @param key       verification (public) key
   * @param input     the signed bytes
   * @param signature signature to verify
   * @return {@code true} if the signature verifies
   * @throws GeneralSecurityException if verification cannot be performed
   */
  boolean verifySignature(String algorithm, PublicKey key, byte[] input, byte[] signature)
      throws GeneralSecurityException;

  /**
   * Computes a salted hash of {@code message} (protocol §7.1: passphrase
   * hashing; §8): the salt is fresh random bytes, so equal messages hash
   * differently every time.
   *
   * @param algorithm hash algorithm (e.g. {@code SHA-256})
   * @param message   bytes to hash
   * @return salted hash
   * @throws GeneralSecurityException if hashing fails
   */
  Hash getHash(String algorithm, byte[] message) throws GeneralSecurityException;

  /**
   * Verifies that {@code message} produces {@code hash} when hashed with the
   * salt carried by {@code hash}.
   *
   * @param message bytes to re-hash
   * @param hash    stored salted hash
   * @return {@code true} if the digest matches; {@code false} for a mismatch
   *     or a malformed/undecodable hash record (fails closed)
   * @throws GeneralSecurityException if the hash algorithm is unknown
   */
  boolean verifyHash(byte[] message, Hash hash) throws GeneralSecurityException;

  /**
   * Computes a keyed MAC of {@code message} with a fresh random salt prefix.
   *
   * @param algorithm MAC algorithm (e.g. {@code HmacSHA256})
   * @param key      secret key material
   * @param message  bytes to MAC
   * @return salted HMAC
   * @throws GeneralSecurityException if the MAC cannot be computed
   */
  Hmac getHmac(String algorithm, String key, byte[] message) throws GeneralSecurityException;

  /**
   * Verifies that {@code message} produces {@code hmac} under {@code key}.
   *
   * @param key     secret key material
   * @param message bytes to MAC
   * @param hmac    stored salted HMAC
   * @return {@code true} if the MAC matches; {@code false} for a mismatch or
   *     a malformed/undecodable record (fails closed)
   * @throws GeneralSecurityException if the MAC algorithm is unknown
   */
  boolean verifyHmac(String key, byte[] message, Hmac hmac) throws GeneralSecurityException;

  /**
   * Generates a fresh key pair.
   *
   * @param algorithm key generation algorithm (v0: {@code RSA} only)
   * @param keySize  key size in bits
   * @return new key pair
   * @throws GeneralSecurityException if generation fails
   */
  KeyPair generateKeyPair(String algorithm, int keySize) throws GeneralSecurityException;

  /**
   * Encrypts {@code input} with the public key of {@code certificate}
   * (asymmetric encryption, protocol §8).
   *
   * @param certificate certificate carrying the encryption (public) key
   * @param algorithm   asymmetric encryption algorithm (v0:
   *                    {@code RSA/NONE/OAEPwithSHA-256andMGF1Padding})
   * @param input       bytes to encrypt
   * @return cipher text (no IV for RSA-OAEP)
   * @throws GeneralSecurityException if encryption fails
   */
  CipherText encrypt(X509Certificate certificate, String algorithm, byte[] input)
      throws GeneralSecurityException;

  /**
   * Decrypts {@code cipherText} with {@code key}.
   *
   * @param key        decryption (private) key
   * @param cipherText cipher text produced by
   *                   {@link #encrypt(X509Certificate, String, byte[])}
   * @return original bytes
   * @throws GeneralSecurityException if decryption fails
   */
  byte[] decrypt(PrivateKey key, CipherText cipherText) throws GeneralSecurityException;

  /**
   * Encrypts {@code input} with {@code key} using a fresh random IV
   * (protocol §12: IVs must never be reused; the IV is carried in the
   * returned cipher text).
   *
   * @param key       symmetric key
   * @param algorithm symmetric encryption algorithm (v0:
   *                  {@code AES/CTR/NoPadding})
   * @param input     bytes to encrypt
   * @return cipher text including the IV
   * @throws GeneralSecurityException if encryption fails
   */
  CipherText encrypt(SecretKey key, String algorithm, byte[] input) throws GeneralSecurityException;

  /**
   * Decrypts {@code cipherText} with {@code key}.
   *
   * @param key        symmetric key
   * @param cipherText cipher text produced by
   *                   {@link #encrypt(SecretKey, String, byte[])}
   * @return original bytes
   * @throws GeneralSecurityException if decryption fails
   */
  byte[] decrypt(SecretKey key, CipherText cipherText) throws GeneralSecurityException;

  /**
   * Adjusts the UTF-8 bytes of {@code key} into a valid AES key by
   * zero-padding to the smallest valid AES length that is at least the raw
   * length (16, 24 or 32 bytes).
   *
   * <p>Key material longer than 32 bytes is rejected with
   * {@link IllegalArgumentException} (legacy semantics, protocol §8):
   * silently truncating it would weaken key separation — two different
   * passphrases sharing a 32-byte prefix would silently become the same
   * AES key.
   *
   * <p>Regression note: the legacy implementation padded lengths in the
   * range (24, 32) to 36 bytes, an invalid AES key size that exploded at
   * cipher-init time. This implementation pads such lengths to 32.
   *
   * @param key passphrase-style key material, at most 32 UTF-8 bytes
   * @return AES key of 16, 24 or 32 bytes
   * @throws IllegalArgumentException when the key material exceeds 32 bytes
   */
  static SecretKey asAesKey(String key) {
    Objects.requireNonNull(key, "key");
    byte[] raw = key.getBytes(StandardCharsets.UTF_8);
    int length = raw.length;
    if (length > 32) {
      throw new IllegalArgumentException("Invalid length " + length
          + " for AES key: key material longer than 32 bytes is rejected, not truncated");
    }
    int adjustedLength;
    if (length <= 16) {
      adjustedLength = 16;
    } else if (length <= 24) {
      adjustedLength = 24;
    } else {
      adjustedLength = 32;
    }
    return new SecretKeySpec(Arrays.copyOf(raw, adjustedLength), "AES");
  }
}
