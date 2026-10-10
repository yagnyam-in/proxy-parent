package in.yagnyam.persona.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/**
 * BouncyCastle implementation of {@link CryptographyService} (protocol §8).
 *
 * <p>v0 whitelists (mirroring the {@code PersonaVersionV0} table, protocol
 * §8):
 * <ul>
 *   <li>Key generation: {@code RSA} only</li>
 *   <li>Cipher algorithms: {@code RSA/NONE/OAEPwithSHA-256andMGF1Padding} and
 *       {@code AES/CTR/NoPadding}</li>
 * </ul>
 * Anything else is rejected with a clear {@link IllegalArgumentException};
 * a future Persona Version must extend these whitelists together with its
 * policy table.
 *
 * <p>Signature and MAC inputs are raw bytes; IVs come from a
 * {@link SecureRandom} so that no IV is ever reused (protocol §12).
 */
public final class BouncyCastleCryptographyService implements CryptographyService {

  private static final BouncyCastleCryptographyService INSTANCE = new BouncyCastleCryptographyService();

  /** Cipher algorithms supported by v0 (protocol §8). */
  private static final Set<String> SUPPORTED_CIPHER_ALGORITHMS = Set.of(
      "RSA/NONE/OAEPwithSHA-256andMGF1Padding", "AES/CTR/NoPadding");

  /** Key generation algorithms supported by v0 (protocol §8). */
  private static final Set<String> SUPPORTED_KEY_GENERATION_ALGORITHMS = Set.of("RSA");

  /** Salt length for {@link Hash} and {@link Hmac}. */
  private static final int SALT_LENGTH_BYTES = 32;

  /** IV length for AES: always the block size, independent of key size. */
  private static final int AES_BLOCK_SIZE_BYTES = 16;

