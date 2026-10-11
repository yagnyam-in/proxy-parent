package in.yagnyam.persona.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.TestCertificates;
import in.yagnyam.persona.TestSupport;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Persona Version v1 primitives of {@link BouncyCastleCryptographyService}
 * (protocol §8): Ed25519 signatures, curve key generation, and the
 * {@code X25519/HKDF-SHA256/AES-256-GCM} asymmetric encryption construction.
 */
class V1CryptographyTest {

  private static final String X25519 = PersonaVersionV1.X25519_ENCRYPTION_ALGORITHM;

  private final BouncyCastleCryptographyService crypto = BouncyCastleCryptographyService.instance();
  private final byte[] payload = "hello persona v1".getBytes(StandardCharsets.UTF_8);

  @Nested
  class Ed25519Signatures {

    private KeyPair ed25519;

    @BeforeEach
    void setUp() {
      ed25519 = TestSupport.signingKeyPair(PersonaVersionV1.instance());
    }

    @Test
    void generatesEd25519KeysWithKeySizeZero() {
      assertThat(ed25519.getPublic().getAlgorithm()).isIn("Ed25519", "EdDSA");
      assertThatThrownBy(() -> crypto.generateKeyPair("Ed25519", 256))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("key size 0");
    }

    @Test
    void signsAndVerifiesAgainstCertificate() throws GeneralSecurityException {
      X509Certificate certificate =
          TestCertificates.selfSignedWithCommonName(ed25519, "p-ed25519", Duration.ofDays(1));
      byte[] signature = crypto.getSignature("Ed25519", ed25519.getPrivate(), payload);
      assertThat(signature).hasSize(64);
      assertThat(crypto.verifySignature("Ed25519", certificate, payload, signature)).isTrue();
    }

    @Test
    void detectsTamperingAndForeignKeys() throws GeneralSecurityException {
      byte[] signature = crypto.getSignature("Ed25519", ed25519.getPrivate(), payload);
      byte[] tampered = payload.clone();
      tampered[0] ^= 1;
      assertThat(crypto.verifySignature("Ed25519", ed25519.getPublic(), tampered, signature)).isFalse();
      KeyPair other = TestSupport.signingKeyPair(PersonaVersionV1.instance());
      assertThat(crypto.verifySignature("Ed25519", other.getPublic(), payload, signature)).isFalse();
    }

    /** Ed25519 is deterministic (RFC 8032): the same key and input give the same signature. */
    @Test
    void signaturesAreDeterministic() throws GeneralSecurityException {
      assertThat(crypto.getSignature("Ed25519", ed25519.getPrivate(), payload))
          .isEqualTo(crypto.getSignature("Ed25519", ed25519.getPrivate(), payload));
    }

    /** Cross-family use fails: an RSA key cannot produce an Ed25519 signature. */
    @Test
    void rejectsKeyOfTheWrongFamily() {
      assertThatThrownBy(() -> crypto.getSignature("Ed25519", TestSupport.rsaKeyPair().getPrivate(), payload))
          .isInstanceOf(GeneralSecurityException.class);
      assertThatThrownBy(() -> crypto.getSignature("SHA256withRSA", ed25519.getPrivate(), payload))
          .isInstanceOf(GeneralSecurityException.class);
    }
  }

  @Nested
  class X25519Encryption {

    private KeyPair recipient;
    private X509Certificate recipientCertificate;

    @BeforeEach
    void setUp() {
      recipient = TestSupport.x25519KeyPair();
      recipientCertificate = TestCertificates.issuedBy(recipient.getPublic(), "p-recipient",
          TestSupport.signingKeyPair(PersonaVersionV1.instance()), Duration.ofDays(1));
    }

    @Test
    void roundTrips() throws GeneralSecurityException {
      CipherText cipherText = crypto.encrypt(recipientCertificate, X25519, payload);
      assertThat(cipherText.encryptionAlgorithm()).isEqualTo(X25519);
      assertThat(Base64.getDecoder().decode(cipherText.iv().orElseThrow())).hasSize(12);
      assertThat(cipherText.ephemeralPublicKey()).isPresent();
      assertThat(cipherText.hmac()).isEmpty();
      // AES-GCM: plaintext length + 16-byte tag.
      assertThat(Base64.getDecoder().decode(cipherText.cipherText())).hasSize(payload.length + 16);
      assertThat(crypto.decrypt(recipient.getPrivate(), cipherText)).isEqualTo(payload);
    }

