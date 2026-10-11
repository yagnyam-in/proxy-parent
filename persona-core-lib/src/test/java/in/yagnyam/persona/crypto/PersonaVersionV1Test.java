package in.yagnyam.persona.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PersonaVersionV1} (protocol §8): the v1 table, RSA
 * retained for backward compatibility, single-key-family signature sets
 * (§7.2), and version resolution.
 */
class PersonaVersionV1Test {

  private final PersonaVersionV1 version = PersonaVersionV1.instance();

  @Test
  void pinsV1PolicyValues() {
    assertThat(version.version()).isEqualTo("v1");
    assertThat(version.validSignatureAlgorithmSets())
        .isEqualTo(List.of(Set.of("Ed25519"), Set.of("SHA256withRSA")));
    assertThat(version.preferredSignatureAlgorithmSet()).isEqualTo(Set.of("Ed25519"));
    assertThat(version.validAsymmetricEncryptionAlgorithms()).isEqualTo(List.of(
        "X25519/HKDF-SHA256/AES-256-GCM", "RSA/NONE/OAEPwithSHA-256andMGF1Padding"));
    assertThat(version.preferredAsymmetricEncryptionAlgorithm())
        .isEqualTo("X25519/HKDF-SHA256/AES-256-GCM");
    assertThat(version.validSymmetricEncryptionAlgorithms()).isEqualTo(List.of("AES/CTR/NoPadding"));
    assertThat(version.preferredSymmetricEncryptionAlgorithm()).isEqualTo("AES/CTR/NoPadding");
    assertThat(version.certificateSignatureAlgorithm()).isEqualTo("Ed25519");
    assertThat(version.keyGenerationAlgorithm()).isEqualTo("Ed25519");
    assertThat(version.encryptionKeyGenerationAlgorithms()).isEqualTo(List.of("X25519", "RSA"));
    assertThat(version.keySize()).isZero();
    assertThat(version.preferredHashAlgorithm()).isEqualTo("SHA-256");
  }

  /** Every v0 signature set stays valid in v1, so v0 envelopes keep verifying. */
  @Test
  void retainsEveryV0SignatureSet() {
    assertThat(version.validSignatureAlgorithmSets())
        .containsAll(PersonaVersionV0.instance().validSignatureAlgorithmSets());
    assertThat(version.validAsymmetricEncryptionAlgorithms())
        .containsAll(PersonaVersionV0.instance().validAsymmetricEncryptionAlgorithms());
  }

  @Test
  void acceptsEachSingleFamilySetAndRejectsMixedSets() {
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("Ed25519"))).isTrue();
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("SHA256withRSA"))).isTrue();
    // A mixed-family set cannot be produced by one certified key (§7.2).
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("Ed25519", "SHA256withRSA"))).isFalse();
    assertThat(version.supportsSignatureAlgorithmSet(Set.of())).isFalse();
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("Ed448"))).isFalse();
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("SHA1withRSA"))).isFalse();
  }

  @Test
  void policyCollectionsAreImmutable() {
    assertThatThrownBy(() -> version.validSignatureAlgorithmSets().add(Set.of("Ed448")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> version.preferredSignatureAlgorithmSet().add("Ed448"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> version.validAsymmetricEncryptionAlgorithms().add("X448"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> version.encryptionKeyGenerationAlgorithms().add("X448"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void resolvesVersionsByIdentifier() {
    assertThat(PersonaVersion.of("v0")).isSameAs(PersonaVersionV0.instance());
    assertThat(PersonaVersion.of("v1")).isSameAs(PersonaVersionV1.instance());
    assertThatThrownBy(() -> PersonaVersion.of("v2"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("v2");
    assertThatThrownBy(() -> PersonaVersion.of(null)).isInstanceOf(NullPointerException.class);
    assertThat(PersonaVersion.knownVersions())
        .containsExactly(PersonaVersionV0.instance(), PersonaVersionV1.instance());
  }

  /** v1 is the default: new personas sign Ed25519; v0 stays resolvable via {@code of("v0")}. */
  @Test
  void latestVersionIsV1() {
    assertThat(PersonaVersion.latestVersion()).isSameAs(PersonaVersionV1.instance());
  }
}
