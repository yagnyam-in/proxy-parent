package in.yagnyam.persona.p2p;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import in.yagnyam.persona.PersonaKey;
import in.yagnyam.persona.envelope.SignedEnvelope;
import in.yagnyam.persona.p2p.P2pTestMessages.TestGreeting;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;

/**
 * Headless end-to-end proof of persona-to-persona messaging (protocol
 * §13): alice sends bob a signed domain message over the
 * {@link InMemoryTransport} — bytes only at the boundary — bob verifies
 * and materializes it; then a byte flipped <em>in transit</em> (a hostile
 * network) is rejected by bob's verifier with a clear reason, and no
 * payload is exposed; and a verbatim transport replay of the genuine wire
 * bytes reaches bob's handler as a payload exactly once, the replay
 * surfacing as a distinct {@code replayed} outcome (protocol §5.2).
 */
class PersonaMessagingIT {

  private final P2pWorld world = new P2pWorld();
  private final PersonaKey aliceKey = world.mint();
  private final PersonaKey bobKey = world.mint();
  private final PersonaMessenger alice = world.messenger(aliceKey);
  private final PersonaMessenger bob = world.messenger(bobKey);

  @Test
  void aliceToBobRoundTripAndInTransitTamperRejection() throws Exception {
    TestGreeting greeting =
        new TestGreeting("greeting-1", aliceKey.id(), bobKey.id(), "Hello Bob!");
    ConcurrentLinkedQueue<ReceivedMessage> bobInbox = new ConcurrentLinkedQueue<>();

    // -- happy path: alice's signed greeting reaches bob over the transport.
    ReceivedMessage received;
    try (AutoCloseable ignored = bob.listen(bobInbox::add)) {
      alice.send(greeting, "json");

      assertThat(bobInbox).hasSize(1);
      received = bobInbox.poll();
    }
    assertThat(received.verified()).isTrue();
    assertThat(received.messageType()).isEqualTo(TestGreeting.MESSAGE_TYPE);
    assertThat(received.signers()).containsExactly(aliceKey.id());
    assertThat(received.payload()).contains(greeting);

    // -- hostile network: a man-in-the-middle flips one payload byte of
    // the very same envelope in transit.
    SignedEnvelope envelope = world.signer().sign(greeting, aliceKey, "json");
    byte[] inTransit = envelope.encode(new JsonPayloadCodec());
    byte[] tampered = flipPayloadByteInTransit(inTransit);
    assertThat(tampered).isNotEqualTo(inTransit);

    // Bob's messenger, registered on the transport under its own address,
    // receives only the tampered bytes.
    bob.listen(m -> {
      assertThat(m.verified()).isFalse();
      assertThat(m.payload()).isEmpty();
      assertThat(m.verification().reason()).hasValueSatisfying(
          reason -> assertThat(reason).contains("does not verify"));
    });
    // Deliver the tampered bytes as the network would: to bob's address.
    world.transport().send(bobKey.id().id(), tampered);
  }

  @Test
  void aTransportReplayOfTheWireBytesReachesBobsHandlerExactlyOnce() throws Exception {
    TestGreeting greeting =
        new TestGreeting("greeting-replay", aliceKey.id(), bobKey.id(), "Hello Bob!");
    ConcurrentLinkedQueue<ReceivedMessage> bobInbox = new ConcurrentLinkedQueue<>();

    try (AutoCloseable ignored = bob.listen(bobInbox::add)) {
      SignedEnvelope sent = alice.send(greeting, "json");
      assertThat(bobInbox).hasSize(1);

      // The transport attacker captures the genuine wire bytes and
      // re-delivers them verbatim to bob's address.
      byte[] captured = sent.encode(new JsonPayloadCodec());
      world.transport().send(bobKey.id().id(), captured);

      // Bob's handler fired for both deliveries, but the replay never
      // reached him as a payload: exactly one verified, one replayed.
      assertThat(bobInbox).hasSize(2);
      assertThat(bobInbox.stream().filter(ReceivedMessage::verified).count()).isEqualTo(1);
      assertThat(bobInbox.stream().filter(ReceivedMessage::replayed).count()).isEqualTo(1);
      assertThat(bobInbox.stream().filter(m -> m.payload().isPresent()).count()).isEqualTo(1);
      ReceivedMessage replay = bobInbox.stream()
          .filter(ReceivedMessage::replayed).findFirst().orElseThrow();
      assertThat(replay.signers()).containsExactly(aliceKey.id());
      assertThat(replay.verification().reason()).hasValueSatisfying(
          reason -> assertThat(reason).contains("duplicate request id"));
    }
  }

  /**
   * Flips one payload byte of an encoded envelope ({@code "Hello"} →
   * {@code "Xello"} inside the payload text) without touching any other
   * field — the tampering is invisible until the §7.3 step 7 signature
   * check over the exact payload bytes.
   */
  private static byte[] flipPayloadByteInTransit(byte[] encodedEnvelope) {
    SignedEnvelope decoded = SignedEnvelope.decode(encodedEnvelope, new JsonPayloadCodec());
    byte[] payload = decoded.payload();
    int body = indexOf(payload, "Hello".getBytes(UTF_8));
    assertThat(body).as("payload carries the greeting body").isGreaterThanOrEqualTo(0);
    payload[body] = 'X';
    return new SignedEnvelope(
        decoded.messageType(), decoded.payloadFormat(), decoded.schemaId(), payload,
        decoded.signedBy(), decoded.signatures()).encode(new JsonPayloadCodec());
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
