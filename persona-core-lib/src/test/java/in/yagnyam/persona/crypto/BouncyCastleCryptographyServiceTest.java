package in.yagnyam.persona.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.TestCertificates;
import in.yagnyam.persona.TestSupport;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BouncyCastleCryptographyService} (protocol §8, §12):
 * sign/verify round-trips and tamper detection, salted hashes, HMACs,
 * asymmetric and symmetric encryption with fresh IVs, AES key sizing
 * regression, and algorithm whitelists.
 */
class BouncyCastleCryptographyServiceTest {

  private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
  private static final String ASYMMETRIC_ALGORITHM = "RSA/NONE/OAEPwithSHA-256andMGF1Padding";
  private static final String SYMMETRIC_ALGORITHM = "AES/CTR/NoPadding";
  private static final String HASH_ALGORITHM = "SHA-256";
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final BouncyCastleCryptographyService cryptographyService = BouncyCastleCryptographyService.instance();

  private KeyPair keyPair;
  private X509Certificate certificate;
  private byte[] payload;

  @BeforeEach
  void setUp() {
    keyPair = TestSupport.rsaKeyPair();
    certificate = TestCertificates.selfSignedWithCommonName(keyPair, "p-crypto-test", Duration.ofDays(1));
    payload = "hello persona protocol".getBytes(StandardCharsets.UTF_8);
  }

  @Nested
  class Signatures {

    @Test
    void signsAndVerifiesRoundTrip() throws GeneralSecurityException {
      byte[] signature = cryptographyService.getSignature(SIGNATURE_ALGORITHM, keyPair.getPrivate(), payload);
      assertThat(cryptographyService.verifySignature(SIGNATURE_ALGORITHM, certificate, payload, signature))
          .isTrue();
    }

    @Test
    void detectsTamperedPayload() throws GeneralSecurityException {
      byte[] signature = cryptographyService.getSignature(SIGNATURE_ALGORITHM, keyPair.getPrivate(), payload);
      byte[] tampered = Arrays.copyOf(payload, payload.length);
      tampered[0] ^= 0x01;
      assertThat(cryptographyService.verifySignature(SIGNATURE_ALGORITHM, certificate, tampered, signature))
          .isFalse();
    }

    @Test
    void detectsTamperedSignature() throws GeneralSecurityException {
      byte[] signature = cryptographyService.getSignature(SIGNATURE_ALGORITHM, keyPair.getPrivate(), payload);
      signature[0] ^= 0x01;
      assertThat(cryptographyService.verifySignature(SIGNATURE_ALGORITHM, certificate, payload, signature))
          .isFalse();
    }

    @Test
    void rejectsSignatureOfDifferentKey() throws GeneralSecurityException {
      byte[] signature = cryptographyService.getSignature(SIGNATURE_ALGORITHM, keyPair.getPrivate(), payload);
      X509Certificate otherCertificate = TestCertificates.selfSignedWithCommonName(
          TestSupport.rsaKeyPair(), "p-other", Duration.ofDays(1));
      assertThat(cryptographyService.verifySignature(SIGNATURE_ALGORITHM, otherCertificate, payload, signature))
          .isFalse();
    }

    @Test
    void rejectsUnknownAlgorithm() {
      assertThatThrownBy(() ->
          cryptographyService.getSignature("NoSuchAlgorithm", keyPair.getPrivate(), payload))
          .isInstanceOf(GeneralSecurityException.class);
    }