    @Test
    void roundTripsEmptyInput() throws GeneralSecurityException {
      CipherText cipherText = crypto.encrypt(recipientCertificate, X25519, new byte[0]);
      assertThat(cipherText.isValid()).isTrue();
      assertThat(crypto.decrypt(recipient.getPrivate(), cipherText)).isEmpty();
    }

    @Test
    void usesFreshEphemeralKeyAndIvPerEncryption() throws GeneralSecurityException {
      CipherText first = crypto.encrypt(recipientCertificate, X25519, payload);
      CipherText second = crypto.encrypt(recipientCertificate, X25519, payload);
      assertThat(first.ephemeralPublicKey()).isNotEqualTo(second.ephemeralPublicKey());
      assertThat(first.iv()).isNotEqualTo(second.iv());
      assertThat(first.cipherText()).isNotEqualTo(second.cipherText());
    }

    /**
     * Known answer, computed independently with pyca/cryptography: RFC 7748
     * §6.1 keys (Alice's as the ephemeral, Bob's as the recipient), IV
     * {@code 000102..0b}. Z, E and R equal the RFC 7748 values, so this pins
     * the HKDF info layout and the AES-256-GCM framing of protocol §8.
     */
    @Test
    void decryptsKnownAnswer() throws GeneralSecurityException {
      PrivateKey bob = KeyFactory.getInstance("X25519", BouncyCastle.provider()).generatePrivate(
          new PKCS8EncodedKeySpec(Base64.getDecoder().decode(
              "MC4CAQAwBQYDK2VuBCIEIF2rCH5iSopLeeF/i4OADuZvO7EpJhi2/Rwviyf/iODr")));
      CipherText known = new CipherText(
          Optional.of("AAECAwQFBgcICQoL"), X25519,
          "RwSugmaODe7GdOSWOdWU/GKosIuXvHKJtV8yPIRqJ5FqudVHc60Z",
          Optional.empty(), Optional.empty(),
          Optional.of("MCowBQYDK2VuAyEAhSDwCYkwp1R0i33ctD73Wg2/Og0mOBr066SpjqqbTmo="));
      assertThat(new String(crypto.decrypt(bob, known), StandardCharsets.UTF_8))
          .isEqualTo("persona v1 known answer");
    }

    @Test
    void wrongRecipientCannotDecrypt() throws GeneralSecurityException {
      CipherText cipherText = crypto.encrypt(recipientCertificate, X25519, payload);
      assertThatThrownBy(() -> crypto.decrypt(TestSupport.x25519KeyPair().getPrivate(), cipherText))
          .isInstanceOf(GeneralSecurityException.class)
          .hasMessageContaining("tag mismatch");
    }

    @Test
    void detectsTamperedCipherTextIvAndEphemeralKey() throws GeneralSecurityException {
      CipherText good = crypto.encrypt(recipientCertificate, X25519, payload);
      CipherText otherEphemeral = crypto.encrypt(recipientCertificate, X25519, payload);
      byte[] sealed = Base64.getDecoder().decode(good.cipherText());
      sealed[0] ^= 1;
      byte[] iv = Base64.getDecoder().decode(good.iv().orElseThrow());
      iv[0] ^= 1;
      CipherText[] tampered = {
          withParts(good, good.iv(), Base64.getEncoder().encodeToString(sealed), good.ephemeralPublicKey()),
          withParts(good, Optional.of(Base64.getEncoder().encodeToString(iv)), good.cipherText(),
              good.ephemeralPublicKey()),
          withParts(good, good.iv(), good.cipherText(), otherEphemeral.ephemeralPublicKey()),
      };
      for (CipherText candidate : tampered) {
        assertThatThrownBy(() -> crypto.decrypt(recipient.getPrivate(), candidate))
            .isInstanceOf(GeneralSecurityException.class);
      }
    }

    @Test
    void rejectsMissingOrMalformedCarrierFields() throws GeneralSecurityException {
      CipherText good = crypto.encrypt(recipientCertificate, X25519, payload);
      assertThatThrownBy(() -> crypto.decrypt(recipient.getPrivate(),
          withParts(good, good.iv(), good.cipherText(), Optional.empty())))
          .isInstanceOf(GeneralSecurityException.class)
          .hasMessageContaining("ephemeral public key");
      assertThatThrownBy(() -> crypto.decrypt(recipient.getPrivate(),
          withParts(good, Optional.empty(), good.cipherText(), good.ephemeralPublicKey())))
          .isInstanceOf(GeneralSecurityException.class);
      assertThatThrownBy(() -> crypto.decrypt(recipient.getPrivate(),
          withParts(good, Optional.of("AAAA"), good.cipherText(), good.ephemeralPublicKey())))
          .isInstanceOf(GeneralSecurityException.class)
          .hasMessageContaining("12 bytes");
      assertThatThrownBy(() -> crypto.decrypt(recipient.getPrivate(),
          withParts(good, good.iv(), good.cipherText(), Optional.of("not base64 !!"))))
          .isInstanceOf(GeneralSecurityException.class);
    }

