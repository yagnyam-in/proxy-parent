package in.yagnyam.persona.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;

/**
 * BouncyCastle implementation of {@link CryptographyService} (protocol §8).
 *
 * <p>Whitelists are the union over every known {@link PersonaVersion}
 * ({@code v0} and {@code v1}, protocol §8):
 * <ul>
 *   <li>Key generation: {@code RSA}, {@code Ed25519}, {@code X25519}</li>
 *   <li>Asymmetric encryption: {@code RSA/NONE/OAEPwithSHA-256andMGF1Padding}
 *       and {@code X25519/HKDF-SHA256/AES-256-GCM}</li>
 *   <li>Symmetric encryption: {@code AES/CTR/NoPadding}</li>
 * </ul>
 * Anything else — including a symmetric algorithm passed to an asymmetric
 * operation and vice versa — is rejected with a clear
 * {@link IllegalArgumentException}; a new Persona Version must extend these
 * whitelists together with its policy table.
 *
 * <p>Signature and MAC inputs are raw bytes; IVs come from a
 * {@link SecureRandom} so that no IV is ever reused (protocol §12).
 */
public final class BouncyCastleCryptographyService implements CryptographyService {

  private static final BouncyCastleCryptographyService INSTANCE = new BouncyCastleCryptographyService();

  private static final String RSA_OAEP_ALGORITHM = "RSA/NONE/OAEPwithSHA-256andMGF1Padding";

  /**
   * v1 asymmetric encryption (protocol §8): ephemeral-static X25519, HKDF-SHA256,
   * AES-256-GCM. Not a JCE transformation string — it is dispatched by
   * {@link #encryptX25519} / {@link #decryptX25519}.
   */
  private static final String X25519_ALGORITHM = PersonaVersionV1.X25519_ENCRYPTION_ALGORITHM;

  /** Asymmetric (certificate-key) encryption algorithms of every known version. */
  private static final Set<String> SUPPORTED_ASYMMETRIC_CIPHER_ALGORITHMS =
      Set.of(RSA_OAEP_ALGORITHM, X25519_ALGORITHM);

  /** Symmetric encryption algorithms of every known version. */
  private static final Set<String> SUPPORTED_SYMMETRIC_CIPHER_ALGORITHMS = Set.of("AES/CTR/NoPadding");

  private static final Set<String> SUPPORTED_CIPHER_ALGORITHMS = Stream
      .concat(SUPPORTED_ASYMMETRIC_CIPHER_ALGORITHMS.stream(), SUPPORTED_SYMMETRIC_CIPHER_ALGORITHMS.stream())
      .collect(Collectors.toUnmodifiableSet());

  /** Curve-typed key families: generated without a key size (policy keySize 0). */
  private static final Set<String> CURVE_KEY_GENERATION_ALGORITHMS = Set.of("Ed25519", "X25519");

  /** Key generation algorithms of every known version. */
  private static final Set<String> SUPPORTED_KEY_GENERATION_ALGORITHMS = Set.of("RSA", "Ed25519", "X25519");

  private static final byte[] X25519_HKDF_INFO_LABEL =
      PersonaVersionV1.X25519_HKDF_INFO_LABEL.getBytes(StandardCharsets.US_ASCII);