    /**
     * G4 (P1): the certificate-based overload enforces X.509 {@code keyUsage}
     * — a CA certificate asserting only {@code keyCertSign | cRLSign} is
     * refused, because it does not assert {@code digitalSignature} — while the
     * public-key overload decides on cryptography alone. That difference is
     * what the registrar's root-key probe relies on (protocol §7.1): a real CA
     * root must still be provably paired with its own key.
     */
    @Test
    void verifiesWithBarePublicKeyWhenCertificateKeyUsageForbidsSignature()
        throws GeneralSecurityException {
      KeyPair caKeyPair = TestSupport.rsaKeyPair();
      X509Certificate caCertificate = TestCertificates.selfSignedCaRoot(
          caKeyPair, "ca-root", Duration.ofDays(1));
      assertThat(caCertificate.getCriticalExtensionOIDs()).contains("2.5.29.15");
      assertThat(caCertificate.getKeyUsage()[0]).isFalse();
      byte[] signature =
          cryptographyService.getSignature(SIGNATURE_ALGORITHM, caKeyPair.getPrivate(), payload);

      assertThatThrownBy(() -> cryptographyService.verifySignature(
          SIGNATURE_ALGORITHM, caCertificate, payload, signature))
          .isInstanceOf(GeneralSecurityException.class);
      assertThat(cryptographyService.verifySignature(
          SIGNATURE_ALGORITHM, caCertificate.getPublicKey(), payload, signature)).isTrue();
      // The keyUsage-neutral overload still rejects a foreign key.
      assertThat(cryptographyService.verifySignature(
          SIGNATURE_ALGORITHM, TestSupport.rsaKeyPair().getPublic(), payload, signature)).isFalse();
    }
  }

  @Nested
  class SaltedHashes {

    @Test
    void hashesAndVerifies() throws GeneralSecurityException {
      Hash hash = cryptographyService.getHash(HASH_ALGORITHM, payload);
      assertThat(hash.algorithm()).isEqualTo(HASH_ALGORITHM);
      assertThat(cryptographyService.verifyHash(payload, hash)).isTrue();
    }

    @Test
    void freshSaltPerHash() throws GeneralSecurityException {
      Hash first = cryptographyService.getHash(HASH_ALGORITHM, payload);
      Hash second = cryptographyService.getHash(HASH_ALGORITHM, payload);
      assertThat(first.iv()).isNotEqualTo(second.iv());
      assertThat(first.hash()).isNotEqualTo(second.hash());
      assertThat(cryptographyService.verifyHash(payload, second)).isTrue();
    }

    @Test
    void detectsWrongMessage() throws GeneralSecurityException {
      Hash hash = cryptographyService.getHash(HASH_ALGORITHM, payload);
      assertThat(cryptographyService.verifyHash("wrong message".getBytes(StandardCharsets.UTF_8), hash))
          .isFalse();
    }

    @Test
    void failsClosedOnMalformedHash() throws GeneralSecurityException {
      assertThat(cryptographyService.verifyHash(payload, Hash.of(HASH_ALGORITHM, "not base64 !!!", "AAAA")))
          .isFalse();
      assertThat(cryptographyService.verifyHash(payload, Hash.of(HASH_ALGORITHM, "", ""))).isFalse();
    }
  }

  @Nested
  class Hmacs {

    @Test
    void macsAndVerifiesRoundTrip() throws GeneralSecurityException {
      Hmac hmac = cryptographyService.getHmac(HMAC_ALGORITHM, "secret-key", payload);
      assertThat(hmac.algorithm()).isEqualTo(HMAC_ALGORITHM);
      assertThat(cryptographyService.verifyHmac("secret-key", payload, hmac)).isTrue();
    }

    @Test
    void rejectsWrongKey() throws GeneralSecurityException {
      Hmac hmac = cryptographyService.getHmac(HMAC_ALGORITHM, "secret-key", payload);
      assertThat(cryptographyService.verifyHmac("other-key", payload, hmac)).isFalse();
    }

    @Test
    void rejectsWrongMessage() throws GeneralSecurityException {
      Hmac hmac = cryptographyService.getHmac(HMAC_ALGORITHM, "secret-key", payload);
      assertThat(cryptographyService.verifyHmac("secret-key",
          "other message".getBytes(StandardCharsets.UTF_8), hmac)).isFalse();
    }

