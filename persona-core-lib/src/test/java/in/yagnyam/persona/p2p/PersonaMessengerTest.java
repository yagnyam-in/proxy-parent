package in.yagnyam.persona.p2p;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.PersonaKey;
import in.yagnyam.persona.envelope.MultiSignature;
import in.yagnyam.persona.envelope.MultiSignedEnvelope;
import in.yagnyam.persona.envelope.SignedEnvelope;
import in.yagnyam.persona.p2p.P2pTestMessages.TestAlert;
import in.yagnyam.persona.p2p.P2pTestMessages.TestCharter;
import in.yagnyam.persona.p2p.P2pTestMessages.TestGreeting;
import in.yagnyam.persona.p2p.P2pTestMessages.TestJointRequest;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PersonaMessenger} (protocol §13): the send path encodes
 * and delivers, the receive path decodes, runs the full §7.3 verification
 * and materializes the payload only on success — every failure is a
 * {@link ReceivedMessage} value, never an exception.
 */
class PersonaMessengerTest {

  private final P2pWorld world = new P2pWorld();
  private final PersonaKey aliceKey = world.mint();
  private final PersonaKey bobKey = world.mint();
  private final PersonaMessenger alice = world.messenger(aliceKey);
  private final PersonaMessenger bob = world.messenger(bobKey);

  private TestGreeting greeting(String body) {
    return new TestGreeting(
        "greeting-1", aliceKey.id(), bobKey.id(), body);
  }

  // -------------------------------------------------------------------------------------
  // Send + receive round-trip
  // -------------------------------------------------------------------------------------

  @Test
  void addressableMessageRoundTripsToItsDeclaredRecipient() throws Exception {
    ConcurrentLinkedQueue<ReceivedMessage> inbox = new ConcurrentLinkedQueue<>();
    try (AutoCloseable ignored = bob.listen(inbox::add)) {
      alice.send(greeting("Hello Bob!"), "json");

      assertThat(inbox).hasSize(1);
      ReceivedMessage received = inbox.peek();
      assertThat(received.verified()).isTrue();
      assertThat(received.multiSigned()).isFalse();
      assertThat(received.messageType()).isEqualTo(TestGreeting.MESSAGE_TYPE);
      assertThat(received.signers()).containsExactly(aliceKey.id());
      assertThat(received.payload()).contains(greeting("Hello Bob!"));
    }
  }

  @Test
  void sendToAnExplicitRecipientRoutesThere() {
    PersonaMessenger carol = world.messenger(world.mint());
    ConcurrentLinkedQueue<ReceivedMessage> carolInbox = new ConcurrentLinkedQueue<>();
    carol.listen(carolInbox::add);

    // The payload is addressed to bob (§5.3), but the explicit recipient
    // wins routing: the address inside a payload is metadata, and envelope
    // verification does not tie it to the receiving endpoint (protocol
    // §7.3 has no such step).
    alice.send(greeting("Hi"), carol.personaId(), "json");

    assertThat(carolInbox).hasSize(1);
    assertThat(carolInbox.peek().verified()).isTrue();
  }

