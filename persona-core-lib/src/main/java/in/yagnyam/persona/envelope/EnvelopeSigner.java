package in.yagnyam.persona.envelope;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.PersonaKey;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.message.MultiSignableMessage;
import in.yagnyam.persona.message.SignableMessage;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.SchemaId;
import in.yagnyam.persona.schema.SchemaRegistry;
import in.yagnyam.persona.schema.codec.PayloadCodec;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Envelope signing per protocol §7.2 (and §7.4 for multi-signatures).
 *
 * <p>§7.2, in order, for a payload {@code P} and persona key {@code K}:
 * <ol>
 *   <li>assert {@code P.isValid()} and that {@code K.id} is authorized to
 *       sign {@code P} (the §5.1 predicate over {@code P.validSigners()});</li>
 *   <li>serialize {@code P} <em>once</em> with the chosen payload format —
 *       the returned envelope carries those exact bytes and nothing ever
 *       re-serializes them (protocol §6.1, §11 rule 10);</li>
 *   <li>compute the §6.4 signing preimage from
 *       {@code {messageType, payloadFormat, schemaId, payloadBytes}};</li>
 *   <li>sign the preimage with each algorithm of the active version's
 *       {@link PersonaVersion#preferredSignatureAlgorithmSet()};</li>
 *   <li>emit the envelope.</li>
 * </ol>
 *
 * <p>The {@code schemaId} emitted is the digest of the payload schema
 * resolved from the message type — the payload codecs of
 * {@code docs/SCHEMA.md} are schema-driven, so a payload without its schema
 * digest is not decodable by peers; the wire field stays optional for other
 * encodings (protocol §6.1).
 *
 * <p>Multi-signature flows (protocol §7.4): initial signing produces a
 * {@link MultiSignedEnvelope} with one entry
 * ({@link #signMulti(String, MultiSignableMessage, PersonaKey, String)});
 * additional signers extend the signature array
 * ({@link #addSignature(MultiSignedEnvelope, PersonaKey)}) without
 * disturbing existing entries. §5.5 defines no {@code messageType()} on a
 * multi-signable payload, so the wire message type of the initial signing
 * is supplied at the envelope layer, where protocol §6.2 requires it
 * anyway; afterwards it travels in the envelope.
 *
 * <p>Sign-time problems (invalid payload, unauthorized signer, unknown
 * format/schema/binding) are input errors of the signer and surface as
 * {@code IllegalArgumentException}; verification outcomes, by contrast, are
 * values ({@link VerificationResult}).
 */
public final class EnvelopeSigner {

  private final CryptographyService cryptographyService;
  private final PersonaVersion version;
  private final SchemaRegistry schemaRegistry;
  private final MessageBindings bindings;

  /**
   * Creates a signer.
   *
   * @param cryptographyService crypto primitives
   * @param version             active persona version — the source of the
   *                            preferred signature algorithm set and the
   *                            only place algorithm names live
   * @param schemaRegistry      payload schema registry
   * @param bindings            message bindings (payload value ⇄ domain
   *                            message)
   */
  public EnvelopeSigner(
      CryptographyService cryptographyService,
      PersonaVersion version,
      SchemaRegistry schemaRegistry,
      MessageBindings bindings) {
    this.cryptographyService = Objects.requireNonNull(cryptographyService, "cryptographyService");
    this.version = Objects.requireNonNull(version, "version");
    this.schemaRegistry = Objects.requireNonNull(schemaRegistry, "schemaRegistry");
    this.bindings = Objects.requireNonNull(bindings, "bindings");
  }

  /**
   * Signs {@code message} as {@code key} into a {@link SignedEnvelope}
   * (protocol §7.2).
   *
   * @param message       the payload to sign
   * @param key           the signer's persona key
   * @param payloadFormat payload format identifier (see
   *                      {@link PayloadFormats#require(String)})
   * @return the signed envelope
   * @throws NullPointerException if any argument is {@code null}
   * @throws IllegalArgumentException if the payload is not valid, the key
   *     is not authorized to sign it, or the message type has no
   *     schema/binding or the format is unknown
   */
  public SignedEnvelope sign(SignableMessage message, PersonaKey key, String payloadFormat) {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(payloadFormat, "payloadFormat");
    String messageType = message.messageType();
    requireValidPayload(messageType, message);
    if (!message.canBeSignedBy(key.id())) {
      throw new IllegalArgumentException(
          "persona '" + key.id() + "' is not authorized to sign messageType '" + messageType
              + "' (protocol §7.2 step 1: the signer must be able to sign on behalf of some"
              + " valid signer of the payload; valid signers are " + message.validSigners() + ")");
    }
    Prepared prepared = prepare(messageType, message, payloadFormat);
    byte[] preimage = SigningPreimage.forSignedEnvelope(
        prepared.messageType(), prepared.payloadFormat(), prepared.schemaId(), prepared.payload());
    return new SignedEnvelope(
        prepared.messageType(),
        prepared.payloadFormat(),
        prepared.schemaId(),
        prepared.payload(),
        key.id(),
        sign(preimage, key));
  }

  /**
   * Signs {@code message} as {@code key} into a {@link MultiSignedEnvelope}
   * with one entry (protocol §7.4: "Initial signing produces a
   * MultiSignedEnvelope with one entry").
   *
   * @param messageType   the wire message type of the payload — resolved
   *                      against the schema registry and carried in the
   *                      envelope (protocol §6.2); §5.5 deliberately defines
   *                      no {@code messageType()} on the payload itself
   * @param message       the multi-signable payload to sign
   * @param key           the signer's persona key
   * @param payloadFormat payload format identifier
   * @return the multi-signed envelope with the one entry
   * @throws IllegalArgumentException if the payload is not valid, the key
   *     is not authorized to co-sign it, or the message type has no
   *     schema/binding or the format is unknown
   */
  public MultiSignedEnvelope signMulti(
      String messageType, MultiSignableMessage message, PersonaKey key, String payloadFormat) {
    Objects.requireNonNull(messageType, "messageType");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(payloadFormat, "payloadFormat");
    requireValidPayload(messageType, message);
    requireAuthorized(messageType, key.id(), message.validSigners());
    Prepared prepared = prepare(messageType, message, payloadFormat);
    byte[] preimage = SigningPreimage.forMultiSignedEnvelope(
        prepared.messageType(), prepared.payloadFormat(), prepared.schemaId(), prepared.payload());
    return new MultiSignedEnvelope(
        prepared.messageType(),
        prepared.payloadFormat(),
        prepared.schemaId(),
        prepared.payload(),
        List.of(new MultiSignature(key.id(), sign(preimage, key))));
  }

  /**
   * Adds {@code key}'s signature to an existing multi-signed envelope
   * (protocol §7.4: "Additional signers extend the signatures array";
   * protocol §5.5: incremental signing yields a new envelope that
   * supersedes the previous one).
   *
   * <p>The existing entries are carried over untouched and the new entry is
   * computed over the §6.4 preimage built from the envelope's own fields —
   * the payload bytes are reused exactly as they are, never re-serialized
   * (protocol §6.1, §11 rule 10). The payload is decoded (read-only) to
   * assert that it is still valid and that the new signer is authorized
   * (protocol §7.2 step 1, §11 rule 4). Existing entries are NOT verified
   * here — verification is the recipient's job (protocol §7.3); co-signing
   * is a cooperative act on an envelope the caller trusts to be authentic.
   *
   * @param envelope the envelope to extend
   * @param key      the additional signer's persona key
   * @return a new envelope with one additional entry
   * @throws IllegalArgumentException if the envelope is not well-formed, its
   *     payload cannot be decoded/materialized, the payload is not valid,
   *     or the key is not authorized to co-sign it
   */
  public MultiSignedEnvelope addSignature(MultiSignedEnvelope envelope, PersonaKey key) {
    Objects.requireNonNull(envelope, "envelope");
    Objects.requireNonNull(key, "key");
    if (!envelope.isValid()) {
      throw new IllegalArgumentException(
          "cannot co-sign an envelope that is not well-formed (protocol §7.3 step 1): "
              + envelope);
    }
    MultiSignableMessage message = multiPayloadOf(envelope);
    requireAuthorized(envelope.messageType(), key.id(), message.validSigners());
    byte[] preimage = SigningPreimage.forMultiSignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
        envelope.payload());
    List<MultiSignature> extended = new ArrayList<>(envelope.signatures());
    extended.add(new MultiSignature(key.id(), sign(preimage, key)));
    return new MultiSignedEnvelope(
        envelope.messageType(),
        envelope.payloadFormat(),
        envelope.schemaId(),
        envelope.payload(),
        extended);
  }

  /** Asserts §7.2 step 1's validity half (§11 rule 4: checked before signing). */
  private static void requireValidPayload(String messageType, Validatable message) {
    if (!message.isValid()) {
      throw new IllegalArgumentException(
          "payload of messageType '" + messageType + "' is not valid (protocol §7.2 step 1,"
              + " §11 rule 4: isValid() must hold before signing)");
    }
  }

  /**
   * Asserts §7.2 step 1's authorization half over a §5.5 signer set (the
   * §5.1 predicate; protocol §7.3 step 5 asserts the same per entry).
   */
  private static void requireAuthorized(
      String messageType, PersonaId candidate, Set<PersonaId> validSigners) {
    if (validSigners != null) {
      for (PersonaId allowed : validSigners) {
        if (candidate.canSignOnBehalfOf(allowed)) {
          return;
        }
      }
    }
    throw new IllegalArgumentException(
        "persona '" + candidate + "' is not authorized to co-sign messageType '" + messageType
            + "' (protocol §7.2 step 1: the signer must be able to sign on behalf of some"
            + " valid signer of the payload; valid signers are " + validSigners + ")");
  }

  /**
   * Serializes {@code message} exactly once (§7.2 step 2) and resolves
   * everything the envelope must carry.
   */
  private Prepared prepare(String messageType, Validatable message, String payloadFormat) {
    MessageBinding binding = bindings.require(messageType);
    PersonaSchema schema = schemaRegistry.requireByMessageType(messageType);
    PayloadCodec codec = PayloadFormats.require(payloadFormat);
    Map<String, Object> payloadValue;
    try {
      payloadValue = binding.wireValue(message);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          "cannot serialize the payload of messageType '" + messageType + "': " + e.getMessage(), e);
    }
    byte[] payload = codec.encode(schema, payloadValue);
    return new Prepared(
        messageType,
        codec.format(),
        schemaRegistry.idOf(schema).value(),
        payload);
  }

  /**
   * Decodes the payload of {@code envelope} into its domain message for the
   * sign-time assertions of {@link #addSignature(MultiSignedEnvelope, PersonaKey)}.
   */
  private MultiSignableMessage multiPayloadOf(MultiSignedEnvelope envelope) {
    PersonaSchema schema = coSignSchemaOf(envelope);
    Map<String, Object> payloadValue;
    try {
      payloadValue =
          PayloadFormats.require(envelope.payloadFormat()).decode(schema, envelope.payload());
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          "cannot co-sign: the payload of messageType '" + envelope.messageType()
              + "' cannot be decoded as '" + envelope.payloadFormat() + "': " + e.getMessage(), e);
    }
    MessageBinding binding = bindings.require(envelope.messageType());
    Validatable message;
    try {
      message = binding.bind(payloadValue);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          "cannot co-sign: the payload of messageType '" + envelope.messageType()
              + "' cannot be materialized: " + e.getMessage(), e);
    }
    if (!(message instanceof MultiSignableMessage multiSignable)) {
      throw new IllegalArgumentException(
          "cannot co-sign: the payload of messageType '" + envelope.messageType()
              + "' does not implement MultiSignableMessage (protocol §5.5) and cannot travel"
              + " in a MultiSignedEnvelope");
    }
    if (!multiSignable.isValid()) {
      throw new IllegalArgumentException(
          "cannot co-sign: the payload of messageType '" + envelope.messageType()
              + "' is not valid (protocol §11 rule 4)");
    }
    return multiSignable;
  }

  /**
   * Resolves the payload schema of an envelope being co-signed, mirroring
   * the verifier's rule (§7.3 step 2, {@code docs/SCHEMA.md} §6 rule 4):
   * via the {@code schemaId} digest when present — cross-checked against
   * the {@code messageType} registration when both resolve, with no silent
   * precedence — and via the {@code messageType} when the digest is
   * absent. An incoming envelope may identify its schema by digest alone
   * (schemas may omit {@code messageType}), so a message-type-only lookup
   * would make such envelopes verifiable but not co-signable.
   */
  private PersonaSchema coSignSchemaOf(MultiSignedEnvelope envelope) {
    String messageType = envelope.messageType();
    String schemaId = envelope.schemaId();
    if (schemaId.isEmpty()) {
      return schemaRegistry.requireByMessageType(messageType);
    }
    // The envelope's canonical constructor already enforces the schemaId grammar.
    SchemaId id = SchemaId.of(schemaId);
    PersonaSchema schema;
    try {
      schema = schemaRegistry.requireById(id);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "cannot co-sign: no schema registered with schemaId '" + schemaId
              + "' (protocol §7.3 step 2 applies to co-signing too: the payload must decode"
              + " under the schema the envelope names)", e);
    }
    Optional<PersonaSchema> byMessageType = schemaRegistry.byMessageType(messageType);
    if (byMessageType.isPresent() && !byMessageType.get().equals(schema)) {
      throw new IllegalArgumentException(
          "cannot co-sign: schemaId '" + schemaId + "' does not match the schema registered for"
              + " messageType '" + messageType + "' (docs/SCHEMA.md §6 rule 4: the digest and"
              + " the message-type entry must agree — neither takes silent precedence)");
    }
    return schema;
  }

  /** §7.2 step 4: sign the preimage with every algorithm of the preferred set. */
  private List<Signature> sign(byte[] preimage, PersonaKey key) {
    PrivateKey privateKey = key.privateKey();
    if (privateKey == null) {
      throw new IllegalArgumentException(
          "persona key of '" + key.id() + "' carries no decoded private key handle"
              + " (PersonaKey.setPrivateKey must be called after loading or decrypting the key"
              + " material)");
    }
    Set<String> algorithms = version.preferredSignatureAlgorithmSet();
    List<Signature> signatures = new ArrayList<>(algorithms.size());
    try {
      for (String algorithm : algorithms) {
        signatures.add(new Signature(
            algorithm, cryptographyService.getSignature(algorithm, privateKey, preimage)));
      }
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          "unable to sign with the preferred algorithm set " + algorithms
              + " of persona version " + version.version() + " (protocol §7.2 step 4)", e);
    }
    return signatures;
  }

  /** The fields an envelope carries, prepared by §7.2 step 2. */
  private record Prepared(String messageType, String payloadFormat, String schemaId,
      byte[] payload) {}
}
