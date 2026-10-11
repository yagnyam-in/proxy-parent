package in.yagnyam.persona.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.PersonaKey;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CipherText;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.resolver.InMemoryPersonaResolver;
import in.yagnyam.persona.schema.InMemorySchemaRegistry;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EnvelopeSigner} (protocol §7.2, §7.4): the §5.1
 * authorization gate, serialize-once payload handling, the algorithm set
 * taken from the active version, and the multi-signature flows.
 */
class EnvelopeSignerTest {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  /** A bystander persona id that is never registered anywhere. */
  private static final PersonaId BYSTANDER = PersonaId.of("p-bob");

  @Test
  void signsAGreetingEndToEnd() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), BYSTANDER, "hello");

    SignedEnvelope envelope = world.signer().sign(greeting, alice.key(), "json");

    assertThat(envelope.messageType()).isEqualTo("example.Greeting@v1");
    assertThat(envelope.payloadFormat()).isEqualTo("json");
    assertThat(envelope.schemaId()).isEqualTo(world.schemaIdOf("example.Greeting@v1"));
    assertThat(envelope.signedBy()).isEqualTo(alice.pinnedId());
    assertThat(envelope.signatures())
        .extracting(Signature::algorithm)
        .containsExactlyElementsOf(
            PersonaVersion.latestVersion().preferredSignatureAlgorithmSet());
  }

  @Test
  void theSignatureCoversTheSectionSixFourPreimageOfTheEmittedFields() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), BYSTANDER, "hello");

    SignedEnvelope envelope = world.signer().sign(greeting, alice.key(), "json");

    // Independent check: recompute the §6.4 preimage from the emitted
    // envelope and verify the signature directly through the crypto
    // service, bypassing the verifier entirely.
    byte[] preimage = SigningPreimage.forSignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
        envelope.payload());
    for (Signature signature : envelope.signatures()) {
      assertThat(signatureVerifies(alice, signature, preimage))
          .as("signature over the §6.4 preimage of the emitted envelope")
          .isTrue();
    }
    Set<String> algorithms = Set.copyOf(
        envelope.signatures().stream().map(Signature::algorithm).toList());
    assertThat(PersonaVersion.latestVersion().supportsSignatureAlgorithmSet(algorithms)).isTrue();
  }

  private static boolean signatureVerifies(
      PersonaFixture signer, Signature signature, byte[] preimage) {
    try {
      return CRYPTO.verifySignature(
          signature.algorithm(), signer.registered().certificate().certificate().getPublicKey(),
          preimage, signature.value());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void signsInTheBinaryPayloadFormat() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), BYSTANDER, "hello");

    SignedEnvelope envelope = world.signer().sign(greeting, alice.key(), "persona-binary");

    assertThat(envelope.payloadFormat()).isEqualTo("persona-binary");
    var decoded = new BinaryPayloadCodec().decode(
        world.schemaRegistry().requireByMessageType("example.Greeting@v1"), envelope.payload());
    assertThat(decoded.get("body")).isEqualTo("hello");
    assertThat(world.verifier().verify(envelope).verified()).isTrue();
  }

  @Test
  void refusesToSignAnInvalidPayload() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    TestMessages.TestGreeting invalid =
        new TestMessages.TestGreeting("", alice.unpinnedId(), BYSTANDER, "hello");

    assertThatThrownBy(() -> world.signer().sign(invalid, alice.key(), "json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not valid")
        .hasMessageContaining("§7.2 step 1");
  }

  @Test
  void refusesToSignForAnUnauthorizedSigner() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture mallory = world.mint();
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), BYSTANDER, "hello");

    assertThatThrownBy(() -> world.signer().sign(greeting, mallory.key(), "json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not authorized to sign")
        .hasMessageContaining("§7.2 step 1");
  }

  @Test
  void refusesUnknownPayloadFormats() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), BYSTANDER, "hello");

    assertThatThrownBy(() -> world.signer().sign(greeting, alice.key(), "xml"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsupported payloadFormat 'xml'");
  }

  @Test
  void refusesMessageTypesWithoutSchemaOrBinding() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), BYSTANDER, "hello");

    // No schema registered for the message type
    EnvelopeSigner noSchema = new EnvelopeSigner(
        CRYPTO, PersonaVersion.latestVersion(), new InMemorySchemaRegistry(), world.bindings());
    assertThatThrownBy(() -> noSchema.sign(greeting, alice.key(), "json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no schema registered with messageType");

    // No binding registered for the message type
    EnvelopeSigner noBinding = new EnvelopeSigner(
        CRYPTO, PersonaVersion.latestVersion(), world.schemaRegistry(), MessageBindings.of());
    assertThatThrownBy(() -> noBinding.sign(greeting, alice.key(), "json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no message binding registered for messageType");
  }

  @Test
  void refusesAKeyWithoutAPrivateKeyHandle() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaKey encryptedOnly = new PersonaKey(
        alice.key().id(),
        null,
        new CipherText(Optional.of("iv"), "AES/CTR/NoPadding", "cipher-text",
            Optional.empty(), Optional.empty(), Optional.empty()),
        alice.key().privateKeySha256Thumbprint(),
        alice.key().publicKeyPem(),
        alice.key().publicKeySha256Thumbprint());
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), BYSTANDER, "hello");

    assertThatThrownBy(() -> world.signer().sign(greeting, encryptedOnly, "json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no decoded private key handle");
  }

  @Test
  void multiSigningInitiallyProducesASingleEntry() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    TestMessages.TestCharter charter = new TestMessages.TestCharter(
        "c-1", Set.of(alice.unpinnedId(), bob.unpinnedId()), 1, "terms");

    MultiSignedEnvelope envelope = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");

    assertThat(envelope.messageType()).isEqualTo("example.Charter@v1");
    assertThat(envelope.payloadFormat()).isEqualTo("json");
    assertThat(envelope.schemaId()).isEqualTo(world.schemaIdOf("example.Charter@v1"));
    assertThat(envelope.signatures()).hasSize(1);
    assertThat(envelope.signatures().get(0).signedBy()).isEqualTo(alice.pinnedId());
    assertThat(world.verifier().verify(envelope).verified()).isTrue();
  }

  @Test
  void incrementalSigningPreservesEarlierEntries() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    TestMessages.TestCharter charter = new TestMessages.TestCharter(
        "c-1", Set.of(alice.unpinnedId(), bob.unpinnedId()), 2, "terms");

    MultiSignedEnvelope initial = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");
    MultiSignedEnvelope extended = world.signer().addSignature(initial, bob.key());

    // The earlier entry is untouched (protocol §7.4)
    assertThat(extended.signatures()).hasSize(2);
    assertThat(extended.signatures().get(0)).isEqualTo(initial.signatures().get(0));
    assertThat(extended.payloadFormat()).isEqualTo(initial.payloadFormat());
    assertThat(extended.payload()).isEqualTo(initial.payload());
    assertThat(extended.signatures().get(1).signedBy()).isEqualTo(bob.pinnedId());
    assertThat(world.verifier().verify(extended).verified()).isTrue();
  }

  @Test
  void incrementalSigningRefusesAnUnauthorizedCoSigner() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture mallory = world.mint();
    TestMessages.TestCharter charter =
        new TestMessages.TestCharter("c-1", Set.of(alice.unpinnedId()), 1, "terms");

    MultiSignedEnvelope initial = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");

    assertThatThrownBy(() -> world.signer().addSignature(initial, mallory.key()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not authorized to co-sign");
  }

  @Test
  void coSignsAnEnvelopeWhoseSchemaIsIdentifiedByDigestOnly() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();

    // A charter-shaped schema with NO messageType declaration: it is
    // registered by digest only, so the envelope can name it solely via
    // schemaId (docs/SCHEMA.md §6 rule 4). A message-type-only lookup —
    // what addSignature used to do — cannot find it.
    InMemorySchemaRegistry registry = new InMemorySchemaRegistry();
    PersonaSchema schema = registry.register("""
        {"type":"record","name":"DigestCharter","namespace":"example",
         "fields":[{"name":"charterId","type":"string"},
                   {"name":"founders","type":{"type":"array","items":"string"}},
                   {"name":"minimumSignatures","type":"int"},
                   {"name":"terms","type":"string"}]}""");
    String schemaId = registry.idOf(schema).value();
    String messageType = "example.DigestCharter@v1";
    MessageBindings bindings = MessageBindings.of(digestCharterBinding(messageType));
    assertThat(registry.byMessageType(messageType)).isEmpty();

    // The initial envelope as a peer would emit it: payload encoded under
    // the digest-only schema, signed by alice (hand-built because
    // signMulti resolves the schema by message type at emission).
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("charterId", "c-1");
    value.put("founders",
        List.of(alice.unpinnedId().uniqueId(), bob.unpinnedId().uniqueId()));
    value.put("minimumSignatures", 2);
    value.put("terms", "terms");
    byte[] payload = new JsonPayloadCodec().encode(schema, value);
    byte[] preimage =
        SigningPreimage.forMultiSignedEnvelope(messageType, "json", schemaId, payload);
    MultiSignedEnvelope initial = new MultiSignedEnvelope(messageType, "json", schemaId, payload,
        List.of(new MultiSignature(alice.pinnedId(), signAll(preimage, alice))));

    EnvelopeSigner signer =
        new EnvelopeSigner(CRYPTO, PersonaVersion.latestVersion(), registry, bindings);
    MultiSignedEnvelope extended = signer.addSignature(initial, bob.key());

    // The co-signed envelope carries both entries and verifies end-to-end.
    assertThat(extended.signatures()).hasSize(2);
    assertThat(extended.signatures().get(0)).isEqualTo(initial.signatures().get(0));
    assertThat(extended.signatures().get(1).signedBy()).isEqualTo(bob.pinnedId());
    EnvelopeVerifier verifier = new EnvelopeVerifier(
        PersonaVersion.latestVersion(), CRYPTO, registry, bindings,
        new InMemoryPersonaResolver(world.registrar()), world.trust(), Clock.systemUTC());
    assertThat(verifier.verify(extended).verified()).isTrue();
  }

  @Test
  void coSigningRejectsAConflictingMessageTypeAndSchemaId() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    TestMessages.TestCharter charter = new TestMessages.TestCharter(
        "c-1", Set.of(alice.unpinnedId(), bob.unpinnedId()), 1, "terms");
    MultiSignedEnvelope initial = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");

    // The envelope names the charter message type but the digest of the
    // (different) greeting schema: digest and message-type entry disagree,
    // and neither may take silent precedence (docs/SCHEMA.md §6 rule 4).
    MultiSignedEnvelope conflicting = new MultiSignedEnvelope(
        initial.messageType(), initial.payloadFormat(),
        world.schemaIdOf("example.Greeting@v1"), initial.payload(), initial.signatures());

    assertThatThrownBy(() -> world.signer().addSignature(conflicting, bob.key()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match the schema registered for messageType")
        .hasMessageContaining("§6 rule 4");
  }

  /**
   * A binding of {@code messageType} to {@link TestMessages.TestCharter},
   * for a schema that is registered by digest only (no messageType of its
   * own).
   */
  private static MessageBinding digestCharterBinding(String messageType) {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return messageType;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        Set<PersonaId> founders = new LinkedHashSet<>();
        for (Object founder : (List<?>) payloadValue.get("founders")) {
          founders.add(PersonaId.of((String) founder));
        }
        return new TestMessages.TestCharter(
            (String) payloadValue.get("charterId"),
            founders,
            (Integer) payloadValue.get("minimumSignatures"),
            (String) payloadValue.get("terms"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        throw new UnsupportedOperationException("co-sign tests only ever bind");
      }
    };
  }

  /** Signs {@code preimage} as {@code signer} with every preferred algorithm. */
  private static List<Signature> signAll(byte[] preimage, PersonaFixture signer) {
    List<Signature> signatures = new ArrayList<>();
    for (String algorithm : PersonaVersion.latestVersion().preferredSignatureAlgorithmSet()) {
      try {
        signatures.add(new Signature(algorithm,
            CRYPTO.getSignature(algorithm, signer.key().privateKey(), preimage)));
      } catch (GeneralSecurityException e) {
        throw new IllegalStateException(e);
      }
    }
    return signatures;
  }

  @Test
  void incrementalSigningRefusesUndecodableOrIllTypedPayloads() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), BYSTANDER, "hello");

    // A single-signed payload's envelope cannot be co-signed: the payload
    // does not implement MultiSignableMessage
    SignedEnvelope signed = world.signer().sign(greeting, alice.key(), "json");
    MultiSignedEnvelope disguised = new MultiSignedEnvelope(
        signed.messageType(), signed.payloadFormat(), signed.schemaId(), signed.payload(),
        List.of(new MultiSignature(signed.signedBy(), signed.signatures())));
    assertThatThrownBy(() -> world.signer().addSignature(disguised, alice.key()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not implement MultiSignableMessage");

    // Garbage payload bytes cannot be decoded at all
    MultiSignedEnvelope garbage = new MultiSignedEnvelope(
        "example.Charter@v1", "json", world.schemaIdOf("example.Charter@v1"),
        new byte[] {1, 2, 3},
        List.of(new MultiSignature(
            alice.pinnedId(), List.of(new Signature("SHA256withRSA", new byte[] {1})))));
    assertThatThrownBy(() -> world.signer().addSignature(garbage, alice.key()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be decoded");
  }
}
