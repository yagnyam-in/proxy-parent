package in.yagnyam.persona;

import in.yagnyam.persona.crypto.CipherText;
import in.yagnyam.persona.crypto.Digests;
import in.yagnyam.persona.crypto.PemService;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

/**
 * Local secret material of a persona owner (protocol §3.3; {@code SPEC.md}
 * §3.1.5, was {@code ProxyKey}).
 *
 * <p>Carries the private key either as plaintext PEM (for secure-enclave
 * storage) or as {@link CipherText} protected by a user passphrase — exactly
 * one of the two. The private key never leaves the owner's device
 * unencrypted, and never appears in {@link #toString()}.
 *
 * <p>The decoded {@link PublicKey}/{@code PrivateKey} handles are mutable
 * transient caches (records cannot carry instance state, so this is a small
 * final class): set them via {@link #setPublicKey}/{@link #setPrivateKey}
 * after encryption/decryption of the PEM material.
 */
public final class PersonaKey implements Validatable {

  private final PersonaId id;
  private final String privateKeyPem;
  private final CipherText privateKeyEncrypted;
  private final String privateKeySha256Thumbprint;
  private final String publicKeyPem;
  private final String publicKeySha256Thumbprint;

  private PublicKey publicKey;
  private PrivateKey privateKey;

  /**
   * Creates a persona key carrying exactly one form of private key material.
   *
   * @param id                        persona id
   * @param privateKeyPem             plaintext PEM private key, or
   *                                  {@code null} if encrypted
   * @param privateKeyEncrypted       encrypted private key, or {@code null}
   *                                  if plaintext
   * @param privateKeySha256Thumbprint optional private-key thumbprint
   * @param publicKeyPem              PEM public key
   * @param publicKeySha256Thumbprint public-key thumbprint
   * @throws IllegalArgumentException unless exactly one of
   *     {@code privateKeyPem}/{@code privateKeyEncrypted} is present
   */
  public PersonaKey(
      PersonaId id,
      String privateKeyPem,
      CipherText privateKeyEncrypted,
      String privateKeySha256Thumbprint,
      String publicKeyPem,
      String publicKeySha256Thumbprint) {
    this.id = Objects.requireNonNull(id, "id");
    if ((privateKeyPem == null) == (privateKeyEncrypted == null)) {
      throw new IllegalArgumentException(
          "Exactly one of privateKeyPem / privateKeyEncrypted must be present");
    }
    this.privateKeyPem = privateKeyPem;
    this.privateKeyEncrypted = privateKeyEncrypted;
    this.privateKeySha256Thumbprint = privateKeySha256Thumbprint;
    this.publicKeyPem = Objects.requireNonNull(publicKeyPem, "publicKeyPem");
    this.publicKeySha256Thumbprint = Objects.requireNonNull(publicKeySha256Thumbprint, "publicKeySha256Thumbprint");
  }

  /**
   * Creates a persona key from a fresh key pair: private/public PEMs,
   * SHA-256 thumbprints and decoded key handles.
   *
   * @param id         persona id
   * @param keyPair    persona key pair
   * @param pemService PEM encoder
   * @return persona key with plaintext private key material
   */
  public static PersonaKey of(PersonaId id, KeyPair keyPair, PemService pemService) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(keyPair, "keyPair");
    Objects.requireNonNull(pemService, "pemService");
    try {
      PersonaKey key = new PersonaKey(
          id,
          pemService.encodePrivateKey(keyPair.getPrivate()),
          null,
          Digests.sha256Hex(keyPair.getPrivate().getEncoded()),
          pemService.encodePublicKey(keyPair.getPublic()),
          Digests.sha256Hex(keyPair.getPublic().getEncoded()));
      key.setPrivateKey(keyPair.getPrivate());
      key.setPublicKey(keyPair.getPublic());
      return key;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to encode key material", e);
    }
  }

  /** @return persona id */
  public PersonaId id() {
    return id;
  }

  /** @return plaintext PEM private key, or {@code null} if encrypted */
  public String privateKeyPem() {
    return privateKeyPem;
  }

  /** @return encrypted private key, or {@code null} if plaintext */
  public CipherText privateKeyEncrypted() {
    return privateKeyEncrypted;
  }

  /** @return private-key thumbprint, or {@code null} if absent */
  public String privateKeySha256Thumbprint() {
    return privateKeySha256Thumbprint;
  }

  /** @return PEM public key */
  public String publicKeyPem() {
    return publicKeyPem;
  }

  /** @return public-key thumbprint */
  public String publicKeySha256Thumbprint() {
    return publicKeySha256Thumbprint;
  }

  /** @return decoded public key handle, or {@code null} until set */
  public PublicKey publicKey() {
    return publicKey;
  }

  /** @return decoded private key handle, or {@code null} until set */
  public PrivateKey privateKey() {
    return privateKey;
  }

  /**
   * Sets the decoded public key handle (transient cache).
   *
   * @param publicKey decoded public key
   */
  public void setPublicKey(PublicKey publicKey) {
    this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
  }

  /**
   * Sets the decoded private key handle (transient cache).
   *
   * @param privateKey decoded private key
   */
  public void setPrivateKey(PrivateKey privateKey) {
    this.privateKey = Objects.requireNonNull(privateKey, "privateKey");
  }

  /**
   * @return canonical {@link PersonaId#uniqueId()} form
   */
  public String getUniqueId() {
    return id.uniqueId();
  }

  /**
   * @return {@code true} when the id is valid, the public key material
   *     (PEM and thumbprint) is present, and exactly one usable form of
   *     private key material is present (plaintext PEM, or valid
   *     {@link CipherText}) — consistent with {@link PersonaCertificate}
   *     and {@code Persona} validity
   */
  @Override
  public boolean isValid() {
    if (id == null || !id.isValid()) {
      return false;
    }
    if (!isPresent(publicKeyPem) || !isPresent(publicKeySha256Thumbprint)) {
      return false;
    }
    if (privateKeyPem != null) {
      return !privateKeyPem.isEmpty();
    }
    return privateKeyEncrypted != null && privateKeyEncrypted.isValid();
  }

  /**
   * Never exposes private key material (protocol §12: sensitive fields stay
   * out of string representations).
   */
  @Override
  public String toString() {
    return "PersonaKey[id=" + id.uniqueId()
        + ", privateKeyPem=" + (privateKeyPem == null ? "absent" : "present")
        + ", privateKeyEncrypted=" + (privateKeyEncrypted == null ? "absent" : "present")
        + ", publicKeySha256Thumbprint=" + publicKeySha256Thumbprint + "]";
  }

  private static boolean isPresent(String value) {
    return value != null && !value.isEmpty();
  }
}