    /** Mirrors the Hash fail-closed test: malformed records never verify. */
    @Test
    void failsClosedOnMalformedHmac() throws GeneralSecurityException {
      assertThat(cryptographyService.verifyHmac("secret-key", payload,
          new Hmac(HMAC_ALGORITHM, "not base64 !!!", "AAAA"))).isFalse();
      assertThat(cryptographyService.verifyHmac("secret-key", payload,
          new Hmac(HMAC_ALGORITHM, "", ""))).isFalse();
    }

    @Test
    void freshSaltPerHmac() throws GeneralSecurityException {
      Hmac first = cryptographyService.getHmac(HMAC_ALGORITHM, "secret-key", payload);
      Hmac second = cryptographyService.getHmac(HMAC_ALGORITHM, "secret-key", payload);
      assertThat(first.iv()).isNotEqualTo(second.iv());
      assertThat(first.hmac()).isNotEqualTo(second.hmac());
    }
  }

  @Nested
  class AsymmetricEncryption {

    @Test
    void encryptsAndDecryptsRoundTrip() throws GeneralSecurityException {
      CipherText cipherText = cryptographyService.encrypt(certificate, ASYMMETRIC_ALGORITHM, payload);
      assertThat(cipherText.iv()).isEmpty();
      assertThat(cipherText.encryptionAlgorithm()).isEqualTo(ASYMMETRIC_ALGORITHM);
      byte[] decrypted = cryptographyService.decrypt(keyPair.getPrivate(), cipherText);
      assertThat(decrypted).isEqualTo(payload);
    }

    @Test
    void wrongKeyCannotDecrypt() throws GeneralSecurityException {
      CipherText cipherText = cryptographyService.encrypt(certificate, ASYMMETRIC_ALGORITHM, payload);
      KeyPair wrongKeyPair = TestSupport.rsaKeyPair();
      assertThatThrownBy(() -> cryptographyService.decrypt(wrongKeyPair.getPrivate(), cipherText))
          .isInstanceOf(GeneralSecurityException.class);
    }
  }

  @Nested
  class SymmetricEncryption {

    @Test
    void encryptsAndDecryptsRoundTrip() throws GeneralSecurityException {
      SecretKey key = CryptographyService.asAesKey("abcdefghijklmnop");
      CipherText cipherText = cryptographyService.encrypt(key, SYMMETRIC_ALGORITHM, payload);
      assertThat(cipherText.iv()).isPresent();
      assertThat(cryptographyService.decrypt(key, cipherText)).isEqualTo(payload);
    }

    @Test
    void usesFreshIvPerEncryption() throws GeneralSecurityException {
      SecretKey key = CryptographyService.asAesKey("abcdefghijklmnop");
      CipherText first = cryptographyService.encrypt(key, SYMMETRIC_ALGORITHM, payload);
      CipherText second = cryptographyService.encrypt(key, SYMMETRIC_ALGORITHM, payload);
      assertThat(first.iv()).isNotEqualTo(second.iv());
      assertThat(first.cipherText()).isNotEqualTo(second.cipherText());
      assertThat(cryptographyService.decrypt(key, first)).isEqualTo(payload);
      assertThat(cryptographyService.decrypt(key, second)).isEqualTo(payload);
    }

    @Test
    void rejectsMissingIvOnDecrypt() {
      SecretKey key = CryptographyService.asAesKey("abcdefghijklmnop");
      CipherText withoutIv = new CipherText(Optional.empty(), SYMMETRIC_ALGORITHM, "AAAA",
          Optional.empty(), Optional.empty());
      assertThatThrownBy(() -> cryptographyService.decrypt(key, withoutIv))
          .isInstanceOf(GeneralSecurityException.class);
    }

