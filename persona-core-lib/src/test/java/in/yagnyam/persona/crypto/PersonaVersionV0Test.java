package in.yagnyam.persona.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PersonaVersionV0} (protocol §8): the v0 policy values are
 * pinned exactly to the §8 table, collections are immutable, and the
 * signature-set matcher enforces exact matches (§11 rule 3, §12).
 */
class PersonaVersionV0Test {

  private final PersonaVersionV0 version = PersonaVersionV0.instance();

  @Test
  void pinsV0PolicyValuesToProtocolTable() {
    assertThat(version.version()).isEqualTo("v0");
    assertThat(version.validSignatureAlgorithmSets())
        .isEqualTo(List.of(Set.of("SHA256withRSA")));
    assertThat(version.preferredSignatureAlgorithmSet()).isEqualTo(Set.of("SHA256withRSA"));
    assertThat(version.validAsymmetricEncryptionAlgorithms())
        .isEqualTo(List.of("RSA/NONE/OAEPwithSHA-256andMGF1Padding"));
    assertThat(version.preferredAsymmetricEncryptionAlgorithm())
        .isEqualTo("RSA/NONE/OAEPwithSHA-256andMGF1Padding");
    assertThat(version.validSymmetricEncryptionAlgorithms())
        .isEqualTo(List.of("AES/CTR/NoPadding"));
    assertThat(version.preferredSymmetricEncryptionAlgorithm()).isEqualTo("AES/CTR/NoPadding");
    assertThat(version.certificateSignatureAlgorithm()).isEqualTo("SHA256withRSA");
    assertThat(version.keyGenerationAlgorithm()).isEqualTo("RSA");
    assertThat(version.encryptionKeyGenerationAlgorithms()).isEqualTo(List.of("RSA"));
    assertThat(version.keySize()).isEqualTo(2048);
    assertThat(version.preferredHashAlgorithm()).isEqualTo("SHA-256");
  }

  @Test
  void latestVersionIsV0() {
    assertThat(PersonaVersion.latestVersion()).isSameAs(PersonaVersionV0.instance());
    assertThat(PersonaVersion.latestVersion().version()).isEqualTo("v0");
  }

  @Test
  void policyCollectionsAreImmutable() {
    assertThatThrownBy(() -> version.validSignatureAlgorithmSets().add(Set.of("SHA512withRSA")))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> version.preferredSignatureAlgorithmSet().add("SHA512withRSA"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> version.validAsymmetricEncryptionAlgorithms().add("RSA/ECB/NoPadding"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> version.validSymmetricEncryptionAlgorithms().add("AES/ECB/PKCS5Padding"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void acceptsExactSignatureAlgorithmSet() {
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("SHA256withRSA"))).isTrue();
  }

  @Test
  void rejectsSupersetAndSubset() {
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("SHA256withRSA", "SHA512withRSA")))
        .isFalse();
    assertThat(version.supportsSignatureAlgorithmSet(Set.of())).isFalse();
  }

  @Test
  void rejectsOtherAlgorithmSets() {
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("SHA512withRSA"))).isFalse();
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("SHA1withRSA"))).isFalse();
    assertThat(version.supportsSignatureAlgorithmSet(Set.of("Ed25519"))).isFalse();
  }

  @Test
  void rejectsNullCandidateSet() {
    assertThatThrownBy(() -> version.supportsSignatureAlgorithmSet(null))
        .isInstanceOf(NullPointerException.class);
  }
}
