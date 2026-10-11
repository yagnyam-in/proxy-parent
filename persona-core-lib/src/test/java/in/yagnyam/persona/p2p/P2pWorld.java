package in.yagnyam.persona.p2p;

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
import in.yagnyam.persona.registrar.InProcessRegistrar;
import in.yagnyam.persona.registrar.RegisteredPersona;
import in.yagnyam.persona.resolver.InMemoryPersonaResolver;
import in.yagnyam.persona.resolver.TrustValidator;
import in.yagnyam.persona.schema.InMemorySchemaRegistry;
import java.security.KeyPair;
import java.time.Clock;
import java.util.UUID;

/**
 * Test world of the p2p tests: an in-process registrar (a real CA), a
 * schema registry with the p2p payload schemas, the bindings, and
 * signer/verifier/messenger factories over a shared transport.
 */
final class P2pWorld {

  /** The {@code example.Greeting@v1} schema (protocol §A.1). */
  static final String GREETING_SCHEMA = """
      {"type":"record","name":"Greeting","namespace":"example","messageType":"example.Greeting@v1",
       "fields":[{"name":"greetingId","type":"string"},
                 {"name":"from","type":"string"},
                 {"name":"to","type":"string"},
                 {"name":"body","type":"string"}]}""";

  /** The {@code example.Charter@v1} schema (protocol §A.3). */
  static final String CHARTER_SCHEMA = """
      {"type":"record","name":"Charter","namespace":"example","messageType":"example.Charter@v1",
       "fields":[{"name":"charterId","type":"string"},
                 {"name":"founders","type":{"type":"array","items":"string"}},
                 {"name":"minimumSignatures","type":"int"},
                 {"name":"terms","type":"string"}]}""";

  /** The {@code example.JointRequest@v1} schema (§5.5 + §5.2). */
  static final String JOINT_REQUEST_SCHEMA = """
      {"type":"record","name":"JointRequest","namespace":"example",
       "messageType":"example.JointRequest@v1",
       "fields":[{"name":"requestId","type":"string"},
                 {"name":"founders","type":{"type":"array","items":"string"}},
                 {"name":"minimumSignatures","type":"int"},
                 {"name":"action","type":"string"}]}""";

  /** The {@code example.MaintenanceAlert@v1} schema (protocol §5.4). */
  static final String ALERT_SCHEMA = """
      {"type":"record","name":"MaintenanceAlert","namespace":"example",
       "messageType":"example.MaintenanceAlert@v1",
       "fields":[{"name":"alertId","type":"string"},
                 {"name":"realm","type":"string"},
                 {"name":"from","type":"string"},
                 {"name":"receivers","type":{"type":"array","items":"string"}},
                 {"name":"body","type":"string"}]}""";

  /** The realm of every test alert. */
  static final String REALM = "test";

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  private final InProcessRegistrar registrar = new InProcessRegistrar();
  private final InMemorySchemaRegistry schemaRegistry = new InMemorySchemaRegistry();
  private final MessageBindings bindings = P2pTestMessages.bindings();
  private final InMemoryTransport transport = new InMemoryTransport();

  P2pWorld() {
    schemaRegistry.register(GREETING_SCHEMA);
    schemaRegistry.register(CHARTER_SCHEMA);
    schemaRegistry.register(JOINT_REQUEST_SCHEMA);
    schemaRegistry.register(ALERT_SCHEMA);
  }

  /** @return the registrar acting as the realm's CA */
  InProcessRegistrar registrar() {
    return registrar;
  }

  /** @return the shared in-memory transport */
  InMemoryTransport transport() {
    return transport;
  }

  /** Mints a fresh persona through the registrar (protocol §7.1). */
  PersonaKey mint() {
    try {
      PersonaVersion version = PersonaVersion.latestVersion();
      String requestId = UUID.randomUUID().toString();
      String personaId = registrar.issuePersonaId(requestId);
      KeyPair keyPair =
          CRYPTO.generateKeyPair(version.keyGenerationAlgorithm(), version.keySize());
      Hash passPhraseHash =
          CRYPTO.getHash(version.preferredHashAlgorithm(), "revocation-pass".getBytes(UTF_8));
      String csr = CsrService.instance()
          .buildCsr(personaId, keyPair, version.certificateSignatureAlgorithm());
      RegisteredPersona registered =
          registrar.registerPersona(requestId, personaId, passPhraseHash, csr);
      return PersonaKey.of(PersonaId.of(registered.certificate()), keyPair, PemService.instance());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to mint test persona", e);
    }
  }

  /** A signer bound to this world's registry and bindings. */
  EnvelopeSigner signer() {
    return new EnvelopeSigner(CRYPTO, PersonaVersion.latestVersion(), schemaRegistry, bindings);
  }

  /** A messenger for {@code key} over this world's transport, JSON-encoded. */
  PersonaMessenger messenger(PersonaKey key) {
    EnvelopeSigner signer = signer();
    EnvelopeVerifier verifier = new EnvelopeVerifier(
        PersonaVersion.latestVersion(), CRYPTO, schemaRegistry, bindings,
        new InMemoryPersonaResolver(registrar),
        new TrustValidator(registrar.trustedRoots(), registrar::isRevoked),
        Clock.systemUTC());
    return PersonaMessenger.overJson(key, signer, verifier, schemaRegistry, bindings, transport);
  }

  /** A messenger for {@code key} with an explicit request deduplicator. */
  PersonaMessenger messenger(PersonaKey key, RequestDeduplicator deduplicator) {
    EnvelopeSigner signer = signer();
    EnvelopeVerifier verifier = new EnvelopeVerifier(
        PersonaVersion.latestVersion(), CRYPTO, schemaRegistry, bindings,
        new InMemoryPersonaResolver(registrar),
        new TrustValidator(registrar.trustedRoots(), registrar::isRevoked),
        Clock.systemUTC());
    return PersonaMessenger.overJson(
        key, signer, verifier, schemaRegistry, bindings, transport, deduplicator);
  }
}
