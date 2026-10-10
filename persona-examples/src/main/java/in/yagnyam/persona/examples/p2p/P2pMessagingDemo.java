package in.yagnyam.persona.examples.p2p;

import static java.nio.charset.StandardCharsets.UTF_8;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.PersonaKey;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.CsrService;
import in.yagnyam.persona.crypto.Hash;
import in.yagnyam.persona.crypto.PemService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.envelope.EnvelopeSigner;
import in.yagnyam.persona.envelope.EnvelopeVerifier;
import in.yagnyam.persona.envelope.MessageBindings;
import in.yagnyam.persona.envelope.SignedEnvelope;
import in.yagnyam.persona.p2p.InMemoryTransport;
import in.yagnyam.persona.p2p.PersonaMessenger;
import in.yagnyam.persona.p2p.ReceivedMessage;
import in.yagnyam.persona.registrar.InProcessRegistrar;
import in.yagnyam.persona.registrar.RegisteredPersona;
import in.yagnyam.persona.resolver.InMemoryPersonaResolver;
import in.yagnyam.persona.resolver.PersonaResolver;
import in.yagnyam.persona.resolver.TrustValidator;
import in.yagnyam.persona.schema.InMemorySchemaRegistry;
import in.yagnyam.persona.schema.SchemaRegistry;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runnable persona-to-persona messaging demo (protocol §13 reference
 * architecture): two personas, alice and bob, registered with an in-process
 * registrar, exchange a signed domain message ({@code example.Greeting@v1},
 * protocol Appendix A.1) over an {@link InMemoryTransport} — bytes only at
 * the boundary — and then a byte flipped in transit is shown to be
 * rejected by bob's verifier.
 *
 * <p>Run it with:
 * <pre>{@code
 * ./mvnw -pl persona-examples -am package
 * java -jar persona-examples/target/persona-examples-0.1.0-SNAPSHOT.jar
 * }</pre>
 *
 * <p>No external network is involved; the in-memory transport models the
 * hostile network boundary by handing receivers bytes only.
 */
public final class P2pMessagingDemo {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  private P2pMessagingDemo() {}

