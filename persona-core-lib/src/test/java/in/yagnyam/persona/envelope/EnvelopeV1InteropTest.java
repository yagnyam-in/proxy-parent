package in.yagnyam.persona.envelope;

import static org.assertj.core.api.Assertions.assertThat;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.crypto.PersonaVersionV0;
import in.yagnyam.persona.crypto.PersonaVersionV1;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * End-to-end envelope signing and verification under Persona Version v1, and
 * interoperability with v0 (protocol §8: v1 keeps RSA in
 * {@code validSignatureAlgorithmSets}; §11 rule 3: the set must match exactly).
 */
class EnvelopeV1InteropTest {

  @Test
  void v1PersonaSignsWithEd25519AndVerifiesUnderV1() {
    EnvelopeWorld world = new EnvelopeWorld(PersonaVersionV1.instance());
    PersonaFixture alice = world.mint();
    for (String format : List.of("json", "persona-binary")) {
      SignedEnvelope envelope = world.signer().sign(greeting(alice), alice.key(), format);
      assertThat(envelope.signatures()).extracting(Signature::algorithm).containsExactly("Ed25519");
      VerificationResult result = world.verifier().verify(envelope);
      assertThat(result.verified()).as("%s: %s", format, result).isTrue();
    }
  }

  /** Backward compatibility: a v0 (SHA256withRSA) envelope still verifies under v1. */
  @Test
  void v0EnvelopeVerifiesUnderV1() {
    EnvelopeWorld world = new EnvelopeWorld(PersonaVersionV0.instance());
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice), alice.key(), "json");
    assertThat(envelope.signatures()).extracting(Signature::algorithm).containsExactly("SHA256withRSA");

    VerificationResult result = world.verifierUnder(PersonaVersionV1.instance()).verify(envelope);
    assertThat(result.verified()).as("%s", result).isTrue();
  }

  /** v0 does not sanction Ed25519, so a v0 peer rejects a v1 envelope on the set check. */
  @Test
  void v1EnvelopeIsRejectedUnderV0() {
    EnvelopeWorld world = new EnvelopeWorld(PersonaVersionV1.instance());
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice), alice.key(), "json");

    VerificationResult result = world.verifierUnder(PersonaVersionV0.instance()).verify(envelope);
    assertThat(result.verified()).isFalse();
  }

  /** A mixed-family signature set is not sanctioned by v1 (protocol §7.2, §12 downgrade defense). */
  @Test
  void mixedFamilySignatureSetIsRejectedUnderV1() {
    EnvelopeWorld world = new EnvelopeWorld(PersonaVersionV1.instance());
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice), alice.key(), "json");
    Signature ed25519 = envelope.signatures().get(0);
    SignedEnvelope mixed = new SignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(), envelope.payload(),
        envelope.signedBy(), List.of(ed25519, new Signature("SHA256withRSA", ed25519.value())));

    assertThat(world.verifier().verify(mixed).verified()).isFalse();
  }

  @Test
  void tamperedV1PayloadIsRejected() {
    EnvelopeWorld world = new EnvelopeWorld(PersonaVersionV1.instance());
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice), alice.key(), "json");
    Signature original = envelope.signatures().get(0);
    byte[] forged = original.value().clone();
    forged[0] ^= 1;
    SignedEnvelope tampered = new SignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(), envelope.payload(),
        envelope.signedBy(), List.of(new Signature(original.algorithm(), forged)));

    assertThat(world.verifier().verify(tampered).verified()).isFalse();
  }

  private static TestMessages.TestGreeting greeting(PersonaFixture from) {
    return new TestMessages.TestGreeting("g-1", from.unpinnedId(), PersonaId.of("p-bob"), "hello");
  }
}