    /** An ephemeral key of another family (here Ed25519) is refused, not reinterpreted. */
    @Test
    void rejectsNonX25519EphemeralKey() throws GeneralSecurityException {
      CipherText good = crypto.encrypt(recipientCertificate, X25519, payload);
      String ed25519Spki = Base64.getEncoder().encodeToString(
          TestSupport.signingKeyPair(PersonaVersionV1.instance()).getPublic().getEncoded());
      assertThatThrownBy(() -> crypto.decrypt(recipient.getPrivate(),
          withParts(good, good.iv(), good.cipherText(), Optional.of(ed25519Spki))))
          .isInstanceOf(GeneralSecurityException.class)
          .hasMessageContaining("X25519");
    }

    /** A non-octet-aligned key BIT STRING is malformed input, reported through the checked contract. */
    @Test
    void rejectsEphemeralKeyWithUnusedBits() throws Exception {
      CipherText good = crypto.encrypt(recipientCertificate, X25519, payload);
      byte[] hostile = new org.bouncycastle.asn1.x509.SubjectPublicKeyInfo(
          new org.bouncycastle.asn1.x509.AlgorithmIdentifier(
              org.bouncycastle.asn1.edec.EdECObjectIdentifiers.id_X25519),
          new org.bouncycastle.asn1.DERBitString(new byte[32], 1)).getEncoded();
      assertThatThrownBy(() -> crypto.decrypt(recipient.getPrivate(),
          withParts(good, good.iv(), good.cipherText(),
              Optional.of(Base64.getEncoder().encodeToString(hostile)))))
          .isInstanceOf(GeneralSecurityException.class)
          .hasMessageContaining("Malformed ephemeral public key");
    }

    /** A small-order ephemeral point (here the all-zero u-coordinate) yields Z = 0 and is refused. */
    @Test
    void rejectsSmallOrderEphemeralKey() throws GeneralSecurityException {
      CipherText good = crypto.encrypt(recipientCertificate, X25519, payload);
      byte[] spki = Base64.getDecoder().decode(good.ephemeralPublicKey().orElseThrow());
      for (int i = spki.length - 32; i < spki.length; i++) {
        spki[i] = 0;
      }
      assertThatThrownBy(() -> crypto.decrypt(recipient.getPrivate(),
          withParts(good, good.iv(), good.cipherText(), Optional.of(Base64.getEncoder().encodeToString(spki)))))
          .isInstanceOf(GeneralSecurityException.class)
          .hasMessageContaining("all-zero");
    }

    @Test
    void rejectsKeysOfTheWrongFamily() throws GeneralSecurityException {
      KeyPair rsa = TestSupport.rsaKeyPair();
      X509Certificate rsaCertificate =
          TestCertificates.selfSignedWithCommonName(rsa, "p-rsa", Duration.ofDays(1));
      assertThatThrownBy(() -> crypto.encrypt(rsaCertificate, X25519, payload))
          .isInstanceOf(GeneralSecurityException.class)
          .hasMessageContaining("X25519");
      CipherText cipherText = crypto.encrypt(recipientCertificate, X25519, payload);
      assertThatThrownBy(() -> crypto.decrypt(rsa.getPrivate(), cipherText))
          .isInstanceOf(GeneralSecurityException.class);
      // And RSA-OAEP refuses a cipher text that claims an ephemeral key.
      CipherText rsaCipherText = crypto.encrypt(rsaCertificate, "RSA/NONE/OAEPwithSHA-256andMGF1Padding", payload);
      assertThatThrownBy(() -> crypto.decrypt(rsa.getPrivate(),
          withParts(rsaCipherText, rsaCipherText.iv(), rsaCipherText.cipherText(), good().ephemeralPublicKey())))
          .isInstanceOf(GeneralSecurityException.class);
    }

    private CipherText good() throws GeneralSecurityException {
      return crypto.encrypt(recipientCertificate, X25519, payload);
    }

    private CipherText withParts(CipherText base, Optional<String> iv, String sealed,
        Optional<String> ephemeralPublicKey) {
      return new CipherText(iv, base.encryptionAlgorithm(), sealed, base.hmacAlgorithm(), base.hmac(),
          ephemeralPublicKey);
    }
  }
}