  /**
   * Runs the demo, printing the outcome of every step.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    P2pMessagingDemo demo = new P2pMessagingDemo();
    demo.run();
  }

  private void run() {
    // ---- Step 1: the realm — registrar (CA), resolver, schema registry.
    step("Stand up the realm: in-process registrar, resolver, schema registry");
    InProcessRegistrar registrar = new InProcessRegistrar();
    PersonaResolver resolver = new InMemoryPersonaResolver(registrar);
    TrustValidator trust = new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);
    SchemaRegistry schemaRegistry = new InMemorySchemaRegistry();
    schemaRegistry.register(GreetingSchema.SCHEMA);
    MessageBindings bindings = MessageBindings.of(GreetingSchema.binding());
    System.out.println("  registered schema " + GreetingSchema.MESSAGE_TYPE
        + " (schemaId " + schemaRegistry.idOf(
            schemaRegistry.requireByMessageType(GreetingSchema.MESSAGE_TYPE)).value() + ")");

    // ---- Step 2: register alice and bob (protocol §7.1: id, key pair, CSR).
    PersonaKey aliceKey = register(registrar, "alice");
    PersonaKey bobKey = register(registrar, "bob");

    // ---- Step 3: messengers over one shared in-memory transport.
    step("Connect messengers over the in-memory transport (bytes only)");
    InMemoryTransport transport = new InMemoryTransport();
    PersonaMessenger alice = messenger(aliceKey, schemaRegistry, bindings, resolver, trust,
        transport);
    PersonaMessenger bob = messenger(bobKey, schemaRegistry, bindings, resolver, trust,
        transport);
    ConcurrentLinkedQueue<ReceivedMessage> bobInbox = new ConcurrentLinkedQueue<>();
    bob.listen(bobInbox::add);

    // ---- Step 4: alice sends bob a signed greeting (protocol §7.2).
    GreetingSchema.Greeting greeting = new GreetingSchema.Greeting(
        "greeting-" + UUID.randomUUID(), aliceKey.id(), bobKey.id(), "Hello Bob!",
        Instant.now().truncatedTo(ChronoUnit.MILLIS));
    step("alice -> bob: " + greeting.body());
    SignedEnvelope envelope = alice.send(greeting, "json");
    byte[] onTheWire = envelope.encode(new JsonPayloadCodec());
    System.out.println("  signed by " + envelope.signedBy().uniqueId()
        + ", envelope on the wire: " + onTheWire.length + " bytes (canonical JSON)");

    // ---- Step 5: bob verifies and materializes (protocol §7.3).
    step("bob receives, verifies (protocol §7.3), materializes");
    ReceivedMessage received = bobInbox.peek();
    System.out.println("  " + received);
    if (!received.verified() || received.payload().isEmpty()) {
      throw new IllegalStateException("demo failed: the genuine greeting was rejected");
    }
    System.out.println("  VERIFIED payload: " + received.payload().orElseThrow());

    // ---- Step 6: the negative path — a man-in-the-middle flips one
    // payload byte in transit; bob's verifier must reject.
    step("hostile network: flip one payload byte in transit");
    byte[] tampered = flipPayloadByte(onTheWire);
    ReceivedMessage rejected = bob.receive(tampered);
    System.out.println("  " + rejected);
    System.out.println("  rejection reason: "
        + rejected.verification().reason().orElse("<none>"));
    if (rejected.verified()) {
      throw new IllegalStateException("demo failed: a tampered envelope was accepted");
    }
    System.out.println("  REJECTED, no payload exposed (fail closed, protocol §4)");

    // ---- Step 7: replay protection — the transport attacker re-delivers
    // the genuine wire bytes verbatim; bob's messenger must dedup them
    // (protocol §5.2, §11 rule 5).
    step("hostile network: replay the genuine wire bytes verbatim");
    ReceivedMessage replay = bob.receive(onTheWire);
    System.out.println("  " + replay);
    if (!replay.replayed() || replay.payload().isPresent()) {
      throw new IllegalStateException("demo failed: a replayed envelope was not deduplicated");
    }
    System.out.println("  REPLAYED (duplicate request id), no payload exposed"
        + " — distinct from a signature rejection (protocol §5.2)");

    step("done");
  }

  /** Mints a persona through the registrar (protocol §7.1), printing the steps. */
  private static PersonaKey register(InProcessRegistrar registrar, String label) {
    step("Register persona '" + label + "' (protocol §7.1)");
    try {
      PersonaVersion version = PersonaVersion.latestVersion();
      String requestId = UUID.randomUUID().toString();
      String personaId = registrar.issuePersonaId(requestId);
      KeyPair keyPair =
          CRYPTO.generateKeyPair(version.keyGenerationAlgorithm(), version.keySize());
      Hash passPhraseHash =
          CRYPTO.getHash(version.preferredHashAlgorithm(), "demo-revocation-pass".getBytes(UTF_8));
      String csr = CsrService.instance()
          .buildCsr(personaId, keyPair, version.certificateSignatureAlgorithm());
      RegisteredPersona registered =
          registrar.registerPersona(requestId, personaId, passPhraseHash, csr);
      PersonaKey key =
          PersonaKey.of(PersonaId.of(registered.certificate()), keyPair, PemService.instance());
      System.out.println("  " + label + " = " + key.getUniqueId());
      return key;
    } catch (Exception e) {
      throw new IllegalStateException("unable to register persona '" + label + "'", e);
    }
  }

  /** A messenger for {@code key} with the realm's registry, resolver and trust. */
  private static PersonaMessenger messenger(
      PersonaKey key, SchemaRegistry schemaRegistry, MessageBindings bindings,
      PersonaResolver resolver, TrustValidator trust, InMemoryTransport transport) {
    PersonaVersion version = PersonaVersion.latestVersion();
    EnvelopeSigner signer = new EnvelopeSigner(CRYPTO, version, schemaRegistry, bindings);
    EnvelopeVerifier verifier = new EnvelopeVerifier(
        version, CRYPTO, schemaRegistry, bindings, resolver, trust);
    return PersonaMessenger.overJson(key, signer, verifier, schemaRegistry, bindings, transport);
  }

  /**
   * Flips one byte of the greeting body ({@code "Hello"} → {@code "Xello"})
   * inside the encoded envelope, leaving every structural property intact —
   * the tampering is invisible until the §7.3 step 7 signature check over
   * the exact payload bytes.
   */
  private static byte[] flipPayloadByte(byte[] encodedEnvelope) {
    SignedEnvelope decoded = SignedEnvelope.decode(encodedEnvelope, new JsonPayloadCodec());
    byte[] payload = decoded.payload();
    int body = indexOf(payload, "Hello".getBytes(UTF_8));
    if (body < 0) {
      throw new IllegalStateException("demo failed: the payload does not carry the greeting body");
    }
    payload[body] = 'X';
    System.out.println("  payload byte " + body + " flipped ('H' -> 'X'), signature untouched");
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

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title);
  }
}
