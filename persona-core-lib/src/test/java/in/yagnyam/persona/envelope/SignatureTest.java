package in.yagnyam.persona.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Signature} (protocol §6.1): structural validity,
 * defensive copies, content equality and log redaction.
 */
class SignatureTest {

  @Test
  void acceptsAValidSignature() {
    Signature signature = new Signature("SHA256withRSA", new byte[] {1, 2, 3});
    assertThat(signature.isValid()).isTrue();
    assertThat(signature.value()).containsExactly(1, 2, 3);
  }

  @Test
  void rejectsEmptyAlgorithmOrValueAsInvalid() {
    assertThat(new Signature("", new byte[] {1}).isValid()).isFalse();
    assertThat(new Signature("SHA256withRSA", new byte[0]).isValid()).isFalse();
  }

  @Test
  void rejectsNullsAtConstruction() {
    assertThatThrownBy(() -> new Signature(null, new byte[] {1}))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Signature("SHA256withRSA", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void defensiveCopiesPreventMutation() {
    byte[] original = new byte[] {1, 2, 3};
    Signature signature = new Signature("SHA256withRSA", original);
    original[0] = 99;
    assertThat(signature.value()).containsExactly(1, 2, 3);

    byte[] taken = signature.value();
    taken[0] = 99;
    assertThat(signature.value()).containsExactly(1, 2, 3);
  }

  @Test
  void equalsAndHashCodeAreContentBased() {
    Signature first = new Signature("SHA256withRSA", new byte[] {1, 2, 3});
    Signature second = new Signature("SHA256withRSA", new byte[] {1, 2, 3});
    Signature differentAlgorithm = new Signature("SHA512withRSA", new byte[] {1, 2, 3});
    Signature differentValue = new Signature("SHA256withRSA", new byte[] {3, 2, 1});

    assertThat(first).isEqualTo(second);
    assertThat(first).hasSameHashCodeAs(second);
    assertThat(first).isNotEqualTo(differentAlgorithm);
    assertThat(first).isNotEqualTo(differentValue);
  }

  @Test
  void toStringNeverEchoesTheSignatureBytes() {
    Signature signature = new Signature("SHA256withRSA", new byte[256]);
    assertThat(signature.toString())
        .contains("SHA256withRSA")
        .contains("256 bytes")
        .doesNotContain("AAECAw");
  }

  @Test
  void valueRoundTripsThroughTheWireModel() {
    Signature signature = new Signature("SHA256withRSA", new byte[] {1, 2, 3});
    assertThat(Signature.fromValue(signature.toValue())).isEqualTo(signature);

    assertThatThrownBy(() -> Signature.fromValue(java.util.Map.of("algorithm", "a", "value", "b")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("value")
        .hasMessageContaining("bytes");
  }
}
