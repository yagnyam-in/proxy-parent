package in.yagnyam.persona;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CipherText;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.PemService;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PersonaKey} (protocol §3.3; {@code SPEC.md} §3.1.5):
 * derivation from a key pair, encrypted/plain private key exclusivity,
 * validity, and no private material in {@code toString()}.
 */
class PersonaKeyTest {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  private PersonaId id;
  private KeyPair keyPair;
  private PemService pemService;
  private PersonaKey key;

  @BeforeEach
  void setUp() {
    id = PersonaId.of("p-key-test");
    keyPair = TestSupport.rsaKeyPair();
    pemService = PemService.instance();
    key = PersonaKey.of(id, keyPair, pemService);
  }

  @Test
  void derivesPemsAndThumbprintsFromKeyPair() {
    assertThat(key.privateKeyPem()).startsWith("-----BEGIN PRIVATE KEY-----");
    assertThat(key.publicKeyPem()).startsWith("-----BEGIN PUBLIC KEY-----");
    assertThat(key.privateKeySha256Thumbprint())
        .isEqualTo(TestSupport.sha256Hex(keyPair.getPrivate().getEncoded()));
    assertThat(key.publicKeySha256Thumbprint())
        .isEqualTo(TestSupport.sha256Hex(keyPair.getPublic().getEncoded()));
  }

  @Test
  void decodesPrivatePemBackToOriginalKey() throws Exception {
    PrivateKey decoded = pemService.decodePrivateKey(key.privateKeyPem());
    assertThat(decoded.getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
  }

  @Test
  void carriesDecodedKeyHandles() {
    assertThat(key.publicKey()).isEqualTo(keyPair.getPublic());
    assertThat(key.privateKey()).isEqualTo(keyPair.getPrivate());
  }

  @Test
  void settersUpdateHandlesAndRejectNull() {
    PublicKey otherPublic = TestSupport.rsaKeyPair().getPublic();
    key.setPublicKey(otherPublic);
    assertThat(key.publicKey()).isEqualTo(otherPublic);
    assertThatThrownBy(() -> key.setPublicKey(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> key.setPrivateKey(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void isValidWithPlainPrivateKey() {
    assertThat(key.isValid()).isTrue();
    assertThat(key.getUniqueId()).isEqualTo("p-key-test");
  }

  @Test
  void isValidWithEncryptedPrivateKey() throws Exception {
    CipherText encrypted = CRYPTO.encrypt(CryptographyService.asAesKey("a-secret-passphrase"),
        "AES/CTR/NoPadding", key.privateKeyPem().getBytes(StandardCharsets.UTF_8));
    PersonaKey encryptedKey = new PersonaKey(id, null, encrypted, key.privateKeySha256Thumbprint(),
        key.publicKeyPem(), key.publicKeySha256Thumbprint());
    assertThat(encryptedKey.privateKeyPem()).isNull();
    assertThat(encryptedKey.privateKeyEncrypted()).isEqualTo(encrypted);
    assertThat(encryptedKey.isValid()).isTrue();
  }

  @Test
  void rejectsBothOrNeitherPrivateMaterial() {
    CipherText encrypted = new CipherText(Optional.of("iv"), "AES/CTR/NoPadding", "abc",
        Optional.empty(), Optional.empty(), Optional.empty());
    assertThatThrownBy(() -> new PersonaKey(id, key.privateKeyPem(), encrypted, null,
        key.publicKeyPem(), key.publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PersonaKey(id, null, null, null, key.publicKeyPem(),
        key.publicKeySha256Thumbprint()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void invalidWithEmptyPrivateKeyPem() {
    PersonaKey empty = new PersonaKey(id, "", null, null, key.publicKeyPem(),
        key.publicKeySha256Thumbprint());
    assertThat(empty.isValid()).isFalse();
  }

  /** Public key material is mandatory, consistent with PersonaCertificate/Persona. */
  @Test
  void invalidWithMissingPublicKeyMaterial() {
    PersonaKey withoutPublicKeyPem = new PersonaKey(id, key.privateKeyPem(), null, null,
        "", key.publicKeySha256Thumbprint());
    PersonaKey withoutPublicThumbprint = new PersonaKey(id, key.privateKeyPem(), null, null,
        key.publicKeyPem(), "");
    assertThat(withoutPublicKeyPem.isValid()).isFalse();
    assertThat(withoutPublicThumbprint.isValid()).isFalse();
  }

  @Test
  void neverExposesPrivateMaterialInToString() {
    String rendered = key.toString();
    assertThat(rendered).doesNotContain("PRIVATE KEY");
    assertThat(rendered).doesNotContain(key.privateKeyPem());
    assertThat(rendered).contains("p-key-test");
    assertThat(rendered).contains("absent");
  }

  @Test
  void rejectsInvalidArguments() {
    KeyPair fresh = TestSupport.rsaKeyPair();
    assertThatThrownBy(() -> PersonaKey.of(null, fresh, pemService))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> PersonaKey.of(id, null, pemService))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> PersonaKey.of(id, fresh, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new PersonaKey(null, "private", null, null, "public", "thumb"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new PersonaKey(id, "private", null, null, null, "thumb"))
        .isInstanceOf(NullPointerException.class);
  }
}
