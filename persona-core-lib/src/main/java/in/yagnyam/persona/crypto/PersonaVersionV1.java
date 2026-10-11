package in.yagnyam.persona.crypto;

import java.util.List;
import java.util.Set;

/**
 * Persona Version {@code v1} — the Ed25519/X25519 successor of the v0 baseline
 * (protocol §8): Ed25519 signatures and X25519 asymmetric encryption, with RSA
 * kept in {@code validSignatureAlgorithmSets} for backward compatibility.
 *
 * <p>Algorithm table:
 * <ul>
 *   <li>Signature: {@code Ed25519} — the preferred set; the v0 set
 *       {@code {SHA256withRSA}} remains valid, so envelopes signed by v0
 *       personas still verify under v1 (§11 rule 3, §12 downgrade defense)</li>
 *   <li>Asymmetric encryption: {@link #X25519_ENCRYPTION_ALGORITHM} — an
 *       ephemeral-static X25519 key agreement, HKDF-SHA256 key derivation and
 *       AES-256-GCM AEAD, constructed exactly as protocol §8 pins it —
 *       preferred; {@code RSA/NONE/OAEPwithSHA-256andMGF1Padding} remains
 *       valid</li>
 *   <li>Symmetric encryption: {@code AES/CTR/NoPadding} — unchanged from v0</li>
 *   <li>Certificate signature: {@code Ed25519}</li>
 *   <li>Key generation: {@code Ed25519} (curve-typed: key size 0)</li>
 *   <li>Encryption key families: {@code X25519} (key agreement; an Ed25519 key
 *       cannot agree) and {@code RSA}</li>
 *   <li>Hash: {@code SHA-256}</li>
 * </ul>
 *
 * <p>Signature algorithm sets stay single-key-family (protocol §7.2):
 * {@code {Ed25519}} and {@code {SHA256withRSA}} are separate valid sets — a
 * set mixing both families is invalid, and migration between families happens
 * by registering a new persona instance (§3.1, §7.2). An Ed25519 key cannot
 * perform X25519 key agreement (and an X25519 key cannot sign), so a v1
 * persona that both signs and receives encrypted content holds two key
 * pairs; how the second, encryption key is certified is the per-algorithm
 * key-descriptor extension deferred by §7.2.
 *
 * <p>{@link PersonaVersion#latestVersion()} still returns v0 until a
 * deployment opts in — activating v1 as the default flips the preferred
 * signature set and with it the key family of every new persona.
 *
 * <p>All returned collections are immutable.
 */
public final class PersonaVersionV1 implements PersonaVersion {

  /** Version identifier. */
  public static final String VERSION = "v1";

  /**
   * The X25519 asymmetric-encryption algorithm identifier of v1: an
   * ephemeral-static X25519 key agreement, HKDF-SHA256 key derivation and
   * AES-256-GCM AEAD, constructed exactly as protocol §8 specifies.
   */
  public static final String X25519_ENCRYPTION_ALGORITHM = "X25519/HKDF-SHA256/AES-256-GCM";

  /**
   * ASCII HKDF-SHA256 {@code info} prefix of {@link #X25519_ENCRYPTION_ALGORITHM}
   * (protocol §8); the raw ephemeral and recipient public keys follow it,
   * binding the derived key to both ends of the agreement.
   */
  public static final String X25519_HKDF_INFO_LABEL = "persona/v1 X25519/HKDF-SHA256/AES-256-GCM";

  private static final PersonaVersionV1 INSTANCE = new PersonaVersionV1();

  private static final List<Set<String>> VALID_SIGNATURE_ALGORITHM_SETS =
      List.of(Set.of("Ed25519"), Set.of("SHA256withRSA"));
  private static final Set<String> PREFERRED_SIGNATURE_ALGORITHM_SET =
      Set.of("Ed25519");
  private static final List<String> VALID_ASYMMETRIC_ENCRYPTION_ALGORITHMS = List.of(
      X25519_ENCRYPTION_ALGORITHM,
      "RSA/NONE/OAEPwithSHA-256andMGF1Padding");
  private static final String PREFERRED_ASYMMETRIC_ENCRYPTION_ALGORITHM =
      X25519_ENCRYPTION_ALGORITHM;
  private static final List<String> VALID_SYMMETRIC_ENCRYPTION_ALGORITHMS =
      List.of("AES/CTR/NoPadding");
  private static final String PREFERRED_SYMMETRIC_ENCRYPTION_ALGORITHM =
      "AES/CTR/NoPadding";
  private static final String CERTIFICATE_SIGNATURE_ALGORITHM = "Ed25519";
  private static final String KEY_GENERATION_ALGORITHM = "Ed25519";
  private static final List<String> ENCRYPTION_KEY_GENERATION_ALGORITHMS =
      List.of("X25519", "RSA");
  private static final int KEY_SIZE = 0;
  private static final String PREFERRED_HASH_ALGORITHM = "SHA-256";

  private PersonaVersionV1() {}

  /**
   * Returns the singleton v1 instance.
   *
   * @return v1 policy
   */
  public static PersonaVersionV1 instance() {
    return INSTANCE;
  }

  @Override
  public String version() {
    return VERSION;
  }

  @Override
  public List<Set<String>> validSignatureAlgorithmSets() {
    return VALID_SIGNATURE_ALGORITHM_SETS;
  }

  @Override
  public Set<String> preferredSignatureAlgorithmSet() {
    return PREFERRED_SIGNATURE_ALGORITHM_SET;
  }

  @Override
  public List<String> validAsymmetricEncryptionAlgorithms() {
    return VALID_ASYMMETRIC_ENCRYPTION_ALGORITHMS;
  }

  @Override
  public String preferredAsymmetricEncryptionAlgorithm() {
    return PREFERRED_ASYMMETRIC_ENCRYPTION_ALGORITHM;
  }

  @Override
  public List<String> validSymmetricEncryptionAlgorithms() {
    return VALID_SYMMETRIC_ENCRYPTION_ALGORITHMS;
  }

  @Override
  public String preferredSymmetricEncryptionAlgorithm() {
    return PREFERRED_SYMMETRIC_ENCRYPTION_ALGORITHM;
  }

  @Override
  public String certificateSignatureAlgorithm() {
    return CERTIFICATE_SIGNATURE_ALGORITHM;
  }

  @Override
  public String keyGenerationAlgorithm() {
    return KEY_GENERATION_ALGORITHM;
  }

  @Override
  public List<String> encryptionKeyGenerationAlgorithms() {
    return ENCRYPTION_KEY_GENERATION_ALGORITHMS;
  }

  @Override
  public int keySize() {
    return KEY_SIZE;
  }

  @Override
  public String preferredHashAlgorithm() {
    return PREFERRED_HASH_ALGORITHM;
  }
}
