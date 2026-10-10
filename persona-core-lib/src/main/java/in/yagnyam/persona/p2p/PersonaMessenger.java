package in.yagnyam.persona.p2p;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.PersonaKey;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.envelope.EnvelopeSigner;
import in.yagnyam.persona.envelope.EnvelopeVerifier;
import in.yagnyam.persona.envelope.MessageBindings;
import in.yagnyam.persona.envelope.MultiSignature;
import in.yagnyam.persona.envelope.MultiSignedEnvelope;
import in.yagnyam.persona.envelope.PayloadFormats;
import in.yagnyam.persona.envelope.SignedEnvelope;
import in.yagnyam.persona.envelope.VerificationResult;
import in.yagnyam.persona.message.AddressableMessage;
import in.yagnyam.persona.message.AlertMessage;
import in.yagnyam.persona.message.MultiSignableMessage;
import in.yagnyam.persona.message.SignableMessage;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.SchemaId;
import in.yagnyam.persona.schema.SchemaRegistry;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import in.yagnyam.persona.schema.codec.PayloadCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The persona-level messenger of the protocol §13 reference architecture:
 * ties a persona's key, the envelope signer/verifier, the schema registry
 * and bindings, and a {@link PersonaTransport} together into
 * send/receive of domain messages between personas.
 *
 * <p>Send path (protocol §7.2): the payload is serialized <em>once</em> by
 * the {@link EnvelopeSigner} via the schema codec, signed, and the envelope
 * is encoded with the transport codec; only those bytes cross the
 * transport. Receive path (protocol §7.3): bytes are decoded into an
 * envelope, the full {@link EnvelopeVerifier} runs (including recursive
 * nested-envelope verification and revocation at verification time), and
 * only a <em>verified</em> envelope's payload is materialized through its
 * {@link in.yagnyam.persona.envelope.MessageBinding} and returned —
 * verification failures are {@link ReceivedMessage} values, never
 * exceptions (fail closed, protocol §4).
 *
 * <p>Receive-side decode order: the wire carries no envelope-kind tag, so
 * bytes are first decoded as a {@link SignedEnvelope} and, when that fails
 * structurally, as a {@link MultiSignedEnvelope} (protocol §6.1/§6.2 — the
 * two shapes are incompatible at the {@code signatures} field, so exactly
 * one can decode). Bytes that decode as neither are rejected as
 * "not an envelope".
 *
 * <p>Multi-signature (protocol §7.4) and alerts (protocol §5.4) are
 * pass-throughs, not new machinery: {@link #sendMulti} and {@link #coSign}
 * delegate to the signer; {@link #sendAlert} signs once and delivers the
 * same bytes to every receiver of the alert — the alert's
 * {@code transportMetadata()} ({realm, alertType, alertId}) is the
 * projection a real push substrate would consume (protocol §7.6 delivery
 * substrates are out of scope for this library; see {@code GAPS.md}).
 *
 * <p>Thread-safety: all members are safe for concurrent use (the
 * transport, signer and verifier are thread-safe; the messenger holds no
 * mutable state of its own). Send-time problems (invalid payload,
 * unauthorized signer, undeliverable address) surface as exceptions of the
 * signer or the transport; receive-side problems are values.
 */
public final class PersonaMessenger {

  private final PersonaKey key;
  private final EnvelopeSigner signer;
  private final EnvelopeVerifier verifier;
  private final SchemaRegistry schemaRegistry;
  private final MessageBindings bindings;
  private final PayloadCodec transportCodec;
  private final PersonaTransport transport;

  /**
   * Creates a messenger for one persona.
   *
   * @param key            the persona's key (the identity this messenger
   *                       sends as; its bare id is the address
   *                       {@link #listen} registers)
   * @param signer         envelope signer (protocol §7.2)
   * @param verifier       envelope verifier (protocol §7.3)
   * @param schemaRegistry payload schema registry (for payload
   *                       materialization of verified envelopes)
   * @param bindings       message bindings (for payload materialization of
   *                       verified envelopes)
   * @param transportCodec transport encoding of the envelope itself —
   *                       canonical JSON ({@code new JsonPayloadCodec()})
   *                       is the recommended form ({@code docs/SCHEMA.md}
   *                       §5); distinct from the payload format carried
   *                       inside the envelope
   * @param transport      the transport to send over and listen on
   */
  public PersonaMessenger(
      PersonaKey key,
      EnvelopeSigner signer,
      EnvelopeVerifier verifier,
      SchemaRegistry schemaRegistry,
      MessageBindings bindings,
      PayloadCodec transportCodec,
      PersonaTransport transport) {
    this.key = Objects.requireNonNull(key, "key");
    this.signer = Objects.requireNonNull(signer, "signer");
    this.verifier = Objects.requireNonNull(verifier, "verifier");
    this.schemaRegistry = Objects.requireNonNull(schemaRegistry, "schemaRegistry");
    this.bindings = Objects.requireNonNull(bindings, "bindings");
    this.transportCodec = Objects.requireNonNull(transportCodec, "transportCodec");
    this.transport = Objects.requireNonNull(transport, "transport");
  }

  /**
   * Creates a messenger with the recommended canonical-JSON transport
   * encoding ({@code docs/SCHEMA.md} §5).
   *
   * @param key            the persona's key
   * @param signer         envelope signer
   * @param verifier       envelope verifier
   * @param schemaRegistry payload schema registry
   * @param bindings       message bindings
   * @param transport      the transport
   * @return the messenger
   */
  public static PersonaMessenger overJson(
      PersonaKey key,
      EnvelopeSigner signer,
      EnvelopeVerifier verifier,
      SchemaRegistry schemaRegistry,
      MessageBindings bindings,
      PersonaTransport transport) {
    return new PersonaMessenger(
        key, signer, verifier, schemaRegistry, bindings, new JsonPayloadCodec(), transport);
  }

  /**
   * @return the persona id this messenger sends as and listens for
   */
  public PersonaId personaId() {
    return key.id();
  }

  // -------------------------------------------------------------------------------------
  // Send path (protocol §7.2, §7.4)
  // -------------------------------------------------------------------------------------

  /**
   * Signs {@code message} and delivers the encoded envelope to
   * {@code recipient} (protocol §7.2 over §13's transport).
   *
   * @param message       the payload to send
   * @param recipient     the recipient persona (addressed by its bare id)
   * @param payloadFormat payload format identifier (see
   *                      {@link PayloadFormats#supportedFormats()})
   * @return the signed envelope that was sent
   * @throws IllegalArgumentException if the payload is not valid or this
   *     persona is not authorized to sign it (protocol §7.2 step 1)
   * @throws DeliveryException if the transport cannot deliver
   */
  public SignedEnvelope send(SignableMessage message, PersonaId recipient, String payloadFormat) {
    Objects.requireNonNull(recipient, "recipient");
    SignedEnvelope envelope = signer.sign(message, key, payloadFormat);
    transport.send(recipient.id(), envelope.encode(transportCodec));
    return envelope;
  }

  /**
   * Sends an addressed payload to its declared recipient (protocol §5.3).
   *
   * @param message       a payload implementing {@link AddressableMessage}
   * @param payloadFormat payload format identifier
   * @return the signed envelope that was sent
   * @throws IllegalArgumentException if {@code message} does not implement
   *     {@link AddressableMessage}
   */
  public SignedEnvelope send(SignableMessage message, String payloadFormat) {
    Objects.requireNonNull(message, "message");
    if (!(message instanceof AddressableMessage addressable)) {
      throw new IllegalArgumentException("messageType '" + message.messageType()
          + "' does not implement AddressableMessage (protocol §5.3); address the envelope"
          + " explicitly with send(message, recipient, payloadFormat)");
    }
    return send(message, addressable.address(), payloadFormat);
  }

  /**
   * Signs a multi-signable payload with this persona's single entry
   * (protocol §7.4 initial signing) and delivers it to {@code recipient}.
   *
   * @param messageType   wire message type of the payload (protocol §6.2)
   * @param message       the multi-signable payload
   * @param recipient     the recipient persona
   * @param payloadFormat payload format identifier
   * @return the multi-signed envelope (one entry) that was sent
   */
  public MultiSignedEnvelope sendMulti(
      String messageType, MultiSignableMessage message, PersonaId recipient,
      String payloadFormat) {
    Objects.requireNonNull(recipient, "recipient");
    MultiSignedEnvelope envelope = signer.signMulti(messageType, message, key, payloadFormat);
    transport.send(recipient.id(), envelope.encode(transportCodec));
    return envelope;
  }

  /**
   * Adds this persona's signature to an in-flight multi-signed envelope
   * (protocol §7.4: incremental signing yields a new envelope that
   * supersedes the previous one).
   *
   * @param envelope the envelope to extend
   * @return a new envelope with this persona's entry added
   */
  public MultiSignedEnvelope coSign(MultiSignedEnvelope envelope) {
    return signer.addSignature(envelope, key);
  }

  /**
   * Delivers an already-signed multi-signed envelope (e.g. the result of
   * {@link #coSign}) to {@code recipient}.
   *
   * @param envelope  the envelope to send
   * @param recipient the recipient persona
   * @throws DeliveryException if the transport cannot deliver
   */
  public void sendMulti(MultiSignedEnvelope envelope, PersonaId recipient) {
    Objects.requireNonNull(envelope, "envelope");
    Objects.requireNonNull(recipient, "recipient");
    transport.send(recipient.id(), envelope.encode(transportCodec));
  }

  /**
   * Signs an alert once and delivers the same bytes to every receiver of
   * its audience (protocol §5.4). The alert's
   * {@link AlertMessage#transportMetadata()} ({realm, alertType, alertId})
   * is the projection a real alert substrate would be given (protocol §5.4
   * last paragraph); the delivery substrate itself (protocol §7.6) is out
   * of scope — here every receiver is simply addressed through the
   * transport.
   *
   * @param alert         the alert payload
   * @param payloadFormat payload format identifier
   * @return the signed envelope that was sent to every receiver
   * @throws IllegalArgumentException if the alert is not valid (protocol
   *     §11 rule 6) or this persona is not authorized to sign it
   * @throws DeliveryException if any receiver is unreachable
   */
  public SignedEnvelope sendAlert(AlertMessage alert, String payloadFormat) {
    Objects.requireNonNull(alert, "alert");
    SignedEnvelope envelope = signer.sign(alert, key, payloadFormat);
    byte[] bytes = envelope.encode(transportCodec);
    for (PersonaId receiver : alert.receivers()) {
      transport.send(receiver.id(), bytes);
    }
    return envelope;
  }

  // -------------------------------------------------------------------------------------
  // Receive path (protocol §7.3)
  // -------------------------------------------------------------------------------------

  /**
   * Registers this messenger as the receiver for its own bare persona id
   * on the transport; every incoming envelope is verified and handed to
   * {@code handler} as a {@link ReceivedMessage}.
   *
   * @param handler consumer of received messages; invoked on the
   *                transport's delivery thread, must not block; receives
   *                verified and rejected outcomes alike (instrumentation is
   *                the handler's job, protocol §14 item 9)
   * @return a handle whose {@code close()} stops receiving
   */
  public AutoCloseable listen(Consumer<ReceivedMessage> handler) {
    Objects.requireNonNull(handler, "handler");
    return transport.listen(key.id().id(), bytes -> handler.accept(receive(bytes)));
  }

  /**
   * Decodes, verifies and materializes an encoded envelope.
   *
   * <p>Never throws for verification or decode failures: every outcome —
   * including bytes that are not an envelope at all — is a
   * {@link ReceivedMessage} whose {@code verification} carries the reason.
   * The payload is present only when verification succeeded (fail closed,
   * protocol §4).
   *
   * @param envelopeBytes encoded envelope bytes, may be {@code null}
   * @return the received message
   */
  public ReceivedMessage receive(byte[] envelopeBytes) {
    if (envelopeBytes == null) {
      return ReceivedMessage.rejected("envelope bytes are null");
    }
    try {
      return receiveSingle(envelopeBytes);
    } catch (Undecodable singleFailure) {
      try {
        return receiveMulti(envelopeBytes);
      } catch (Undecodable multiFailure) {
        return ReceivedMessage.rejected(
            "the bytes are not a decodable envelope (protocol §6.1/§6.2): as SignedEnvelope: "
                + singleFailure.getMessage() + "; as MultiSignedEnvelope: "
                + multiFailure.getMessage());
      }
    }
  }

  /** Decodes as a single-signed envelope, verifies, materializes (§7.3). */
  private ReceivedMessage receiveSingle(byte[] bytes) {
    SignedEnvelope envelope;
    try {
      envelope = SignedEnvelope.decode(bytes, transportCodec);
    } catch (RuntimeException e) {
      throw new Undecodable(e.getMessage());
    }
    List<PersonaId> signers = List.of(envelope.signedBy());
    VerificationResult result = verifier.verify(envelope);
    if (!result.verified()) {
      return ReceivedMessage.rejected(false, envelope.messageType(), signers,
          result.reason().orElse("?"));
    }
    return ReceivedMessage.verified(false, envelope.messageType(), signers,
        materialize(envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
            envelope.payload()));
  }

  /** Decodes as a multi-signed envelope, verifies, materializes (§7.3/§7.4). */
  private ReceivedMessage receiveMulti(byte[] bytes) {
    MultiSignedEnvelope envelope;
    try {
      envelope = MultiSignedEnvelope.decode(bytes, transportCodec);
    } catch (RuntimeException e) {
      throw new Undecodable(e.getMessage());
    }
    List<PersonaId> signers = new ArrayList<>(envelope.signatures().size());
    for (MultiSignature entry : envelope.signatures()) {
      signers.add(entry.signedBy());
    }
    VerificationResult result = verifier.verify(envelope);
    if (!result.verified()) {
      return ReceivedMessage.rejected(true, envelope.messageType(), signers,
          result.reason().orElse("?"));
    }
    return ReceivedMessage.verified(true, envelope.messageType(), signers,
        materialize(envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
            envelope.payload()));
  }

  /**
   * Materializes the payload of a <em>verified</em> envelope through its
   * schema, codec and binding (protocol §7.3 steps 2-3 — repeated here
   * because the verifier deliberately exposes only the outcome, not the
   * materialized message). Verification has already succeeded, so a failure
   * here is an internal inconsistency and surfaces as such.
   */
  private Validatable materialize(
      String messageType, String payloadFormat, String schemaId, byte[] payload) {
    PersonaSchema schema = resolveSchema(messageType, schemaId);
    Map<String, Object> value = PayloadFormats.require(payloadFormat).decode(schema, payload);
    return bindings.require(messageType).bind(value);
  }

  /**
   * Resolves the payload schema exactly as the verifier's §7.3 step 2 does:
   * via {@code schemaId} when present (cross-checked against the
   * {@code messageType} entry, {@code docs/SCHEMA.md} §6 rule 4), via
   * {@code messageType} otherwise.
   */
  private PersonaSchema resolveSchema(String messageType, String schemaId) {
    if (schemaId.isEmpty()) {
      return schemaRegistry.requireByMessageType(messageType);
    }
    PersonaSchema schema = schemaRegistry.byId(SchemaId.of(schemaId))
        .orElseThrow(() -> new IllegalStateException(
            "verified envelope carries schemaId '" + schemaId + "' that no longer resolves"
                + " (protocol §7.3 step 2)"));
    schemaRegistry.byMessageType(messageType).ifPresent(byType -> {
      if (!byType.equals(schema)) {
        throw new IllegalStateException(
            "verified envelope's schemaId '" + schemaId + "' no longer matches the schema"
                + " registered for messageType '" + messageType + "' (docs/SCHEMA.md §6 rule 4)");
      }
    });
    return schema;
  }

  /** Internal control flow: the bytes were not an envelope of this kind. */
  private static final class Undecodable extends RuntimeException {

    private Undecodable(String reason) {
      super(reason, null, false, false);
    }
  }
}