    /**
     * Zero-length input is legal for AES/CTR: the cipher text is empty while
     * the IV is still present, so an encrypt result MUST round-trip through
     * {@code decrypt} rather than being rejected as an invalid cipher text.
     */
    @Test
    void roundTripsZeroLengthSymmetricInput() throws GeneralSecurityException {
      SecretKey key = CryptographyService.asAesKey("abcdefghijklmnop");
      CipherText cipherText = cryptographyService.encrypt(key, SYMMETRIC_ALGORITHM, new byte[0]);
      assertThat(cipherText.cipherText()).isEmpty();
      assertThat(cipherText.iv()).isPresent();
      assertThat(cipherText.isValid()).isTrue();
      assertThat(cryptographyService.decrypt(key, cipherText)).isEmpty();
    }

    /**
     * An empty cipher text without an IV cannot come from any supported
     * scheme (IV-less RSA-OAEP always produces a modulus-sized output), so it
     * stays invalid even though zero-length symmetric cipher texts are legal.
     */
    @Test
    void rejectsEmptyCipherTextWithoutIv() {
      CipherText withoutIv = new CipherText(Optional.empty(), SYMMETRIC_ALGORITHM, "",
          Optional.empty(), Optional.empty());
      assertThat(withoutIv.isValid()).isFalse();
      assertThatThrownBy(() -> cryptographyService.decrypt(
          CryptographyService.asAesKey("abcdefghijklmnop"), withoutIv))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid cipher text");
    }
  }

  @Nested
  class AesKeySizing {

    @Test
    void padsToSmallestValidAesLength() {
      assertThat(asAesKeyEncodedLength("a")).isEqualTo(16);      // 1 -> 16
      assertThat(asAesKeyEncodedLength("a".repeat(16))).isEqualTo(16);
      assertThat(asAesKeyEncodedLength("a".repeat(17))).isEqualTo(24);
      assertThat(asAesKeyEncodedLength("a".repeat(24))).isEqualTo(24);
      assertThat(asAesKeyEncodedLength("a".repeat(25))).isEqualTo(32); // legacy bug padded to 36
      assertThat(asAesKeyEncodedLength("a".repeat(32))).isEqualTo(32);
    }