  private static final int X25519_KEY_BYTES = 32;
  private static final int AES_256_KEY_BYTES = 32;
  private static final int GCM_IV_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;

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
   * symmetric together). Package-private: tests assert this set equals the
   * union of every known version's valid encryption algorithms, so the
   * derive-or-verify whitelist and the version policies can never silently
   * diverge.
   *
   * @return supported cipher algorithms
   */
  static Set<String> supportedCipherAlgorithms() {
    return SUPPORTED_CIPHER_ALGORITHMS;
  }

  /**
   * Key generation algorithms supported by this implementation.
   * Package-private: tests assert this set equals the union of every known
   * version's signing and encryption key families.
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
    // initVerify(Certificate) enforces the certificate's critical keyUsage
    // (digitalSignature); that restriction is intentional here — see
    // CryptographyService#verifySignature(String, PublicKey, byte[], byte[])
    // for the keyUsage-neutral variant.
    Signature verifier = Signature.getInstance(algorithm, BouncyCastle.provider());
    verifier.initVerify(certificate);
    verifier.update(input);
    return verifier.verify(signature);
  }

  @Override
  public boolean verifySignature(String algorithm, PublicKey key, byte[] input, byte[] signature)
      throws GeneralSecurityException {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(signature, "signature");
    Signature verifier = Signature.getInstance(algorithm, BouncyCastle.provider());
    verifier.initVerify(key);
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
    if (CURVE_KEY_GENERATION_ALGORITHMS.contains(algorithm)) {
      // Curve-typed families have a fixed size; the policy pins keySize 0
      // (protocol §8), so any other value is a caller error, not a hint.
      if (keySize != 0) {
        throw new IllegalArgumentException(
            "Curve-typed key family " + algorithm + " takes key size 0, got " + keySize);
      }
    } else {
      generator.initialize(keySize, secureRandom);
    }
    return generator.generateKeyPair();
  }

  @Override
  public CipherText encrypt(X509Certificate certificate, String algorithm, byte[] input)
      throws GeneralSecurityException {
    Objects.requireNonNull(certificate, "certificate");
    Objects.requireNonNull(input, "input");
    requireAsymmetric(algorithm);
    if (X25519_ALGORITHM.equals(algorithm)) {
      return encryptX25519(certificate.getPublicKey(), input);
    }
    Cipher cipher = Cipher.getInstance(algorithm, BouncyCastle.provider());
    try {
      // No Cipher.init overload takes (Certificate, AlgorithmParameterSpec),
      // so the certificate's public key is passed directly alongside the
      // explicit OAEP parameters.
      cipher.init(Cipher.ENCRYPT_MODE, certificate.getPublicKey(), RSA_OAEP_PARAMETERS);
      byte[] cipherText = cipher.doFinal(input);
      return new CipherText(Optional.empty(), algorithm, base64(cipherText), Optional.empty(),
          Optional.empty(), Optional.empty());
    } catch (RuntimeException e) {
      throw asSecurityException("Asymmetric encryption failed", e);
    }
  }

  @Override
  public byte[] decrypt(PrivateKey key, CipherText cipherText) throws GeneralSecurityException {
    Objects.requireNonNull(key, "key");
    requireValid(cipherText);
    String algorithm = cipherText.encryptionAlgorithm();
    requireAsymmetric(algorithm);
    if (X25519_ALGORITHM.equals(algorithm)) {
      return decryptX25519(key, cipherText);
    }
    if (cipherText.ephemeralPublicKey().isPresent()) {
      throw new GeneralSecurityException(algorithm + " cipher text must not carry an ephemeral public key");
    }
    Cipher cipher = Cipher.getInstance(algorithm, BouncyCastle.provider());
    try {
      cipher.init(Cipher.DECRYPT_MODE, key, RSA_OAEP_PARAMETERS);
      return cipher.doFinal(decodeBase64(cipherText.cipherText(), "cipher text"));
    } catch (RuntimeException e) {
      throw asSecurityException("Asymmetric decryption failed", e);
    }
  }

  /**
   * v1 asymmetric encryption (protocol §8):
   * <ol>
   *   <li>generate a fresh ephemeral X25519 key pair {@code (e, E)};</li>
   *   <li>{@code Z = X25519(e, R)} for the recipient public key {@code R};
   *       an all-zero {@code Z} (small-order {@code R}) is rejected;</li>
   *   <li>{@code K = HKDF-SHA256(ikm = Z, salt = empty,
   *       info = label || E || R, L = 32)} with raw 32-byte keys;</li>
   *   <li>{@code C = AES-256-GCM(K, iv, input)}, fresh 12-byte iv, 128-bit
   *       tag appended, no AAD.</li>
   * </ol>
   * The cipher text carries {@code iv}, {@code C} and {@code E} (as X.509
   * SubjectPublicKeyInfo DER), all base64.
   */
  private CipherText encryptX25519(PublicKey recipientKey, byte[] input) throws GeneralSecurityException {
    byte[] recipient = rawX25519PublicKey(recipientKey.getEncoded(), "recipient certificate key");
    try {
      X25519PrivateKeyParameters ephemeral = new X25519PrivateKeyParameters(secureRandom);
      X25519PublicKeyParameters ephemeralPublic = ephemeral.generatePublicKey();
      byte[] sharedSecret = agree(ephemeral, recipient);
      byte[] contentKey = x25519ContentKey(sharedSecret, ephemeralPublic.getEncoded(), recipient);
      byte[] iv = randomBytes(GCM_IV_BYTES);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastle.provider());
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(contentKey, "AES"),
          new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] sealed = cipher.doFinal(input);
      byte[] ephemeralSpki = new SubjectPublicKeyInfo(
          new AlgorithmIdentifier(EdECObjectIdentifiers.id_X25519), ephemeralPublic.getEncoded())
          .getEncoded();
      return new CipherText(Optional.of(base64(iv)), X25519_ALGORITHM, base64(sealed),
          Optional.empty(), Optional.empty(), Optional.of(base64(ephemeralSpki)));
    } catch (java.io.IOException | RuntimeException e) {
      throw new GeneralSecurityException("X25519 encryption failed", e);
    }
  }

  private byte[] decryptX25519(PrivateKey key, CipherText cipherText) throws GeneralSecurityException {
    if (cipherText.iv().isEmpty() || cipherText.ephemeralPublicKey().isEmpty()) {
      throw new GeneralSecurityException(
          X25519_ALGORITHM + " cipher text requires both an IV and an ephemeral public key");
    }
    if (cipherText.hmacAlgorithm().isPresent()) {
      throw new GeneralSecurityException(
          X25519_ALGORITHM + " is an AEAD and its cipher text must not carry a separate MAC");
    }
    byte[] iv = decodeBase64(cipherText.iv().get(), "IV");
    if (iv.length != GCM_IV_BYTES) {
      throw new GeneralSecurityException("X25519 cipher text IV must be " + GCM_IV_BYTES + " bytes");
    }
    byte[] ephemeral = rawX25519PublicKey(
        decodeBase64(cipherText.ephemeralPublicKey().get(), "ephemeral public key"),
        "ephemeral public key");
    byte[] sealed = decodeBase64(cipherText.cipherText(), "cipher text");
    X25519PrivateKeyParameters recipient = x25519PrivateKey(key);
    try {
      byte[] sharedSecret = agree(recipient, ephemeral);
      byte[] contentKey = x25519ContentKey(
          sharedSecret, ephemeral, recipient.generatePublicKey().getEncoded());
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastle.provider());
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(contentKey, "AES"),
          new GCMParameterSpec(GCM_TAG_BITS, iv));
      return cipher.doFinal(sealed);
    } catch (AEADBadTagException e) {
      throw new GeneralSecurityException("X25519 decryption failed: authentication tag mismatch", e);
    } catch (RuntimeException e) {
      throw new GeneralSecurityException("X25519 decryption failed", e);
    }
  }

  private static byte[] agree(X25519PrivateKeyParameters privateKey, byte[] peerPublicKey)
      throws GeneralSecurityException {
    X25519Agreement agreement = new X25519Agreement();
    agreement.init(privateKey);
    byte[] sharedSecret = new byte[agreement.getAgreementSize()];
    try {
      agreement.calculateAgreement(new X25519PublicKeyParameters(peerPublicKey, 0), sharedSecret, 0);
    } catch (IllegalStateException e) {
      // BouncyCastle refuses an all-zero result (small-order peer key).
      throw new GeneralSecurityException("X25519 agreement produced an all-zero shared secret", e);
    }
    // Defense in depth, independent of the provider's own check (RFC 7748 §6.1).
    if (isAllZero(sharedSecret)) {
      throw new GeneralSecurityException("X25519 agreement produced an all-zero shared secret");
    }
    return sharedSecret;
  }

  private static byte[] x25519ContentKey(byte[] sharedSecret, byte[] ephemeral, byte[] recipient) {
    byte[] info = new byte[X25519_HKDF_INFO_LABEL.length + 2 * X25519_KEY_BYTES];
    System.arraycopy(X25519_HKDF_INFO_LABEL, 0, info, 0, X25519_HKDF_INFO_LABEL.length);
    System.arraycopy(ephemeral, 0, info, X25519_HKDF_INFO_LABEL.length, X25519_KEY_BYTES);
    System.arraycopy(recipient, 0, info, X25519_HKDF_INFO_LABEL.length + X25519_KEY_BYTES, X25519_KEY_BYTES);
    return Hkdf.hkdfSha256(sharedSecret, new byte[0], info, AES_256_KEY_BYTES);
  }

  /** Extracts the raw 32-byte X25519 key of a SubjectPublicKeyInfo DER, requiring id-X25519. */
  private static byte[] rawX25519PublicKey(byte[] spkiDer, String role) throws GeneralSecurityException {
    SubjectPublicKeyInfo info;
    try {
      info = SubjectPublicKeyInfo.getInstance(spkiDer);
    } catch (RuntimeException e) {
      throw new GeneralSecurityException("Malformed " + role + " (not a SubjectPublicKeyInfo)", e);
    }
    requireX25519(info.getAlgorithm().getAlgorithm(), role);
    byte[] raw;
    try {
      raw = info.getPublicKeyData().getOctets();
    } catch (RuntimeException e) {
      // A BIT STRING with non-zero unused bits makes BouncyCastle throw
      // IllegalStateException; keep the declared checked contract.
      throw new GeneralSecurityException("Malformed " + role + " (non-octet-aligned key bits)", e);
    }
    if (raw.length != X25519_KEY_BYTES) {
      throw new GeneralSecurityException(role + " must be a " + X25519_KEY_BYTES + "-byte X25519 key");
    }
    return raw;
  }

  private static X25519PrivateKeyParameters x25519PrivateKey(PrivateKey key) throws GeneralSecurityException {
    try {
      PrivateKeyInfo info = PrivateKeyInfo.getInstance(key.getEncoded());
      requireX25519(info.getPrivateKeyAlgorithm().getAlgorithm(), "decryption key");
      byte[] raw = ASN1OctetString.getInstance(info.parsePrivateKey()).getOctets();
      if (raw.length != X25519_KEY_BYTES) {
        throw new GeneralSecurityException(
            "decryption key must be a " + X25519_KEY_BYTES + "-byte X25519 key");
      }
      return new X25519PrivateKeyParameters(raw, 0);
    } catch (java.io.IOException | RuntimeException e) {
      throw new GeneralSecurityException("Malformed X25519 decryption key", e);
    }
  }

  private static void requireX25519(ASN1ObjectIdentifier algorithm, String role)
      throws GeneralSecurityException {
    if (!EdECObjectIdentifiers.id_X25519.equals(algorithm)) {
      throw new java.security.InvalidKeyException(
          X25519_ALGORITHM + " requires an X25519 " + role + ", got key algorithm OID " + algorithm);
    }
  }

  private static boolean isAllZero(byte[] bytes) {
    int accumulator = 0;
    for (byte b : bytes) {
      accumulator |= b;
    }
    return accumulator == 0;
  }

  @Override
  public CipherText encrypt(SecretKey key, String algorithm, byte[] input) throws GeneralSecurityException {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(input, "input");
    requireSymmetric(algorithm);
    byte[] iv = randomBytes(AES_BLOCK_SIZE_BYTES);
    Cipher cipher = Cipher.getInstance(algorithm, BouncyCastle.provider());
    try {
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
      byte[] cipherText = cipher.doFinal(input);
      return new CipherText(Optional.of(base64(iv)), algorithm, base64(cipherText),
          Optional.empty(), Optional.empty(), Optional.empty());
    } catch (RuntimeException e) {
      throw asSecurityException("Symmetric encryption failed", e);
    }
  }

  @Override
  public byte[] decrypt(SecretKey key, CipherText cipherText) throws GeneralSecurityException {
    Objects.requireNonNull(key, "key");
    requireValid(cipherText);
    requireSymmetric(cipherText.encryptionAlgorithm());
    if (cipherText.iv().isEmpty()) {
      throw new GeneralSecurityException("Symmetric cipher text requires an IV");
    }
    if (cipherText.ephemeralPublicKey().isPresent()) {
      throw new GeneralSecurityException("Symmetric cipher text must not carry an ephemeral public key");
    }
    byte[] iv;
    try {
      iv = Base64.getDecoder().decode(cipherText.iv().get());
    } catch (IllegalArgumentException e) {
      throw new GeneralSecurityException("Cipher text carries a malformed IV", e);
    }
    Cipher cipher = Cipher.getInstance(cipherText.encryptionAlgorithm(), BouncyCastle.provider());
    try {
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
      return cipher.doFinal(decodeBase64(cipherText.cipherText(), "cipher text"));
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

  private static void requireAsymmetric(String algorithm) {
    Objects.requireNonNull(algorithm, "algorithm");
    if (!SUPPORTED_ASYMMETRIC_CIPHER_ALGORITHMS.contains(algorithm)) {
      throw new IllegalArgumentException("Unsupported asymmetric encryption algorithm: " + algorithm
          + " (supported: " + SUPPORTED_ASYMMETRIC_CIPHER_ALGORITHMS + ")");
    }
  }

  private static void requireSymmetric(String algorithm) {
    Objects.requireNonNull(algorithm, "algorithm");
    if (!SUPPORTED_SYMMETRIC_CIPHER_ALGORITHMS.contains(algorithm)) {
      throw new IllegalArgumentException("Unsupported symmetric encryption algorithm: " + algorithm
          + " (supported: " + SUPPORTED_SYMMETRIC_CIPHER_ALGORITHMS + ")");
    }
  }

  private static byte[] decodeBase64(String value, String what) throws GeneralSecurityException {
    try {
      return Base64.getDecoder().decode(value);
    } catch (IllegalArgumentException e) {
      throw new GeneralSecurityException("Cipher text carries a malformed " + what, e);
    }
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