  @Test
  void sendWithoutAnAddressRequiresAnAddressableMessage() {
    // An alert is signable but not addressable: it has an audience, not
    // a single recipient address.
    TestAlert alert = new TestAlert(
        "alert-1", P2pWorld.REALM, aliceKey.id(), List.of(bobKey.id()), "heads up");
    assertThatThrownBy(() -> alice.send(alert, "json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("AddressableMessage");
  }

  @Test
  void sendToAnUnknownRecipientFailsLoudly() {
    TestGreeting toNowhere =
        new TestGreeting("g-1", aliceKey.id(), PersonaId.of("nobody"), "hi");
    assertThatThrownBy(() -> alice.send(toNowhere, "json"))
        .isInstanceOf(DeliveryException.class);
  }

  // -------------------------------------------------------------------------------------
  // Receive: fail closed
  // -------------------------------------------------------------------------------------

  @Test
  void aTamperedPayloadIsRejectedAndExposesNoPayload() {
    SignedEnvelope envelope = world.signer().sign(greeting("Hello Bob!"), aliceKey, "json");
    byte[] tampered = tamperPayloadByte(envelope);

    ReceivedMessage received = bob.receive(tampered);

    assertThat(received.verified()).isFalse();
    assertThat(received.payload()).isEmpty();
    assertThat(received.messageType()).isEqualTo(TestGreeting.MESSAGE_TYPE);
    assertThat(received.signers()).containsExactly(aliceKey.id());
    assertThat(received.verification().reason()).hasValueSatisfying(
        reason -> assertThat(reason).contains("does not verify"));
  }

  @Test
  void bytesThatAreNotAnEnvelopeAreRejected() {
    ReceivedMessage received = bob.receive("not an envelope".getBytes(UTF_8));

    assertThat(received.verified()).isFalse();
    assertThat(received.payload()).isEmpty();
    assertThat(received.messageType()).isNull();
    assertThat(received.signers()).isEmpty();
    assertThat(received.verification().reason()).hasValueSatisfying(
        reason -> assertThat(reason).contains("not a decodable envelope"));
  }

  @Test
  void nullBytesAreRejected() {
    ReceivedMessage received = bob.receive(null);

    assertThat(received.verified()).isFalse();
    assertThat(received.verification().reason()).hasValueSatisfying(
        reason -> assertThat(reason).contains("null"));
  }

  // -------------------------------------------------------------------------------------
  // Multi-signature (protocol §7.4) and alerts (protocol §5.4) pass-throughs
  // -------------------------------------------------------------------------------------

  @Test
  void quorumFlowCompletesWhenTheSecondFounderCoSigns() {
    TestCharter charter = new TestCharter(
        "charter-1", Set.of(aliceKey.id(), bobKey.id()), 2, "the terms");

    // Alice opens with one entry; bob captures the raw bytes off the wire.
    ConcurrentLinkedQueue<byte[]> bobWire = new ConcurrentLinkedQueue<>();
    world.transport().listen(bobKey.id().id(), bobWire::add);
    MultiSignedEnvelope opened =
        alice.sendMulti(TestCharter.MESSAGE_TYPE, charter, bobKey.id(), "json");
    assertThat(bobWire).hasSize(1);

    // One entry, quorum of two: not yet verified.
    ReceivedMessage partial = bob.receive(bobWire.poll());
    assertThat(partial.verified()).isFalse();
    assertThat(partial.multiSigned()).isTrue();
    assertThat(partial.verification().reason()).hasValueSatisfying(
        reason -> assertThat(reason).contains("quorum"));

    // Bob co-signs and returns the completed envelope to alice.
    MultiSignedEnvelope completed = bob.coSign(opened);
    ConcurrentLinkedQueue<ReceivedMessage> aliceInbox = new ConcurrentLinkedQueue<>();
    alice.listen(aliceInbox::add);
    bob.sendMulti(completed, aliceKey.id());

    assertThat(aliceInbox).hasSize(1);
    ReceivedMessage received = aliceInbox.peek();
    assertThat(received.verified()).isTrue();
    assertThat(received.multiSigned()).isTrue();
    assertThat(received.signers()).containsExactlyInAnyOrder(aliceKey.id(), bobKey.id());
    assertThat(received.payload()).contains(charter);
  }

  @Test
  void anAlertIsDeliveredToEveryReceiverWithItsMetadata() {
    ConcurrentLinkedQueue<ReceivedMessage> bobInbox = new ConcurrentLinkedQueue<>();
    PersonaKey carolKey = world.mint();
    PersonaMessenger carol = world.messenger(carolKey);
    ConcurrentLinkedQueue<ReceivedMessage> carolInbox = new ConcurrentLinkedQueue<>();
    bob.listen(bobInbox::add);
    carol.listen(carolInbox::add);

    TestAlert alert = new TestAlert(
        "alert-1", P2pWorld.REALM, aliceKey.id(),
        List.of(bobKey.id(), carolKey.id()), "maintenance tonight");
    alice.sendAlert(alert, "json");

    assertThat(bobInbox).hasSize(1);
    assertThat(carolInbox).hasSize(1);
    ReceivedMessage received = bobInbox.peek();
    assertThat(received.verified()).isTrue();
    assertThat(received.payload()).contains(alert);
    assertThat(alert.transportMetadata().alertId()).isEqualTo("alert-1");
    assertThat(alert.transportMetadata().alertType()).isEqualTo(TestAlert.MESSAGE_TYPE);
    assertThat(alert.transportMetadata().realm()).isEqualTo(P2pWorld.REALM);
  }

  // -------------------------------------------------------------------------------------
  // Replay protection (protocol §5.2, §11 rule 5)
  // -------------------------------------------------------------------------------------

  @Test
  void identicalBytesDeliveredTwiceAreAcceptedExactlyOnce() {
    byte[] wire = captureWireBytes(() -> alice.send(greeting("Hello Bob!"), "json"));

    ReceivedMessage first = bob.receive(wire);
    ReceivedMessage replay = bob.receive(wire);

    assertThat(first.verified()).isTrue();
    assertThat(first.replayed()).isFalse();
    assertThat(first.payload()).isPresent();

    // The replay is a distinct outcome from a verification rejection: the
    // envelope DID verify; only the duplicate delivery is refused.
    assertThat(replay.verified()).isFalse();
    assertThat(replay.replayed()).isTrue();
    assertThat(replay.payload()).isEmpty();
    assertThat(replay.messageType()).isEqualTo(TestGreeting.MESSAGE_TYPE);
    assertThat(replay.signers()).containsExactly(aliceKey.id());
    assertThat(replay.verification().reason()).hasValueSatisfying(
        reason -> assertThat(reason).contains("duplicate request id"));
  }

  @Test
  void theListenCallbackPathDeduplicatesToo() throws Exception {
    byte[] wire = captureWireBytes(() -> alice.send(greeting("Hello Bob!"), "json"));
    ConcurrentLinkedQueue<ReceivedMessage> inbox = new ConcurrentLinkedQueue<>();
    try (AutoCloseable ignored = bob.listen(inbox::add)) {
      world.transport().send(bobKey.id().id(), wire);
      world.transport().send(bobKey.id().id(), wire);
    }

    assertThat(inbox).hasSize(2);
    assertThat(inbox.stream().filter(ReceivedMessage::verified).count()).isEqualTo(1);
    assertThat(inbox.stream().filter(ReceivedMessage::replayed).count()).isEqualTo(1);
    assertThat(inbox.stream().filter(m -> m.payload().isPresent()).count()).isEqualTo(1);
  }

  @Test
  void theSameRequestDeliveredToTwoRecipientsIsProcessedByEach() {
    PersonaMessenger carol = world.messenger(world.mint());
    TestGreeting forBob = greeting("shared-request");
    // Same sender, same requestId, addressed to carol.
    TestGreeting forCarol = new TestGreeting(
        "shared-request", aliceKey.id(), carol.personaId(), "Hello Carol!");

    assertThat(bob.receive(
        world.signer().sign(forBob, aliceKey, "json").encode(new JsonPayloadCodec()))
        .verified()).isTrue();
    assertThat(carol.receive(
        world.signer().sign(forCarol, aliceKey, "json").encode(new JsonPayloadCodec()))
        .verified()).isTrue();
  }

  @Test
  void aForgedRequestIdIsRejectedWithoutPoisoningTheVictimsDedupStore() {
    // Attacker captures alice's envelope for request "req-poison" and
    // tampers it: verification must fail BEFORE the dedup check, so the
    // forged requestId never enters bob's store.
    SignedEnvelope genuine = world.signer().sign(
        new TestGreeting("req-poison", aliceKey.id(), bobKey.id(), "Hello Bob!"),
        aliceKey, "json");
    byte[] forged = tamperPayloadByte(genuine);

    ReceivedMessage rejected = bob.receive(forged);
    assertThat(rejected.verified()).isFalse();
    assertThat(rejected.replayed()).isFalse();
    assertThat(rejected.verification().reason()).hasValueSatisfying(
        reason -> assertThat(reason).contains("does not verify"));

    // The victim's genuine request with the very same requestId is still
    // accepted: the forgery did not poison the dedup store.
    ReceivedMessage accepted = bob.receive(genuine.encode(new JsonPayloadCodec()));
    assertThat(accepted.verified()).isTrue();
    assertThat(accepted.payload()).isPresent();
  }

  @Test
  void afterEvictionARerequestIsProcessedAgainAndThenDeduplicated() {
    PersonaMessenger bobBounded = world.messenger(bobKey, new RequestDeduplicator(2));

    byte[] req1 = captureWireBytes(() -> alice.send(
        new TestGreeting("req-1", aliceKey.id(), bobKey.id(), "one"), "json"));
    byte[] req2 = captureWireBytes(() -> alice.send(
        new TestGreeting("req-2", aliceKey.id(), bobKey.id(), "two"), "json"));
    byte[] req3 = captureWireBytes(() -> alice.send(
        new TestGreeting("req-3", aliceKey.id(), bobKey.id(), "three"), "json"));

    assertThat(bobBounded.receive(req1).verified()).isTrue();
    assertThat(bobBounded.receive(req1).replayed()).isTrue();
    assertThat(bobBounded.receive(req2).verified()).isTrue();
    assertThat(bobBounded.receive(req3).verified()).isTrue();

    // Capacity exceeded: req-1 was evicted, so its re-request is processed
    // again (the safe eviction direction) — and then deduplicated anew.
    assertThat(bobBounded.receive(req1).verified()).isTrue();
    assertThat(bobBounded.receive(req1).replayed()).isTrue();
  }

  @Test
  void concurrentDuplicateDeliveriesAreAcceptedExactlyOnce() throws InterruptedException {
    byte[] wire = captureWireBytes(() -> alice.send(greeting("Hello Bob!"), "json"));
    int threads = 32;
    AtomicInteger verified = new AtomicInteger();
    AtomicInteger replayed = new AtomicInteger();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    for (int t = 0; t < threads; t++) {
      pool.submit(() -> {
        try {
          start.await();
          ReceivedMessage received = bob.receive(wire);
          if (received.verified()) {
            verified.incrementAndGet();
          } else if (received.replayed()) {
            replayed.incrementAndGet();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    assertThat(verified.get()).isEqualTo(1);
    assertThat(replayed.get()).isEqualTo(threads - 1);
  }

  @Test
  void nonRequestPayloadsAreNotDeduplicated() {
    // A multi-signed charter is not a RequestMessage: delivering the same
    // completed envelope twice is not a replay.
    TestCharter charter = new TestCharter(
        "charter-1", Set.of(aliceKey.id(), bobKey.id()), 2, "the terms");
    MultiSignedEnvelope opened =
        world.signer().signMulti(TestCharter.MESSAGE_TYPE, charter, aliceKey, "json");
    MultiSignedEnvelope completed = bob.coSign(opened);
    byte[] wire = completed.encode(new JsonPayloadCodec());

    assertThat(alice.receive(wire).verified()).isTrue();
    assertThat(alice.receive(wire).verified()).isTrue();

    // Nor is an alert: the same alert bytes broadcast twice both verify.
    TestAlert alert = new TestAlert(
        "alert-1", P2pWorld.REALM, aliceKey.id(), List.of(bobKey.id()), "heads up");
    SignedEnvelope alertEnvelope = world.signer().sign(alert, aliceKey, "json");
    byte[] alertWire = alertEnvelope.encode(new JsonPayloadCodec());

    assertThat(bob.receive(alertWire).verified()).isTrue();
    assertThat(bob.receive(alertWire).verified()).isTrue();
  }

  // -------------------------------------------------------------------------------------
  // Replay protection of multi-signed requests (protocol §5.2 over §7.4):
  // two tiers, both enforced before the payload reaches a handler. Tier B
  // keys on the authenticated §6.4 preimage bytes (order-independent and
  // spelling-independent, since the signatures array is not
  // authenticated) and catches every respelling — reorder, respell,
  // incremental-superset, disjoint-quorum split — of ONE captured
  // envelope. Tier A keys on (recipient, requestId, bare signer id) and
  // catches what tier B cannot: a retry from the SAME requester with a
  // DIFFERENT payload under a reused requestId. A disjoint quorum with a
  // different payload passes both tiers by design (§5.2 scopes
  // uniqueness to the requesting Persona).
  // -------------------------------------------------------------------------------------

  @Test
  void aMultiSignedRequestReplayedWithReorderedSignaturesIsAcceptedExactlyOnce() {
    PersonaMessenger carol = world.messenger(world.mint());
    TestJointRequest request = new TestJointRequest(
        "joint-1", Set.of(aliceKey.id(), bobKey.id()), 2, "open the vault");
    MultiSignedEnvelope completed = world.signer().addSignature(
        world.signer().signMulti(TestJointRequest.MESSAGE_TYPE, request, aliceKey, "json"),
        bobKey);
    assertThat(completed.signatures()).hasSize(2);

    // The attacker-legal wire transformation: swap the signature entries.
    // The reordered envelope still verifies (§7.4) — only dedup can stop
    // the second delivery from reaching handlers.
    MultiSignedEnvelope reordered = new MultiSignedEnvelope(
        completed.messageType(), completed.payloadFormat(), completed.schemaId(),
        completed.payload(),
        List.of(completed.signatures().get(1), completed.signatures().get(0)));

    ReceivedMessage accepted = carol.receive(completed.encode(new JsonPayloadCodec()));
    ReceivedMessage replay = carol.receive(reordered.encode(new JsonPayloadCodec()));

    assertThat(accepted.verified()).isTrue();
    assertThat(accepted.payload()).contains(request);

    assertThat(replay.verified()).isFalse();
    assertThat(replay.replayed()).isTrue();
    assertThat(replay.payload()).isEmpty();
    assertThat(replay.signers()).containsExactly(bobKey.id(), aliceKey.id());
    assertThat(replay.verification().reason()).hasValueSatisfying(
        reason -> assertThat(reason).contains("duplicate request id"));
  }

  @Test
  void aMultiSignedRequestReplayedWithRespelledSignerIdsIsDeduplicated() {
    PersonaMessenger carol = world.messenger(world.mint());
    // Unpinned founders, so both the pinned and the unpinned signedBy
    // spellings are authorized (§5.1 delegation) and the respelled envelope
    // still verifies.
    TestJointRequest request = new TestJointRequest(
        "joint-1",
        Set.of(PersonaId.of(aliceKey.id().id()), PersonaId.of(bobKey.id().id())),
        2, "open the vault");
    MultiSignedEnvelope completed = world.signer().addSignature(
        world.signer().signMulti(TestJointRequest.MESSAGE_TYPE, request, aliceKey, "json"),
        bobKey);

    // An unauthenticated respelling: strip the certificate pins from the
    // signedBy ids (and reorder for good measure). 'alice' and
    // 'alice#<thumbprint>' resolving to one certificate are one signer
    // (protocol §6.2), so the respelled envelope verifies — and dedups.
    List<MultiSignature> respelled = completed.signatures().stream()
        .map(entry -> new MultiSignature(
            PersonaId.of(entry.signedBy().id()), entry.signatures()))
        .toList();
    MultiSignedEnvelope variant = new MultiSignedEnvelope(
        completed.messageType(), completed.payloadFormat(), completed.schemaId(),
        completed.payload(), List.of(respelled.get(1), respelled.get(0)));

    assertThat(carol.receive(completed.encode(new JsonPayloadCodec())).verified()).isTrue();
    ReceivedMessage replay = carol.receive(variant.encode(new JsonPayloadCodec()));
    assertThat(replay.replayed()).isTrue();
    assertThat(replay.payload()).isEmpty();
  }

  @Test
  void aRequestReSignedWithAnAdditionalCoSignerDeduplicatesAsAReplay() {
    // Incremental §7.4 co-signing: the payload MAY be signed incrementally
    // and each additional signature yields a new envelope that SUPERSEDES
    // the previous one (protocol §5.5). The requestId is the same, the
    // payload bytes are identical; only the authorization grew. Executing
    // the request again would double-execute it, so the stronger envelope
    // dedups as a replay — the safe direction.
    PersonaKey daveKey = world.mint();
    PersonaMessenger carol = world.messenger(world.mint());
    TestJointRequest request = new TestJointRequest(
        "joint-2", Set.of(aliceKey.id(), bobKey.id(), daveKey.id()), 2, "open the vault");
    MultiSignedEnvelope twoOfThree = world.signer().addSignature(
        world.signer().signMulti(TestJointRequest.MESSAGE_TYPE, request, aliceKey, "json"),
        bobKey);
    MultiSignedEnvelope threeOfThree = world.signer().addSignature(twoOfThree, daveKey);

    ReceivedMessage accepted = carol.receive(twoOfThree.encode(new JsonPayloadCodec()));
    assertThat(accepted.verified()).isTrue();

    ReceivedMessage superseding = carol.receive(threeOfThree.encode(new JsonPayloadCodec()));
    assertThat(superseding.replayed()).isTrue();
    assertThat(superseding.payload()).isEmpty();
  }

  @Test
  void aCapturedEnvelopeSplitIntoDisjointQuorumSubsetsIsAcceptedExactlyOnce() {
    // The disjoint-quorum split replay: a transport attacker captures a
    // 4-founder joint request with quorum 2, fully signed, and splits the
    // wire envelope into two deliveries retaining disjoint signature
    // subsets — {alice,bob} then {carol,dave}. The retained signatures
    // array is not authenticated (§6.4), so both subsets are legal wire
    // forms, every retained signature verifies, and each subset meets
    // quorum — yet both authenticate the SAME preimage bytes, so the
    // second delivery is a replay and the handler fires exactly once.
    PersonaKey carolKey = world.mint();
    PersonaKey daveKey = world.mint();
    PersonaMessenger erin = world.messenger(world.mint());
    TestJointRequest request = new TestJointRequest("joint-split",
        Set.of(aliceKey.id(), bobKey.id(), carolKey.id(), daveKey.id()), 2, "open the vault");
    MultiSignedEnvelope fullySigned = world.signer().addSignature(
        world.signer().addSignature(
            world.signer().addSignature(
                world.signer().signMulti(
                    TestJointRequest.MESSAGE_TYPE, request, aliceKey, "json"),
                bobKey),
            carolKey),
        daveKey);
    assertThat(fullySigned.signatures()).hasSize(4);

    JsonPayloadCodec codec = new JsonPayloadCodec();
    MultiSignedEnvelope decoded =
        MultiSignedEnvelope.decode(fullySigned.encode(codec), codec);
    // Subset {alice,bob} meets quorum; subset {carol,dave} is disjoint
    // and also meets quorum.
    MultiSignedEnvelope aliceBob = new MultiSignedEnvelope(
        decoded.messageType(), decoded.payloadFormat(), decoded.schemaId(), decoded.payload(),
        List.of(decoded.signatures().get(0), decoded.signatures().get(1)));
    MultiSignedEnvelope carolDave = new MultiSignedEnvelope(
        decoded.messageType(), decoded.payloadFormat(), decoded.schemaId(), decoded.payload(),
        List.of(decoded.signatures().get(2), decoded.signatures().get(3)));

    AtomicInteger handled = new AtomicInteger();
    ReceivedMessage first = erin.receive(aliceBob.encode(codec));
    if (first.verified()) {
      handled.incrementAndGet();
    }
    ReceivedMessage second = erin.receive(carolDave.encode(codec));
    if (second.verified()) {
      handled.incrementAndGet();
    }

    assertThat(first.verified()).isTrue();
    assertThat(first.payload()).contains(request);
    assertThat(first.signers()).containsExactlyInAnyOrder(aliceKey.id(), bobKey.id());

    assertThat(second.verified()).isFalse();
    assertThat(second.replayed()).isTrue();
    assertThat(second.payload()).isEmpty();
    assertThat(second.signers()).containsExactlyInAnyOrder(carolKey.id(), daveKey.id());
    assertThat(second.verification().reason()).hasValueSatisfying(
        reason -> assertThat(reason).contains("duplicate request id"));
    assertThat(handled.get()).isEqualTo(1);
  }

  @Test
  void aRetryFromTheSameRequesterWithADifferentPayloadIsRejectedAsDuplicate() {
    // §5.2/§11 rule 5 scope requestId uniqueness to the requesting
    // Persona, not to a byte sequence: a second VERIFIED request from the
    // same requester under the same requestId is a duplicate even when
    // its authenticated bytes differ (a retry with a changed body, or the
    // same payload re-encoded under another payloadFormat). A
    // byte-level key alone would process both; tier A (signer-set
    // overlap on (recipient, requestId)) rejects the retries and the
    // handler fires exactly once.
    AtomicInteger handled = new AtomicInteger();
    String requestId = "greeting-retry";
    byte[] original = captureWireBytes(() -> alice.send(
        new TestGreeting(requestId, aliceKey.id(), bobKey.id(), "Hello Bob!"), "json"));
    byte[] changedBody = captureWireBytes(() -> alice.send(
        new TestGreeting(requestId, aliceKey.id(), bobKey.id(), "Hello again, Bob!"),
        "json"));
    byte[] changedFormat = captureWireBytes(() -> alice.send(
        new TestGreeting(requestId, aliceKey.id(), bobKey.id(), "Hello Bob!"),
        "persona-binary"));

    ReceivedMessage first = bob.receive(original);
    if (first.verified()) {
      handled.incrementAndGet();
    }
    ReceivedMessage retryBody = bob.receive(changedBody);
    ReceivedMessage retryFormat = bob.receive(changedFormat);

    assertThat(first.verified()).isTrue();
    assertThat(first.payload()).isPresent();

    for (ReceivedMessage retry : List.of(retryBody, retryFormat)) {
      assertThat(retry.verified()).isFalse();
      assertThat(retry.replayed()).isTrue();
      assertThat(retry.payload()).isEmpty();
      assertThat(retry.signers()).containsExactly(aliceKey.id());
      assertThat(retry.verification().reason()).hasValueSatisfying(
          reason -> assertThat(reason).contains("duplicate request id"));
    }
    assertThat(handled.get()).isEqualTo(1);
  }

  @Test
  void theSameRequestIdFromDisjointSignerSetsAreDifferentRequests() {
    // §5.2 scopes dedup to the requester: a disjoint quorum reusing a
    // request id is a different authorized request and must be processed.
    PersonaKey carolKey = world.mint();
    PersonaKey daveKey = world.mint();
    PersonaMessenger erin = world.messenger(world.mint());
    TestJointRequest aliceBob = new TestJointRequest(
        "shared-id", Set.of(aliceKey.id(), bobKey.id()), 2, "action A");
    TestJointRequest carolDave = new TestJointRequest(
        "shared-id", Set.of(carolKey.id(), daveKey.id()), 2, "action B");

    MultiSignedEnvelope first = world.signer().addSignature(
        world.signer().signMulti(TestJointRequest.MESSAGE_TYPE, aliceBob, aliceKey, "json"),
        bobKey);
    MultiSignedEnvelope second = world.signer().addSignature(
        world.signer().signMulti(TestJointRequest.MESSAGE_TYPE, carolDave, carolKey, "json"),
        daveKey);

    assertThat(erin.receive(first.encode(new JsonPayloadCodec())).verified()).isTrue();
    ReceivedMessage other = erin.receive(second.encode(new JsonPayloadCodec()));
    assertThat(other.verified()).isTrue();
    assertThat(other.payload()).contains(carolDave);
  }

  // -------------------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------------------

  /**
   * Runs {@code send} while capturing the raw bytes delivered to bob's
   * address off the wire (a raw transport listener, bypassing bob's
   * messenger, so deliveries can be replayed at will).
   */
  private byte[] captureWireBytes(Runnable send) {
    ConcurrentLinkedQueue<byte[]> wire = new ConcurrentLinkedQueue<>();
    AutoCloseable tap = world.transport().listen(bobKey.id().id(), wire::add);
    try {
      send.run();
    } finally {
      try {
        tap.close();
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    assertThat(wire).hasSize(1);
    return wire.peek();
  }

  /**
   * Flips one byte of the payload text ({@code "Hello"} → {@code "Xello"})
   * inside the envelope, keeping every structural property of the envelope
   * intact — so the rejection must come from the §7.3 step 7 signature
   * check, not from a decode failure.
   */
  private static byte[] tamperPayloadByte(SignedEnvelope envelope) {
    byte[] payload = envelope.payload();
    int hello = indexOf(payload, "Hello".getBytes(UTF_8));
    assertThat(hello).as("payload carries the greeting body").isGreaterThanOrEqualTo(0);
    payload[hello] = 'X';
    SignedEnvelope tampered = new SignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(), payload,
        envelope.signedBy(), envelope.signatures());
    return tampered.encode(new JsonPayloadCodec());
  }

  private static int indexOf(byte[] haystack, byte[] needle) {
    outer:
    for (int i = 0; i + needle.length <= haystack.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }
}
