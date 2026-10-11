package in.yagnyam.persona.envelope;

import static org.assertj.core.api.Assertions.assertThat;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.resolver.InMemoryPersonaResolver;
import in.yagnyam.persona.resolver.PersonaResolver;
import in.yagnyam.persona.resolver.TrustValidator;
import in.yagnyam.persona.schema.InMemorySchemaRegistry;
import in.yagnyam.persona.schema.SchemaParser;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for {@link EnvelopeVerifier} (protocol §7.3, §7.4, §7.5):
 * real personas minted through the in-process registrar sign payloads that
 * are verified through the full step sequence — and every tampering,
 * mislabeling, trust and policy violation is rejected with an informative
 * reason.
 */
class EnvelopeVerifierTest {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  /** The single signature algorithm of the active version, taken from the policy source. */
  private static final String POLICY_ALGORITHM =
      PersonaVersion.latestVersion().preferredSignatureAlgorithmSet().iterator().next();

  // -----------------------------------------------------------------------------------
  // Happy paths
  // -----------------------------------------------------------------------------------

  @Test
  void verifiesAGenuinelySignedEnvelope() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(
        greeting(alice, "hello"), alice.key(), "json");

    VerificationResult result = world.verifier().verify(envelope);
    assertThat(result.verified()).isTrue();
    assertThat(result.reason()).isEmpty();
  }

  @Test
  void verifiesAnEnvelopeSignedInTheBinaryPayloadFormat() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(
        greeting(alice, "hello"), alice.key(), "persona-binary");

    assertThat(world.verifier().verify(envelope).verified()).isTrue();
  }

  // -----------------------------------------------------------------------------------
  // §7.3 step 1: well-formedness + signature-set policy (§11 rule 3, §12)
  // -----------------------------------------------------------------------------------

  @Test
  void rejectsAnEnvelopeThatIsNotWellFormed() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");
    SignedEnvelope unsigned = new SignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
        envelope.payload(), envelope.signedBy(), List.of());

    VerificationResult result = world.verifier().verify(unsigned);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason ->
        assertThat(reason).contains("not well-formed"));
  }

  @Test
  void rejectsNullEnvelopes() {
    EnvelopeWorld world = new EnvelopeWorld();

    assertThat(world.verifier().verify((SignedEnvelope) null).reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("envelope is null"));
    assertThat(world.verifier().verify((MultiSignedEnvelope) null).reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("envelope is null"));
  }

  @Test
  void rejectsASignatureSetOutsideTheVersionPolicy() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // A superset of a valid set is not a valid set (protocol §12)
    List<Signature> tooMany = new ArrayList<>(envelope.signatures());
    // The extra algorithm name is a test literal on purpose: the point is a
    // set outside the version policy, whatever its members are.
    tooMany.add(new Signature("SHA512withRSA", new byte[] {1}));
    SignedEnvelope extended = relabel(envelope, null, null, null, tooMany);

    VerificationResult result = world.verifier().verify(extended);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("does not exactly match any valid signature algorithm set");
      assertThat(reason).contains("supersets, subsets");
    });
  }

  @Test
  void rejectsAMixedFamilySignatureSet() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // RSA + Ed25519 in one set violates the §7.2 key-family constraint: a
    // set like that is not among the version's valid sets, so §7.3 step 1
    // rejects it before any cryptography runs.
    List<Signature> mixedFamily = new ArrayList<>(envelope.signatures());
    mixedFamily.add(new Signature("SHA256withRSA", new byte[] {1}));
    SignedEnvelope mixed = relabel(envelope, null, null, null, mixedFamily);

    VerificationResult result = world.verifier().verify(mixed);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason)
            .contains("does not exactly match any valid signature algorithm set"));
  }

  @Test
  void rejectsDuplicateAlgorithmEntries() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // §6.1: "one entry per algorithm" — two entries for the same algorithm
    // are rejected even though their names collapse into a valid set.
    Signature original = envelope.signatures().get(0);
    List<Signature> duplicated = List.of(original, new Signature(original.algorithm(),
        new byte[] {1, 2, 3}));
    SignedEnvelope duplicatedEnvelope = relabel(envelope, null, null, null, duplicated);

    VerificationResult result = world.verifier().verify(duplicatedEnvelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason ->
        assertThat(reason).contains("duplicate entries for the same algorithm"));
  }

  // -----------------------------------------------------------------------------------
  // §7.3 step 2: schema resolution
  // -----------------------------------------------------------------------------------

  @Test
  void rejectsAnUnknownMessageTypeWithoutASchemaId() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    SignedEnvelope relabeled = relabel(envelope, "example.Unknown@v1", null, "", null);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("no schema registered for messageType 'example.Unknown@v1'");
      assertThat(reason).contains("§7.3 step 2");
    });
  }

  @Test
  void rejectsAMessageTypeTheRecipientHasNoBindingFor() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // With the schemaId still present the schema resolves; the unbound
    // message type is rejected at materialization (§7.3 steps 3 and 5).
    SignedEnvelope relabeled = relabel(envelope, "example.Unknown@v1", null, null, null);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("no message binding registered for messageType");
      assertThat(reason).contains("example.Unknown@v1");
    });
  }

  @Test
  void rejectsASchemaIdTheRegistryDoesNotKnow() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    SignedEnvelope relabeled = relabel(envelope, null, null, "ab".repeat(20), null);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason ->
        assertThat(reason).contains("no schema registered with schemaId"));
  }

  @Test
  void rejectsASchemaIdThatContradictsTheMessageTypeSchema() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // The greeting-shaped schema without a messageType: same fields, a
    // different digest. Swapping the schemaId to it must be rejected by the
    // digest/message-type cross-check (docs/SCHEMA.md §6 rule 4).
    String isoSchemaId = world.schemaRegistry()
        .idOf(SchemaParser.parse(EnvelopeWorld.GREETING_ISO_SCHEMA)).value();
    SignedEnvelope relabeled = relabel(envelope, null, null, isoSchemaId, null);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("does not match the schema registered for messageType");
      assertThat(reason).contains("example.Greeting@v1");
    });
  }

  @Test
  void rejectsWhenTheVerifiersOwnRegistryLacksTheSchema() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // A recipient that never registered the schema cannot decode the payload
    VerificationResult result =
        world.verifierWithSchemaRegistry(new InMemorySchemaRegistry())
            .verify(envelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason ->
        assertThat(reason).contains("no schema registered with schemaId"));
  }

  // -----------------------------------------------------------------------------------
  // §7.3 step 3: payload decode + validity (§11 rule 4)
  // -----------------------------------------------------------------------------------

  @Test
  void rejectsAnUnknownPayloadFormat() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    SignedEnvelope relabeled = relabel(envelope, null, "xml", null, null);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("unsupported payloadFormat 'xml'");
      assertThat(reason).contains("rejected, not ignored");
    });
  }

  @Test
  void rejectsAPayloadFormatRelabelEvenToAKnownFormat() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // Relabeling json -> persona-binary cannot decode the canonical JSON
    // bytes, so the envelope dies at step 3 before any policy is consulted;
    // the §6.4 preimage additionally binds the format (see SigningPreimageTest)
    SignedEnvelope relabeled = relabel(envelope, null, "persona-binary", null, null);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("cannot be decoded as 'persona-binary'");
      assertThat(reason).contains("§7.3 step 3");
    });
  }

  @Test
  void rejectsAPayloadThatFailsItsOwnDomainRules() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();

    // A hostile envelope the signer would never emit: structurally fine,
    // cryptographically signed by alice, but the payload itself is invalid
    // (empty body). Hand-sign it through the crypto service directly.
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("greetingId", "g-1");
    value.put("from", alice.unpinnedId().uniqueId());
    value.put("to", "p-bob");
    value.put("body", "");
    byte[] payload = new JsonPayloadCodec().encode(
        world.schemaRegistry().requireByMessageType("example.Greeting@v1"), value);
    SignedEnvelope hostile = handSigned(world, alice, payload);

    VerificationResult result = world.verifier().verify(hostile);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("is not valid");
      assertThat(reason).contains("§11 rule 4");
    });
  }

  @Test
  void rejectsASingleSignedEnvelopeCarryingAMultiSignablePayload() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();

    // A pure §5.5 payload cannot travel single-signed: §7.4 says initial
    // multi signing produces a MultiSignedEnvelope, so a charter dressed up
    // as a SignedEnvelope is rejected at materialization (§7.3 step 3)
    TestMessages.TestCharter charter = new TestMessages.TestCharter(
        "c-1", Set.of(alice.unpinnedId(), bob.unpinnedId()), 1, "terms");
    MultiSignedEnvelope signed = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");
    SignedEnvelope disguised = new SignedEnvelope(
        signed.messageType(), signed.payloadFormat(), signed.schemaId(), signed.payload(),
        signed.signatures().get(0).signedBy(), signed.signatures().get(0).signatures());

    VerificationResult result = world.verifier().verify(disguised);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("does not implement SignableMessage");
      assertThat(reason).contains("example.Charter@v1");
    });
  }

  // -----------------------------------------------------------------------------------
  // §7.3 step 4: recursive nested verification (§6.3, §7.5, §11 rule 9)
  // -----------------------------------------------------------------------------------

  @Test
  void verifiesNestedEnvelopesAcrossPersonas() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bank = world.mint();

    // The bank's envelope is nested inside alice's payment authorization
    // (§6.3): the recipient gains proof of both signatures in one pass (§7.5)
    SignedEnvelope account = world.signer().sign(greeting(bank, "account"), bank.key(), "json");
    TestMessages.TestPaymentAuthorization authorization = new TestMessages.TestPaymentAuthorization(
        "pa-1", alice.unpinnedId(), account, "100", List.of(account));
    SignedEnvelope envelope =
        world.signer().sign(authorization, alice.key(), "json");

    VerificationResult result = world.verifier().verify(envelope);
    assertThat(result.verified()).isTrue();
  }

  @Test
  void verifiesNestedEnvelopesInsideABinaryPayload() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bank = world.mint();

    SignedEnvelope account = world.signer().sign(greeting(bank, "account"), bank.key(), "json");
    TestMessages.TestPaymentAuthorization authorization = new TestMessages.TestPaymentAuthorization(
        "pa-1", alice.unpinnedId(), account, "100", List.of());
    SignedEnvelope envelope =
        world.signer().sign(authorization, alice.key(), "persona-binary");

    assertThat(world.verifier().verify(envelope).verified()).isTrue();
  }

  @Test
  void failsFastOnATamperedNestedEnvelopeField() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bank = world.mint();

    SignedEnvelope account = world.signer().sign(greeting(bank, "account"), bank.key(), "json");
    SignedEnvelope tamperedAccount =
        withTamperedPayload(world, account, "tampered account");
    // The OUTER envelope is perfectly signed; only the nested envelope inside
    // its payload carries tampered bytes
    TestMessages.TestPaymentAuthorization authorization = new TestMessages.TestPaymentAuthorization(
        "pa-1", alice.unpinnedId(), tamperedAccount, "100", List.of());
    SignedEnvelope envelope = world.signer().sign(authorization, alice.key(), "json");

    VerificationResult result = world.verifier().verify(envelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("nested envelope at 'payerAccount'");
      assertThat(reason).contains("does not verify");
    });
  }

  @Test
  void failsFastOnATamperedNestedEnvelopeInsideACollection() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bank = world.mint();

    SignedEnvelope account = world.signer().sign(greeting(bank, "account"), bank.key(), "json");
    SignedEnvelope tamperedAccount =
        withTamperedPayload(world, account, "tampered approval");
    TestMessages.TestPaymentAuthorization authorization = new TestMessages.TestPaymentAuthorization(
        "pa-1", alice.unpinnedId(), account, "100", List.of(tamperedAccount));
    SignedEnvelope envelope = world.signer().sign(authorization, alice.key(), "json");

    VerificationResult result = world.verifier().verify(envelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason ->
        assertThat(reason).contains("nested envelope at 'approvals[0]'"));
  }

  // -----------------------------------------------------------------------------------
  // §7.3 step 5: signer authorization (§5.1, §11 rule 2)
  // -----------------------------------------------------------------------------------

  @Test
  void rejectsAWronglyLabeledSigner() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // The envelope claims bob signed a payload only alice may sign
    SignedEnvelope relabeled = relabel(envelope, null, null, null, null, bob.pinnedId());

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("not authorized to sign");
      assertThat(reason).contains("§7.3 step 5");
    });
  }

  // -----------------------------------------------------------------------------------
  // §7.3 step 6: resolution + certificate checks at verification time
  // -----------------------------------------------------------------------------------

  @Test
  void rejectsASignerWhosePinDoesNotResolve() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // alice's id pinned to a thumbprint that was never issued: the §5.1
    // predicate still authorizes her (the payload's valid signer is the
    // unpinned id), so the failure must come from resolution (step 6)
    PersonaId wrongPin = PersonaId.of(alice.unpinnedId().id(), "ee".repeat(32));
    SignedEnvelope relabeled = relabel(envelope, null, null, null, null, wrongPin);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("could not be resolved");
      assertThat(reason).contains("pin mismatch");
    });
  }

  @Test
  void rejectsAPinnedSignerWidenedByAPinStrippingResolver() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // The envelope claims alice pinned to a certificate thumbprint that was
    // never issued. A naive resolver that strips the pin resolves alice's
    // REAL certificate anyway, and the signature genuinely verifies against
    // it — only the verifier's own pin-consistency assertion (the trust
    // boundary, not resolver etiquette) can reject this.
    PersonaId forgedPin = PersonaId.of(alice.unpinnedId().id(), "00".repeat(32));
    SignedEnvelope relabeled = relabel(envelope, null, null, null, null, forgedPin);
    PersonaResolver pinStripping = id ->
        new InMemoryPersonaResolver(world.registrar()).resolvePersona(PersonaId.of(id.id()));

    VerificationResult result =
        world.verifier(pinStripping, world.trust(), Clock.systemUTC()).verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("thumbprint pin");
      assertThat(reason).contains(alice.registered().certificate().sha256Thumbprint());
      assertThat(reason).contains("§7.3 step 6");
    });

    // The bundled resolver is unchanged: it refuses the pin at resolution.
    VerificationResult bundled = world.verifier().verify(relabeled);
    assertThat(bundled.verified()).isFalse();
    assertThat(bundled.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("could not be resolved"));
  }

  @Test
  void rejectsAResolverThatAnswersWithADifferentPersona() throws Exception {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture mallory = world.mint();

    // The attack: Mallory signs a greeting whose 'from' is alice — so the
    // §5.1 authorization of signedBy=alice genuinely passes — and declares
    // signedBy=alice (unpinned). A peer-directory resolver answers the
    // lookup with MALLORY's legitimately issued persona+certificate:
    // Mallory's persona matches her own certificate (TrustValidator is
    // satisfied) and Mallory's signature genuinely verifies against it.
    // Only the verifier's resolved-identity binding (signedBy must BE the
    // resolved persona and its certificate owner) rejects this.
    TestMessages.TestGreeting greeting =
        new TestMessages.TestGreeting("g-1", alice.unpinnedId(), PersonaId.of("p-bob"), "hello");
    String messageType = TestMessages.TestGreeting.MESSAGE_TYPE;
    String schemaId = world.schemaIdOf(messageType);
    byte[] payload = new JsonPayloadCodec().encode(
        world.schemaRegistry().requireByMessageType(messageType),
        world.bindings().require(messageType).wireValue(greeting));
    byte[] preimage = SigningPreimage.forSignedEnvelope(messageType, "json", schemaId, payload);
    byte[] signature = CRYPTO.getSignature(
        POLICY_ALGORITHM, mallory.key().privateKey(), preimage);
    SignedEnvelope forged = new SignedEnvelope(messageType, "json", schemaId, payload,
        alice.unpinnedId(), List.of(new Signature(POLICY_ALGORITHM, signature)));

    PersonaResolver peerDirectory = id -> Optional.of(mallory.registered().persona());
    VerificationResult result =
        world.verifier(peerDirectory, world.trust(), Clock.systemUTC()).verify(forged);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("different identity");
      assertThat(reason).contains(alice.unpinnedId().uniqueId());
      assertThat(reason).contains(mallory.pinnedId().uniqueId());
      assertThat(reason).contains("§7.3 step 6");
    });

    // The honest resolution path is unchanged: resolving alice truthfully,
    // Mallory's signature simply does not verify against alice's
    // certificate — the identity binding adds no rejection of its own.
    VerificationResult honest = world.verifier().verify(forged);
    assertThat(honest.verified()).isFalse();
    assertThat(honest.reason()).hasValueSatisfying(reason ->
        assertThat(reason).contains("does not verify"));
  }

  @Test
  void rejectsARevokedCertificateEvenWithAWarmCryptoCache() throws Exception {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // First verification populates the crypto sub-result cache
    assertThat(world.verifier().verify(envelope).verified()).isTrue();

    // Revocation at verification time must still reject (§11 rule 11) — the
    // resolver already refuses revoked personas ...
    world.registrar().revoke(alice.unpinnedId().id(), "revocation-pass");
    VerificationResult viaResolver = world.verifier().verify(envelope);
    assertThat(viaResolver.verified()).isFalse();
    assertThat(viaResolver.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("could not be resolved"));

    // ... and even a resolver that ignores revocation is stopped by the
    // trust validator, proving the cache stores only the cryptographic
    // sub-result and every policy check is repeated per envelope
    PersonaResolver blind = personaId -> Optional.of(alice.registered().persona());
    VerificationResult viaTrust = world.verifier(blind, world.trust(), Clock.systemUTC())
        .verify(envelope);
    assertThat(viaTrust.verified()).isFalse();
    assertThat(viaTrust.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("revoked"));
  }

  @Test
  void rejectsAnExpiredCertificate() {
    // A registrar whose certificates expire within half a second, verified
    // at a clock one hour past their validity window
    EnvelopeWorld world = new EnvelopeWorld(Duration.ofMillis(500));
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    Clock expired = Clock.fixed(
        alice.registered().certificate().validTill().plusSeconds(3600), ZoneOffset.UTC);
    VerificationResult result = world.verifier(expired).verify(envelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("expired"));
  }

  @Test
  void rejectsACertificateThatIsNotValidYet() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    Clock tooEarly = Clock.fixed(
        alice.registered().certificate().validFrom().minusSeconds(3600), ZoneOffset.UTC);
    VerificationResult result = world.verifier(tooEarly).verify(envelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("not valid yet"));
  }

  @Test
  void rejectsACertificateThatDoesNotChainToATrustedRoot() {
    // The persona resolves and is unrevoked, but the recipient pins a
    // different realm's registrar root (protocol §3.4 rule 5, §11 rule 12)
    EnvelopeWorld homeRealm = new EnvelopeWorld();
    EnvelopeWorld otherRealm = new EnvelopeWorld();
    PersonaFixture alice = homeRealm.mint();
    SignedEnvelope envelope =
        homeRealm.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    TrustValidator otherRoots = new TrustValidator(
        otherRealm.registrar().trustedRoots(), otherRealm.registrar()::isRevoked);
    VerificationResult result = homeRealm
        .verifier(new InMemoryPersonaResolver(homeRealm.registrar()), otherRoots,
            Clock.systemUTC())
        .verify(envelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason ->
            assertThat(reason).contains("does not chain to a trusted registrar root"));
  }

  // -----------------------------------------------------------------------------------
  // §7.3 step 7: preimage + signature verification
  // -----------------------------------------------------------------------------------

  @Test
  void rejectsATamperedPayload() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // Same schema, decodes fine, passes every policy — but the bytes differ
    // from the signed bytes, so the signature fails
    SignedEnvelope tampered = withTamperedPayload(world, envelope, "tampered body");

    VerificationResult result = world.verifier().verify(tampered);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("does not verify");
      assertThat(reason).contains("§7.3 step 7");
    });
  }

  @Test
  void rejectsATamperedSignature() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    Signature original = envelope.signatures().get(0);
    byte[] flipped = original.value();
    flipped[0] = (byte) (flipped[0] ^ 0x55);
    SignedEnvelope tampered = relabel(envelope, null, null, null,
        List.of(new Signature(original.algorithm(), flipped)));

    VerificationResult result = world.verifier().verify(tampered);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("does not verify"));
  }

  @Test
  void rejectsASignatureComputedWithAnotherPersonasKey() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture mallory = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // Everything about the envelope is alice's — except the signature,
    // which mallory computed over the very same preimage
    byte[] preimage = SigningPreimage.forSignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
        envelope.payload());
    byte[] forged;
    try {
      forged = CRYPTO.getSignature(POLICY_ALGORITHM, mallory.key().privateKey(), preimage);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
    SignedEnvelope forgedEnvelope = relabel(envelope, null, null, null,
        List.of(new Signature(POLICY_ALGORITHM, forged)));

    VerificationResult result = world.verifier().verify(forgedEnvelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("does not verify"));
  }

  // -----------------------------------------------------------------------------------
  // §6.4 type confusion: re-labeling must never keep a signature valid
  // -----------------------------------------------------------------------------------

  @Test
  void rejectsARelabeledMessageType() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // Relabel to the isomorphic message type AND its schemaId, so schema
    // resolution, decoding, binding and authorization all still pass; only
    // the §6.4 preimage can (and must) reject the transplant
    SignedEnvelope relabeled = relabel(envelope, "example.Greeting2@v1", null,
        world.schemaIdOf("example.Greeting2@v1"), null);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("does not verify");
      assertThat(reason).contains("relabel");
    });
  }

  @Test
  void rejectsARemovedSchemaId() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope envelope = world.signer().sign(greeting(alice, "hello"), alice.key(), "json");

    // Stripping the schemaId keeps every step green — the schema still
    // resolves via the message type — but the preimage changes
    SignedEnvelope relabeled = relabel(envelope, null, null, "", null);

    VerificationResult result = world.verifier().verify(relabeled);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("does not verify"));
  }

  @Test
  void rejectsASingleSignedSignatureTransplantedIntoAMultiSignedEnvelope() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    TestMessages.TestEscrowInstruction escrow = escrow(alice, bob);

    // The escrow instruction implements both §5 contracts, so every policy
    // step of the multi verification passes; only the §6.4 domain
    // separation can (and must) reject the transplant
    SignedEnvelope single = world.signer().sign(escrow, alice.key(), "json");
    MultiSignedEnvelope transplanted = new MultiSignedEnvelope(
        single.messageType(), single.payloadFormat(), single.schemaId(), single.payload(),
        List.of(new MultiSignature(single.signedBy(), single.signatures())));

    VerificationResult result = world.verifier().verify(transplanted);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("does not verify"));
  }

  @Test
  void rejectsAMultiSignedSignatureTransplantedIntoASingleSignedEnvelope() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    TestMessages.TestEscrowInstruction escrow = escrow(alice, bob);

    MultiSignedEnvelope multi = world.signer().signMulti(
        TestMessages.TestEscrowInstruction.MESSAGE_TYPE, escrow, alice.key(), "json");
    SignedEnvelope transplanted = new SignedEnvelope(
        multi.messageType(), multi.payloadFormat(), multi.schemaId(), multi.payload(),
        multi.signatures().get(0).signedBy(), multi.signatures().get(0).signatures());

    VerificationResult result = world.verifier().verify(transplanted);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("does not verify"));
  }

  // -----------------------------------------------------------------------------------
  // §7.4 + §6.2: multi-signature quorum
  // -----------------------------------------------------------------------------------

  @Test
  void verifiesAMultiSignedEnvelopeThatReachesQuorum() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    TestMessages.TestCharter charter = new TestMessages.TestCharter(
        "c-1", Set.of(alice.unpinnedId(), bob.unpinnedId()), 2, "terms");

    MultiSignedEnvelope envelope = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");
    assertThat(world.verifier().verify(envelope).verified())
        .as("one signature of two is not a quorum")
        .isFalse();

    envelope = world.signer().addSignature(envelope, bob.key());
    VerificationResult result = world.verifier().verify(envelope);
    assertThat(result.verified()).isTrue();
  }

  @Test
  void countsDuplicateSpellingsOfOneCertificateAsASingleSigner() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    TestMessages.TestCharter charter = new TestMessages.TestCharter(
        "c-1", Set.of(alice.unpinnedId(), bob.unpinnedId()), 2, "terms");

    // alice signs twice, under both spellings of her id — p-alice and
    // p-alice#<thumbprint> resolve to the same certificate, so §6.2 counts
    // her once and the 2-signature quorum is NOT reached (protocol §6.2:
    // "never two")
    MultiSignedEnvelope envelope = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.keyAs(alice.unpinnedId()),
        "json");
    envelope = world.signer().addSignature(envelope, alice.key());

    VerificationResult duplicated = world.verifier().verify(envelope);
    assertThat(duplicated.verified()).isFalse();
    assertThat(duplicated.reason())
        .hasValueSatisfying(reason -> assertThat(reason).contains("quorum not reached"));

    // bob's distinct certificate completes the quorum
    envelope = world.signer().addSignature(envelope, bob.key());
    assertThat(world.verifier().verify(envelope).verified()).isTrue();
  }

  @Test
  void rejectsACryptographicallyValidEntryFromAnUnauthorizedCoSigner() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    PersonaFixture mallory = world.mint();

    // Founders {alice, bob}, quorum 1 — and a payload rule that counts the
    // RAW verified signer set, so it cannot mask the verifier's per-entry
    // authorization check (the default rule would silently drop mallory's
    // entry at quorum counting and this test would pass for the wrong reason)
    TestMessages.TestRawCountingCharter charter = new TestMessages.TestRawCountingCharter(
        "rc-1", Set.of(alice.unpinnedId(), bob.unpinnedId()), 1, "terms");
    MultiSignedEnvelope genuine = world.signer().signMulti(
        TestMessages.TestRawCountingCharter.MESSAGE_TYPE, charter, alice.key(), "json");

    // mallory's entry is everything but authorized: a real persona, a real
    // certificate chaining to the trusted root, and a cryptographically
    // valid signature over the correct §6.4 preimage — hand-built because
    // the signer (correctly) refuses to emit it
    byte[] preimage = SigningPreimage.forMultiSignedEnvelope(
        genuine.messageType(), genuine.payloadFormat(), genuine.schemaId(), genuine.payload());
    byte[] mallorySignature;
    try {
      mallorySignature =
          CRYPTO.getSignature(POLICY_ALGORITHM, mallory.key().privateKey(), preimage);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
    MultiSignedEnvelope hostile = new MultiSignedEnvelope(
        genuine.messageType(), genuine.payloadFormat(), genuine.schemaId(), genuine.payload(),
        List.of(new MultiSignature(mallory.pinnedId(),
            List.of(new Signature(POLICY_ALGORITHM, mallorySignature)))));

    VerificationResult result = world.verifier().verify(hostile);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("not authorized to co-sign");
      assertThat(reason).contains(mallory.pinnedId().uniqueId());
    });
  }

  @Test
  void twoSpellingsOfOneCertificateDoNotReachARawCountingQuorum() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();

    // Quorum 2 counted over the RAW set handed to the payload rule: only
    // the envelope layer's certificate-thumbprint dedup (§6.2) stands
    // between "alice signed twice under two spellings" and a false quorum
    TestMessages.TestRawCountingCharter charter = new TestMessages.TestRawCountingCharter(
        "rc-2", Set.of(alice.unpinnedId(), bob.unpinnedId()), 2, "terms");
    MultiSignedEnvelope envelope = world.signer().signMulti(
        TestMessages.TestRawCountingCharter.MESSAGE_TYPE, charter,
        alice.keyAs(alice.unpinnedId()), "json");
    envelope = world.signer().addSignature(envelope, alice.key());

    VerificationResult result = world.verifier().verify(envelope);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason ->
        assertThat(reason).contains("quorum not reached"));

    // Control: bob's distinct certificate does reach the raw-counting quorum
    envelope = world.signer().addSignature(envelope, bob.key());
    assertThat(world.verifier().verify(envelope).verified()).isTrue();
  }

  @Test
  void signatureOrderNeverAffectsTheOutcome() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    PersonaFixture mallory = world.mint();
    TestMessages.TestCharter charter = new TestMessages.TestCharter(
        "c-1", Set.of(alice.unpinnedId(), bob.unpinnedId(), mallory.unpinnedId()), 2, "terms");

    MultiSignedEnvelope envelope = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");
    envelope = world.signer().addSignature(envelope, bob.key());

    List<MultiSignature> reversed = new ArrayList<>(envelope.signatures().reversed());
    MultiSignedEnvelope reversedEnvelope = new MultiSignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
        envelope.payload(), reversed);
    assertThat(world.verifier().verify(reversedEnvelope).verified())
        .as("protocol §7.4: ordering of signatures MUST NOT affect verification outcome")
        .isTrue();

    // An envelope below quorum fails in every order
    MultiSignedEnvelope below = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");
    assertThat(world.verifier().verify(below).verified()).isFalse();
    assertThat(world.verifier().verify(new MultiSignedEnvelope(
        below.messageType(), below.payloadFormat(), below.schemaId(), below.payload(),
        List.copyOf(below.signatures().reversed()))).verified()).isFalse();
  }

  @Test
  void rejectsAMultiSignedEnvelopeWhereOneEntryIsBad() {
    // §4 "no partial trust": a quorum envelope carrying one broken entry is
    // rejected outright, not "salvaged" from the remaining good entries
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    PersonaFixture bob = world.mint();
    TestMessages.TestCharter charter = new TestMessages.TestCharter(
        "c-1", Set.of(alice.unpinnedId(), bob.unpinnedId()), 1, "terms");

    MultiSignedEnvelope envelope = world.signer().signMulti(
        TestMessages.TestCharter.MESSAGE_TYPE, charter, alice.key(), "json");
    envelope = world.signer().addSignature(envelope, bob.key());

    MultiSignature bobEntry = envelope.signatures().get(1);
    byte[] flipped = bobEntry.signatures().get(0).value();
    flipped[0] = (byte) (flipped[0] ^ 0x55);
    MultiSignature broken = new MultiSignature(bobEntry.signedBy(),
        List.of(new Signature(bobEntry.signatures().get(0).algorithm(), flipped)));
    MultiSignedEnvelope withBrokenEntry = new MultiSignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
        envelope.payload(), List.of(broken, envelope.signatures().get(0)));

    VerificationResult result = world.verifier().verify(withBrokenEntry);
    assertThat(result.verified()).isFalse();
    assertThat(result.reason()).hasValueSatisfying(reason -> {
      assertThat(reason).contains("does not verify");
      assertThat(reason).contains(bob.pinnedId().uniqueId());
    });
  }

  // -----------------------------------------------------------------------------------
  // Crypto sub-result cache: bounded, and eviction never changes a verdict
  // -----------------------------------------------------------------------------------

  @Test
  void cacheEvictionNeverChangesAVerdict() {
    EnvelopeWorld world = new EnvelopeWorld();
    PersonaFixture alice = world.mint();
    SignedEnvelope one = world.signer().sign(greeting(alice, "one"), alice.key(), "json");
    SignedEnvelope two = world.signer().sign(greeting(alice, "two"), alice.key(), "json");
    SignedEnvelope three = world.signer().sign(greeting(alice, "three"), alice.key(), "json");

    // A 2-entry cache: verifying the third envelope evicts the eldest entry
    EnvelopeVerifier tiny = new EnvelopeVerifier(
        PersonaVersion.latestVersion(), CRYPTO, world.schemaRegistry(), world.bindings(),
        new InMemoryPersonaResolver(world.registrar()), world.trust(), Clock.systemUTC(), 2);

    assertThat(tiny.verify(one).verified()).isTrue();
    assertThat(tiny.verify(two).verified()).isTrue();
    assertThat(tiny.verify(three).verified()).isTrue();
    // 'one' was evicted; its sub-result is simply recomputed — same verdict
    assertThat(tiny.verify(one).verified()).isTrue();
    assertThat(tiny.verify(one).verified()).isTrue();
  }

  @Test
  void rejectsANonPositiveCacheCapacity() {
    EnvelopeWorld world = new EnvelopeWorld();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> new EnvelopeVerifier(
        PersonaVersion.latestVersion(), CRYPTO, world.schemaRegistry(), world.bindings(),
        new InMemoryPersonaResolver(world.registrar()), world.trust(), Clock.systemUTC(), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cacheCapacity");
  }

  // -----------------------------------------------------------------------------------
  // Fixtures
  // -----------------------------------------------------------------------------------

  private static TestMessages.TestGreeting greeting(PersonaFixture from, String body) {
    return new TestMessages.TestGreeting(
        "g-1", from.unpinnedId(), PersonaId.of("p-bob"), body);
  }

  private static TestMessages.TestEscrowInstruction escrow(PersonaFixture partyA,
      PersonaFixture partyB) {
    return new TestMessages.TestEscrowInstruction(
        "e-1", partyA.unpinnedId(), partyB.unpinnedId(), "100");
  }

  /**
   * Rebuilds {@code envelope} with the given fields replaced (a null argument
   * keeps the original field), keeping the payload and signature bytes —
   * exactly what an attacker re-labeling an intercepted envelope produces.
   */
  private static SignedEnvelope relabel(
      SignedEnvelope envelope,
      String messageType,
      String payloadFormat,
      String schemaId,
      List<Signature> signatures) {
    return relabel(envelope, messageType, payloadFormat, schemaId, signatures, null);
  }

  private static SignedEnvelope relabel(
      SignedEnvelope envelope,
      String messageType,
      String payloadFormat,
      String schemaId,
      List<Signature> signatures,
      PersonaId signedBy) {
    return new SignedEnvelope(
        messageType != null ? messageType : envelope.messageType(),
        payloadFormat != null ? payloadFormat : envelope.payloadFormat(),
        schemaId != null ? schemaId : envelope.schemaId(),
        envelope.payload(),
        signedBy != null ? signedBy : envelope.signedBy(),
        signatures != null ? signatures : envelope.signatures());
  }

  /**
   * Re-encodes the envelope's payload with {@code replacement} in its string
   * body field: valid bytes under the same schema, but different from the
   * signed bytes.
   */
  private static SignedEnvelope withTamperedPayload(
      EnvelopeWorld world, SignedEnvelope envelope, String replacement) {
    JsonPayloadCodec codec = new JsonPayloadCodec();
    var schema = world.schemaRegistry().requireByMessageType(envelope.messageType());
    Map<String, Object> value = codec.decode(schema, envelope.payload());
    value.put("body", replacement);
    byte[] tampered = codec.encode(schema, value);
    return new SignedEnvelope(envelope.messageType(), envelope.payloadFormat(),
        envelope.schemaId(), tampered, envelope.signedBy(), envelope.signatures());
  }

  /**
   * Hand-signs arbitrary payload bytes as {@code signer} — how a hostile
   * envelope the signer would refuse to emit still gets a valid signature
   * (the signer's own key, the correct preimage).
   */
  private static SignedEnvelope handSigned(
      EnvelopeWorld world, PersonaFixture signer, byte[] payload) {
    String messageType = "example.Greeting@v1";
    String schemaId = world.schemaIdOf(messageType);
    byte[] preimage =
        SigningPreimage.forSignedEnvelope(messageType, "json", schemaId, payload);
    byte[] signature;
    try {
      signature = CRYPTO.getSignature(POLICY_ALGORITHM, signer.key().privateKey(), preimage);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
    return new SignedEnvelope(messageType, "json", schemaId, payload, signer.pinnedId(),
        List.of(new Signature(POLICY_ALGORITHM, signature)));
  }
}
