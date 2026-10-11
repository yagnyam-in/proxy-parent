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

  /** @return key family for new personas' signing keys (e.g. {@code RSA},
   *      {@code Ed25519}) */
  String keyGenerationAlgorithm();

  /**
   * @return key families of persona keys that receive asymmetric encryption
   *     under this version (protocol §8). v0's RSA key is dual-use — the same
   *     key signs and wraps — so its list is the signing family itself; v1's
   *     Ed25519 signing key cannot perform key agreement, so its X25519
   *     encryption family is declared separately (a v1 persona that both signs
   *     and receives encrypted content holds two key pairs; how the second,
   *     encryption key is certified is the per-algorithm key-descriptor
   *     extension deferred by protocol §7.2).
   */
  List<String> encryptionKeyGenerationAlgorithms();

  /** @return key size in bits (0 for curve-typed families) */
  int keySize();

  /** @return canonical hash algorithm for thumbprints and salted hashes */
  String preferredHashAlgorithm();

  /**
   * Returns the latest Persona Version: {@code v1} (Ed25519 signatures,
   * X25519 key agreement). New personas therefore generate Ed25519 keys.
   * {@code v0} stays resolvable through {@link #of(String) of("v0")}, and a
   * v1 verifier still accepts v0 ({@code SHA256withRSA}) envelopes.
   *
   * @return latest version instance
   */
  static PersonaVersion latestVersion() {
    return PersonaVersionV1.instance();
  }

  /**
   * Resolves a Persona Version by its opaque identifier (protocol §3.5: a
   * Realm may bind to a specific Persona Version).
   *
   * @param version version identifier ({@code v0} or {@code v1})
   * @return the version's policy
   * @throws IllegalArgumentException when {@code version} names no known
   *     Persona Version
   */
  static PersonaVersion of(String version) {
    Objects.requireNonNull(version, "version");
    return switch (version) {
      case PersonaVersionV0.VERSION -> PersonaVersionV0.instance();
      case PersonaVersionV1.VERSION -> PersonaVersionV1.instance();
      default -> throw new IllegalArgumentException(
          "Unknown persona version '" + version + "' (known: v0, v1)");
    };
  }

  /**
   * @return every known Persona Version, oldest first
   */
  static List<PersonaVersion> knownVersions() {
    return List.of(PersonaVersionV0.instance(), PersonaVersionV1.instance());
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