  /**
   * Exact RSA-OAEP parameters (protocol §8): SHA-256 digest, MGF1 with
   * SHA-256, empty label. JCE transformation strings do not pin the MGF1
   * digest — providers disagree on the default (commonly SHA-1) — so both
   * asymmetric encryption and decryption initialize with these explicit
   * parameters instead of relying on provider defaults.
   */
  private static final OAEPParameterSpec RSA_OAEP_PARAMETERS = new OAEPParameterSpec(
      "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

  private final SecureRandom secureRandom = new SecureRandom();

  private BouncyCastleCryptographyService() {
    BouncyCastle.provider();
  }

  /**
   * Returns the shared, thread-safe instance.
   *
   * @return singleton cryptography service
   */
  public static BouncyCastleCryptographyService instance() {
    return INSTANCE;
  }

  /**
   * Cipher algorithms supported by this implementation (asymmetric and
   * symmetric together). Package-private: tests assert this set equals
   * {@code PersonaVersion.latestVersion()}'s valid encryption algorithms, so
   * the derive-or-verify whitelist and the version policy can never silently
   * diverge.
   *
   * @return supported cipher algorithms
   */
  static Set<String> supportedCipherAlgorithms() {
    return SUPPORTED_CIPHER_ALGORITHMS;
  }

  /**
   * Key generation algorithms supported by this implementation.
   * Package-private: tests assert this set equals
   * {@code PersonaVersion.latestVersion()}'s key generation algorithm.
   *
   * @return supported key generation algorithms
   */
  static Set<String> supportedKeyGenerationAlgorithms() {
    return SUPPORTED_KEY_GENERATION_ALGORITHMS;
  }

  @Override
  public byte[] getSignature(String algorithm, PrivateKey key, byte[] input) throws GeneralSecurityException {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(input, "input");
    Signature signature = Signature.getInstance(algorithm, BouncyCastle.provider());
    signature.initSign(key);
    signature.update(input);
    return signature.sign();
  }

  @Override
  public boolean verifySignature(String algorithm, X509Certificate certificate, byte[] input, byte[] signature)
      throws GeneralSecurityException {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(certificate, "certificate");
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(signature, "signature");
    Signature verifier = Signature.getInstance(algorithm, BouncyCastle.provider());
    verifier.initVerify(certificate);
    verifier.update(input);
    return verifier.verify(signature);
  }

  @Override
  public Hash getHash(String algorithm, byte[] message) throws GeneralSecurityException {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(message, "message");
    byte[] salt = randomBytes(SALT_LENGTH_BYTES);
    MessageDigest digest = MessageDigest.getInstance(algorithm);
    digest.update(salt);
    byte[] hash = digest.digest(message);
    return new Hash(algorithm, base64(salt), base64(hash));
  }

  @Override
  public boolean verifyHash(byte[] message, Hash hash) throws GeneralSecurityException {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(hash, "hash");
    if (!hash.isValid()) {
      return false;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance(hash.algorithm());
      digest.update(Base64.getDecoder().decode(hash.iv()));
      byte[] expected = digest.digest(message);
      return MessageDigest.isEqual(expected, Base64.getDecoder().decode(hash.hash()));
    } catch (IllegalArgumentException e) {
      // Malformed base64 in the stored hash: fail closed.
      return false;
    }
  }

  @Override
  public Hmac getHmac(String algorithm, String key, byte[] message) throws GeneralSecurityException {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(message, "message");
    byte[] salt = randomBytes(SALT_LENGTH_BYTES);
    Mac mac = Mac.getInstance(algorithm);
    mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
    mac.update(salt);
    byte[] hmac = mac.doFinal(message);
    return new Hmac(algorithm, base64(salt), base64(hmac));
  }

  @Override
  public boolean verifyHmac(String key, byte[] message, Hmac hmac) throws GeneralSecurityException {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(hmac, "hmac");
    if (!hmac.isValid()) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance(hmac.algorithm());
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), hmac.algorithm()));
      mac.update(Base64.getDecoder().decode(hmac.iv()));
      byte[] expected = mac.doFinal(message);
      return MessageDigest.isEqual(expected, Base64.getDecoder().decode(hmac.hmac()));
    } catch (IllegalArgumentException e) {
      // Malformed base64 in the stored MAC: fail closed.
      return false;
    }
  }

  @Override
  public KeyPair generateKeyPair(String algorithm, int keySize) throws GeneralSecurityException {
    Objects.requireNonNull(algorithm, "algorithm");
    if (!SUPPORTED_KEY_GENERATION_ALGORITHMS.contains(algorithm)) {
      throw new IllegalArgumentException("Unsupported key generation algorithm: " + algorithm
          + " (supported: " + SUPPORTED_KEY_GENERATION_ALGORITHMS + ")");
    }
    KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm, BouncyCastle.provider());
    generator.initialize(keySize, secureRandom);
    return generator.generateKeyPair();
  }

  @Override
  public CipherText encrypt(X509Certificate certificate, String algorithm, byte[] input)
      throws GeneralSecurityException {
    Objects.requireNonNull(certificate, "certificate");
    Objects.requireNonNull(input, "input");
    Cipher cipher = cipher(algorithm);
    try {
      // No Cipher.init overload takes (Certificate, AlgorithmParameterSpec),
      // so the certificate's public key is passed directly alongside the
      // explicit OAEP parameters.
      cipher.init(Cipher.ENCRYPT_MODE, certificate.getPublicKey(), RSA_OAEP_PARAMETERS);
      byte[] cipherText = cipher.doFinal(input);
      return new CipherText(Optional.empty(), algorithm, base64(cipherText), Optional.empty(), Optional.empty());
    } catch (RuntimeException e) {
      throw asSecurityException("Asymmetric encryption failed", e);
    }
  }

  @Override
  public byte[] decrypt(PrivateKey key, CipherText cipherText) throws GeneralSecurityException {
    Objects.requireNonNull(key, "key");
    requireValid(cipherText);
    Cipher cipher = cipher(cipherText.encryptionAlgorithm());
    try {
      cipher.init(Cipher.DECRYPT_MODE, key, RSA_OAEP_PARAMETERS);
      return cipher.doFinal(Base64.getDecoder().decode(cipherText.cipherText()));
    } catch (RuntimeException e) {
      throw asSecurityException("Asymmetric decryption failed", e);
    }
  }

  @Override
  public CipherText encrypt(SecretKey key, String algorithm, byte[] input) throws GeneralSecurityException {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(input, "input");
    byte[] iv = randomBytes(AES_BLOCK_SIZE_BYTES);
    Cipher cipher = cipher(algorithm);
    try {
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
      byte[] cipherText = cipher.doFinal(input);
      return new CipherText(Optional.of(base64(iv)), algorithm, base64(cipherText),
          Optional.empty(), Optional.empty());
    } catch (RuntimeException e) {
      throw asSecurityException("Symmetric encryption failed", e);
    }
  }

  @Override
  public byte[] decrypt(SecretKey key, CipherText cipherText) throws GeneralSecurityException {
    Objects.requireNonNull(key, "key");
    requireValid(cipherText);
    if (cipherText.iv().isEmpty()) {
      throw new GeneralSecurityException("Symmetric cipher text requires an IV");
    }
    byte[] iv;
    try {
      iv = Base64.getDecoder().decode(cipherText.iv().get());
    } catch (IllegalArgumentException e) {
      throw new GeneralSecurityException("Cipher text carries a malformed IV", e);
    }
    Cipher cipher = cipher(cipherText.encryptionAlgorithm());
    try {
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
      return cipher.doFinal(Base64.getDecoder().decode(cipherText.cipherText()));
    } catch (RuntimeException e) {
      throw asSecurityException("Symmetric decryption failed", e);
    }
  }

  /**
   * Providers can surface internal failures as runtime exceptions (for
   * example BouncyCastle's {@code DataLengthException} when an RSA
   * ciphertext numerically exceeds the private key's modulus, e.g. when
   * decrypting with the wrong key); fold them into the declared checked
   * contract so callers only ever see {@link GeneralSecurityException}.
   */
  private static GeneralSecurityException asSecurityException(String message, RuntimeException cause) {
    return new GeneralSecurityException(message, cause);
  }

  private static void requireValid(CipherText cipherText) {
    Objects.requireNonNull(cipherText, "cipherText");
    if (!cipherText.isValid()) {
      throw new IllegalArgumentException("Invalid cipher text: " + cipherText);
    }
  }

  private static Cipher cipher(String algorithm) throws GeneralSecurityException {
    Objects.requireNonNull(algorithm, "algorithm");
    if (!SUPPORTED_CIPHER_ALGORITHMS.contains(algorithm)) {
      throw new IllegalArgumentException("Unsupported encryption algorithm: " + algorithm
          + " (supported: " + SUPPORTED_CIPHER_ALGORITHMS + ")");
    }
    return Cipher.getInstance(algorithm, BouncyCastle.provider());
  }

  private byte[] randomBytes(int length) {
    byte[] bytes = new byte[length];
    secureRandom.nextBytes(bytes);
    return bytes;
  }

  private static String base64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }
}
