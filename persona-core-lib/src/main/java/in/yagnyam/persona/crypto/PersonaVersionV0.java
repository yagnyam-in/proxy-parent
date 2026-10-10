package in.yagnyam.persona.crypto;

import java.util.List;
import java.util.Set;

/**
 * Persona Version {@code v0} — the recommended baseline (protocol §8).
 *
 * <p>Algorithm table:
 * <ul>
 *   <li>Signature: {@code SHA256withRSA}</li>
 *   <li>Asymmetric encryption: {@code RSA/NONE/OAEPwithSHA-256andMGF1Padding}</li>
 *   <li>Symmetric encryption: {@code AES/CTR/NoPadding}</li>
 *   <li>Key generation: RSA / 2048</li>
 *   <li>Hash: {@code SHA-256}</li>
 * </ul>
 * All returned collections are immutable.
 */
public final class PersonaVersionV0 implements PersonaVersion {

  /** Version identifier. */
  public static final String VERSION = "v0";

  private static final PersonaVersionV0 INSTANCE = new PersonaVersionV0();

  private static final List<Set<String>> VALID_SIGNATURE_ALGORITHM_SETS =
      List.of(Set.of("SHA256withRSA"));
  private static final Set<String> PREFERRED_SIGNATURE_ALGORITHM_SET =
      Set.of("SHA256withRSA");
  private static final List<String> VALID_ASYMMETRIC_ENCRYPTION_ALGORITHMS =
      List.of("RSA/NONE/OAEPwithSHA-256andMGF1Padding");
  private static final String PREFERRED_ASYMMETRIC_ENCRYPTION_ALGORITHM =
      "RSA/NONE/OAEPwithSHA-256andMGF1Padding";
  private static final List<String> VALID_SYMMETRIC_ENCRYPTION_ALGORITHMS =
      List.of("AES/CTR/NoPadding");
  private static final String PREFERRED_SYMMETRIC_ENCRYPTION_ALGORITHM =
      "AES/CTR/NoPadding";
  private static final String CERTIFICATE_SIGNATURE_ALGORITHM = "SHA256withRSA";
  private static final String KEY_GENERATION_ALGORITHM = "RSA";
  private static final int KEY_SIZE = 2048;
  private static final String PREFERRED_HASH_ALGORITHM = "SHA-256";

  private PersonaVersionV0() {}

  /**
   * Returns the singleton v0 instance.
   *
   * @return v0 policy
   */
  public static PersonaVersionV0 instance() {
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
  public int keySize() {
    return KEY_SIZE;
  }

  @Override
  public String preferredHashAlgorithm() {
    return PREFERRED_HASH_ALGORITHM;
  }
}
