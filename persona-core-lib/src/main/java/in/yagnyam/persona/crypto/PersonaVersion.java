package in.yagnyam.persona.crypto;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Versioned cryptographic policy of the Persona Protocol (protocol §8).
 *
 * <p>Concrete algorithms are versioned as a group so peers can negotiate
 * compatibility. Nothing outside a {@code PersonaVersion} implementation may
 * hard-code algorithm names (repository convention); callers always take the
 * active version and pass its values into {@link CryptographyService}.
 *
 * <p>Signature algorithm sets MUST be single-key-family: every algorithm in a
 * set must be computable with the signer's one certified private key
 * (protocol §7.2 key-family constraint).
 */
public interface PersonaVersion {

  /** @return opaque version identifier (e.g. {@code v0}) */
  String version();

  /** @return accepted signature algorithm sets; an envelope's signature set
   *      must match one of them exactly (protocol §11 rule 3) */
  List<Set<String>> validSignatureAlgorithmSets();

  /** @return the set new signatures should use */
  Set<String> preferredSignatureAlgorithmSet();

  /** @return accepted asymmetric (certificate-key) encryption algorithms */
  List<String> validAsymmetricEncryptionAlgorithms();

  /** @return the default asymmetric encryption algorithm */
  String preferredAsymmetricEncryptionAlgorithm();

  /** @return accepted symmetric encryption algorithms */
  List<String> validSymmetricEncryptionAlgorithms();

  /** @return the default symmetric encryption algorithm */
  String preferredSymmetricEncryptionAlgorithm();

  /** @return algorithm the Registrar uses to sign certificates (§8) */
  String certificateSignatureAlgorithm();

  /** @return key family for new personas (e.g. {@code RSA}) */
  String keyGenerationAlgorithm();

  /** @return key size in bits (0 for curve-typed families) */
  int keySize();

  /** @return canonical hash algorithm for thumbprints and salted hashes */
  String preferredHashAlgorithm();

  /**
   * Returns the latest Persona Version.
   *
   * @return latest version instance
   */
  static PersonaVersion latestVersion() {
    return PersonaVersionV0.instance();
  }

  /**
   * Checks whether {@code signatureAlgorithms} matches one of
   * {@link #validSignatureAlgorithmSets()} exactly (protocol §11 rule 3 and
   * §12 downgrade defense: supersets, subsets and the empty set are all
   * rejected).
   *
   * @param signatureAlgorithms candidate signature set
   * @return {@code true} only on an exact match against a sanctioned set
   */
  default boolean supportsSignatureAlgorithmSet(Set<String> signatureAlgorithms) {
    Objects.requireNonNull(signatureAlgorithms, "signatureAlgorithms");
    Set<String> candidate = Set.copyOf(signatureAlgorithms);
    return validSignatureAlgorithmSets().stream().anyMatch(valid -> valid.equals(candidate));
  }
}
