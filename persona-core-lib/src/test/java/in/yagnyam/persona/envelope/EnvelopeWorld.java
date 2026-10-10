package in.yagnyam.persona.envelope;

import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.registrar.InProcessRegistrar;
import in.yagnyam.persona.resolver.InMemoryPersonaResolver;
import in.yagnyam.persona.resolver.PersonaResolver;
import in.yagnyam.persona.resolver.TrustValidator;
import in.yagnyam.persona.schema.InMemorySchemaRegistry;
import in.yagnyam.persona.schema.SchemaRegistry;
import java.time.Clock;
import java.time.Duration;

/**
 * Test world: an in-process registrar (a real CA), a schema registry with
 * the test payload schemas, the message bindings, a signer and verifier
 * factories — everything an end-to-end envelope test needs, with the
 * registrar/trust pieces overridable for the trust-failure tests.
 */
final class EnvelopeWorld {

  /** The {@code example.Greeting@v1} schema (protocol §A.1). */
  static final String GREETING_SCHEMA = """
      {"type":"record","name":"Greeting","namespace":"example","messageType":"example.Greeting@v1",
       "fields":[{"name":"greetingId","type":"string"},
                 {"name":"from","type":"string"},
                 {"name":"to","type":"string"},
                 {"name":"body","type":"string"}]}""";

  /**
   * An isomorphic copy of the greeting schema under a second message type:
   * same fields, different identity — signing as one and relabeling to the
   * other must fail on the §6.4 preimage, not on the schema.
   */
  static final String GREETING2_SCHEMA = """
      {"type":"record","name":"Greeting2","namespace":"example","messageType":"example.Greeting2@v1",
       "fields":[{"name":"greetingId","type":"string"},
                 {"name":"from","type":"string"},
                 {"name":"to","type":"string"},
                 {"name":"body","type":"string"}]}""";

  /**
   * A greeting-shaped schema with no {@code messageType} declaration: a
   * different canonical form (hence a different schemaId) with an
   * isomorphic field structure — for the schemaId-relabel test.
   */
  static final String GREETING_ISO_SCHEMA = """
      {"type":"record","name":"GreetingIso","namespace":"example",
       "fields":[{"name":"greetingId","type":"string"},
                 {"name":"from","type":"string"},
                 {"name":"to","type":"string"},
                 {"name":"body","type":"string"}]}""";

  /** The {@code example.PaymentAuthorization@v1} schema (protocol §6.3). */
  static final String PAYMENT_AUTHORIZATION_SCHEMA = """
      {"type":"record","name":"PaymentAuthorization","namespace":"example",
       "messageType":"example.PaymentAuthorization@v1",
       "fields":[{"name":"authorizationId","type":"string"},
                 {"name":"payer","type":"string"},
                 {"name":"payerAccount","type":"envelope"},
                 {"name":"amount","type":"string"},
                 {"name":"approvals","type":{"type":"array","items":"envelope"}}]}""";

  /** The {@code example.Charter@v1} schema (protocol §A.3). */
  static final String CHARTER_SCHEMA = """
      {"type":"record","name":"Charter","namespace":"example","messageType":"example.Charter@v1",
       "fields":[{"name":"charterId","type":"string"},
                 {"name":"founders","type":{"type":"array","items":"string"}},
                 {"name":"minimumSignatures","type":"int"},
                 {"name":"terms","type":"string"}]}""";

  /**
   * The {@code example.RawCountingCharter@v1} schema: a charter whose
   * payload rule counts the raw verified signer set — see
   * {@code TestMessages.TestRawCountingCharter}.
   */
  static final String RAW_COUNTING_CHARTER_SCHEMA = """
      {"type":"record","name":"RawCountingCharter","namespace":"example",
       "messageType":"example.RawCountingCharter@v1",
       "fields":[{"name":"charterId","type":"string"},
                 {"name":"founders","type":{"type":"array","items":"string"}},
                 {"name":"minimumSignatures","type":"int"},
                 {"name":"terms","type":"string"}]}""";