    /**
     * Legacy semantics restored: key material longer than 32 bytes is
     * rejected, never silently truncated — truncation would silently map
     * two passphrases sharing a 32-byte prefix to the same AES key.
     */
    @Test
    void rejectsKeysLongerThan32Bytes() {
      assertThatThrownBy(() -> CryptographyService.asAesKey("a".repeat(33)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("33");
      assertThatThrownBy(() -> CryptographyService.asAesKey("a".repeat(64)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("64");
    }

    @Test
    void producesAesKeys() {
      SecretKey key = CryptographyService.asAesKey("any-passphrase-length");
      assertThat(key.getAlgorithm()).isEqualTo("AES");
    }

    /**
     * Regression for the legacy bug: keys of length in (24, 32) were padded
     * to 36 bytes — an invalid AES key size that exploded at cipher init.
     * A 25-char key must yield a usable 32-byte AES key.
     */
    @Test
    void twentyFiveCharKeyIsUsableAes256Key() throws GeneralSecurityException {
      SecretKey key = CryptographyService.asAesKey("abcdefghijklmnopqrstuvwxy");
      assertThat(key.getEncoded()).hasSize(32);
      CipherText cipherText = cryptographyService.encrypt(key, SYMMETRIC_ALGORITHM, payload);
      assertThat(cryptographyService.decrypt(key, cipherText)).isEqualTo(payload);
    }

    @Test
    void rejectsNullKey() {
      assertThatThrownBy(() -> CryptographyService.asAesKey(null))
          .isInstanceOf(NullPointerException.class);
    }

    private int asAesKeyEncodedLength(String key) {
      return CryptographyService.asAesKey(key).getEncoded().length;
    }
  }

  /**
   * Invariant (AGENTS.md: crypto policy lives in PersonaVersion): the
   * derive-or-verify whitelists of {@link BouncyCastleCryptographyService}
   * must exactly equal the active Persona Version's algorithm tables. If a
   * future version extends the policy without extending the service (or
   * vice versa), these tests fail the build.
   */
  @Nested
  class VersionPolicyConsistency {

    private final PersonaVersion version = PersonaVersion.latestVersion();

    @Test
    void cipherWhitelistEqualsVersionEncryptionAlgorithms() {
      Set<String> expected = new HashSet<>();
      expected.addAll(version.validAsymmetricEncryptionAlgorithms());
      expected.addAll(version.validSymmetricEncryptionAlgorithms());
      assertThat(BouncyCastleCryptographyService.supportedCipherAlgorithms())
          .isEqualTo(expected);
    }

    @Test
    void keyGenerationWhitelistEqualsVersionKeyGenerationAlgorithm() {
      assertThat(BouncyCastleCryptographyService.supportedKeyGenerationAlgorithms())
          .containsExactlyInAnyOrder(version.keyGenerationAlgorithm());
    }

    /** Derive-or-verify: every algorithm in the version policy must actually work. */
    @Test
    void everyVersionAlgorithmActuallyRoundTrips() throws GeneralSecurityException {
      for (String algorithm : version.validAsymmetricEncryptionAlgorithms()) {
        CipherText cipherText = cryptographyService.encrypt(certificate, algorithm, payload);
        assertThat(cryptographyService.decrypt(keyPair.getPrivate(), cipherText))
            .isEqualTo(payload);
      }
      for (String algorithm : version.validSymmetricEncryptionAlgorithms()) {
        SecretKey key = CryptographyService.asAesKey("policy-consistency-key");
        CipherText cipherText = cryptographyService.encrypt(key, algorithm, payload);
        assertThat(cryptographyService.decrypt(key, cipherText)).isEqualTo(payload);
      }
      KeyPair generated = cryptographyService.generateKeyPair(
          version.keyGenerationAlgorithm(), version.keySize());
      assertThat(generated.getPrivate()).isNotNull();
      assertThat(generated.getPublic()).isNotNull();
    }

    /** And an algorithm outside the policy is still rejected. */
    @Test
    void rejectsAlgorithmOutsideVersionPolicy() {
      String outsidePolicy = "RSA/ECB/PKCS1Padding";
      assertThat(version.validAsymmetricEncryptionAlgorithms()).doesNotContain(outsidePolicy);
      assertThat(version.validSymmetricEncryptionAlgorithms()).doesNotContain(outsidePolicy);
      assertThatThrownBy(() -> cryptographyService.encrypt(certificate, outsidePolicy, payload))
          .isInstanceOf(IllegalArgumentException.class);
      String outsideKeyGeneration = "DSA";
      assertThat(version.keyGenerationAlgorithm()).isNotEqualTo(outsideKeyGeneration);
      assertThatThrownBy(() -> cryptographyService.generateKeyPair(outsideKeyGeneration, 1024))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class PolicyWhitelists {

    @Test
    void generatesRsaKeyPairs() throws GeneralSecurityException {
      KeyPair generated = cryptographyService.generateKeyPair("RSA", 2048);
      assertThat(generated.getPrivate()).isNotNull();
      assertThat(generated.getPublic().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void rejectsUnsupportedKeyGenerationAlgorithm() {
      assertThatThrownBy(() -> cryptographyService.generateKeyPair("EC", 256))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("EC");
    }

    @Test
    void rejectsUnsupportedCipherAlgorithm() {
      assertThatThrownBy(() -> cryptographyService.encrypt(certificate, "AES/ECB/PKCS5Padding", payload))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> cryptographyService.encrypt(CryptographyService.asAesKey("abcdefghijklmnop"),
          "DESede/CBC/PKCS5Padding", payload))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidCipherTextRecords() {
      CipherText invalid = new CipherText(Optional.empty(), "", "AAA", Optional.empty(), Optional.empty());
      assertThatThrownBy(() -> cryptographyService.decrypt(keyPair.getPrivate(), invalid))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> cryptographyService.decrypt(keyPair.getPrivate(), null))
          .isInstanceOf(NullPointerException.class);
    }
  }
}
