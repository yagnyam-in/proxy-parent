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
import java.util.Optional;
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
      assertThat(asAesKeyEncodedLength("a".repeat(33))).isEqualTo(32); // truncated
      assertThat(asAesKeyEncodedLength("a".repeat(64))).isEqualTo(32); // truncated
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