  /** The {@code example.EscrowInstruction@v1} schema. */
  static final String ESCROW_INSTRUCTION_SCHEMA = """
      {"type":"record","name":"EscrowInstruction","namespace":"example",
       "messageType":"example.EscrowInstruction@v1",
       "fields":[{"name":"instructionId","type":"string"},
                 {"name":"partyA","type":"string"},
                 {"name":"partyB","type":"string"},
                 {"name":"amount","type":"string"}]}""";

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  private final InProcessRegistrar registrar;
  private final InMemorySchemaRegistry schemaRegistry;
  private final MessageBindings bindings;
  private final EnvelopeSigner signer;

  EnvelopeWorld() {
    this(new InProcessRegistrar());
  }

  /**
   * @param personaCertificateValidity validity of issued certificates — the
   *                                   expiry tests mint registrars with a
   *                                   near-immediate validity window
   */
  EnvelopeWorld(Duration personaCertificateValidity) {
    this(new InProcessRegistrar(personaCertificateValidity));
  }

  EnvelopeWorld(InProcessRegistrar registrar) {
    this.registrar = registrar;
    this.schemaRegistry = new InMemorySchemaRegistry();
    registerTestSchemas(schemaRegistry);
    this.bindings = TestMessages.bindings();
    this.signer = new EnvelopeSigner(
        CRYPTO, PersonaVersion.latestVersion(), schemaRegistry, bindings);
  }

  private static void registerTestSchemas(SchemaRegistry registry) {
    registry.register(GREETING_SCHEMA);
    registry.register(GREETING2_SCHEMA);
    registry.register(GREETING_ISO_SCHEMA);
    registry.register(PAYMENT_AUTHORIZATION_SCHEMA);
    registry.register(CHARTER_SCHEMA);
    registry.register(RAW_COUNTING_CHARTER_SCHEMA);
    registry.register(ESCROW_INSTRUCTION_SCHEMA);
  }

  /** @return the registrar acting as the realm's CA */
  InProcessRegistrar registrar() {
    return registrar;
  }

  /** @return the schema registry with the test payload schemas */
  InMemorySchemaRegistry schemaRegistry() {
    return schemaRegistry;
  }

  /** @return the message bindings of the test payload types */
  MessageBindings bindings() {
    return bindings;
  }

  /** @return the signer bound to this world's registry and bindings */
  EnvelopeSigner signer() {
    return signer;
  }

  /** Mints a fresh persona through the registrar (protocol §7.1). */
  PersonaFixture mint() {
    return PersonaFixture.mint(registrar);
  }

  /**
   * A verifier that resolves personas through this world's registrar and
   * trusts its roots, at the system clock.
   */
  EnvelopeVerifier verifier() {
    return verifier(Clock.systemUTC());
  }

  /** A verifier of this world at {@code clock}. */
  EnvelopeVerifier verifier(Clock clock) {
    return verifier(new InMemoryPersonaResolver(registrar), trust(), clock);
  }

  /** A verifier with custom resolution and trust (wrong-root, blind-resolver tests). */
  EnvelopeVerifier verifier(PersonaResolver resolver, TrustValidator trustValidator, Clock clock) {
    return new EnvelopeVerifier(
        PersonaVersion.latestVersion(), CRYPTO, schemaRegistry, bindings, resolver,
        trustValidator, clock);
  }

  /** A verifier of this world with a different schema registry. */
  EnvelopeVerifier verifierWithSchemaRegistry(SchemaRegistry registry) {
    return new EnvelopeVerifier(
        PersonaVersion.latestVersion(), CRYPTO, registry, bindings,
        new InMemoryPersonaResolver(registrar), trust(), Clock.systemUTC());
  }

  /** The trust validator over this world's registrar roots and revocation status. */
  TrustValidator trust() {
    return new TrustValidator(registrar.trustedRoots(), registrar::isRevoked);
  }

  /**
   * The schemaId of the schema registered for {@code messageType}.
   *
   * @param messageType message type
   * @return its 40-hex digest
   */
  String schemaIdOf(String messageType) {
    return schemaRegistry.idOf(schemaRegistry.requireByMessageType(messageType)).value();
  }
}
