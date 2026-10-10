package in.yagnyam.persona.p2p;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.PersonaKey;
import in.yagnyam.persona.envelope.MultiSignedEnvelope;
import in.yagnyam.persona.envelope.SignedEnvelope;
import in.yagnyam.persona.p2p.P2pTestMessages.TestAlert;
import in.yagnyam.persona.p2p.P2pTestMessages.TestCharter;
import in.yagnyam.persona.p2p.P2pTestMessages.TestGreeting;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
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
  // Helpers
  // -------------------------------------------------------------------------------------

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
