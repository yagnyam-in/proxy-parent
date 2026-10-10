package in.yagnyam.persona.envelope;

import in.yagnyam.persona.Persona;
import in.yagnyam.persona.PersonaCertificate;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.Digests;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.message.MultiSignableMessage;
import in.yagnyam.persona.message.SignableMessage;
import in.yagnyam.persona.resolver.PersonaResolver;
import in.yagnyam.persona.resolver.TrustResult;
import in.yagnyam.persona.resolver.TrustValidator;
import in.yagnyam.persona.schema.ArrayType;
import in.yagnyam.persona.schema.EnvelopeType;
import in.yagnyam.persona.schema.Field;
import in.yagnyam.persona.schema.MapType;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.RecordType;
import in.yagnyam.persona.schema.SchemaId;
import in.yagnyam.persona.schema.SchemaRegistry;
import in.yagnyam.persona.schema.TypeNode;
import in.yagnyam.persona.schema.codec.PayloadCodec;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Envelope verification per protocol §7.3 (fail-fast, §4: "Failure of any
 * of the above MUST be treated as a hard rejection. There is no partial
 * trust.").
 *
 * <p>Steps of §7.3, in order, for a {@link SignedEnvelope}:
 * <ol>
 *   <li>well-formedness, then the exact signature-set match against one of
 *       the active version's {@link PersonaVersion#validSignatureAlgorithmSets()}
 *       (protocol §11 rule 3, §12 downgrade defense: supersets, subsets,
 *       duplicate algorithm entries and the empty set are all rejected);</li>
 *   <li>resolve the payload schema from the registry — via {@code schemaId}
 *       when present, cross-checked against the {@code messageType} entry
 *       when both are registered ({@code docs/SCHEMA.md} §6 rule 4), via
 *       {@code messageType} otherwise;</li>
 *   <li>decode the payload with the schema and the codec of the envelope's
 *       {@code payloadFormat} (unknown formats are rejected, not ignored),
 *       materialize the domain message through its {@link MessageBinding},
 *       assert {@code isValid()} (protocol §11 rule 4);</li>
 *   <li>recursively verify every nested envelope found in fields and
 *       collections of the decoded payload (protocol §6.3, §7.5, §11 rule
 *       9), fail-fast;</li>
 *   <li>assert the §5.1 authorization predicate
 *       ({@code canBeSignedBy(signedBy)});</li>
 *   <li>resolve the signer persona and run the certificate checks
 *       <em>at verification time</em> — validity window, chain to a trusted
 *       registrar root, revocation — through the {@link TrustValidator},
 *       surfacing its failure reason;</li>
 *   <li>recompute the §6.4 preimage from the envelope's own
 *       {@code {messageType, payloadFormat, schemaId, payload}} — the exact
 *       bytes the envelope carries, never re-serialized — and verify every
 *       signature against the resolved certificate's public key.</li>
 * </ol>
 *
 * <p>A {@link MultiSignedEnvelope} is verified the same way (protocol
 * §7.4): steps 1-4 once for the envelope, then steps 5-7 independently per
 * {@link MultiSignature} entry. Any entry that fails any of its checks
 * rejects the whole envelope (protocol §4: no partial trust). The quorum is
 * a separate, final check (protocol §7.4): distinct <em>verified</em>
 * signers, deduplicated by the resolved certificate thumbprint —
 * {@code alice} and {@code alice#<thumbprint>} are one signer, never two
 * (protocol §6.2) — passed to the payload's
 * {@link MultiSignableMessage#hasSufficientSignatures(Set)}. Signature
 * order never affects the outcome (protocol §7.4): every entry is checked
 * and the quorum is counted over a set.
 *
 * <p><strong>Caching choice (protocol §7.3 last paragraph).</strong> This
 * verifier caches <em>only the cryptographic sub-result</em> — "this
 * signature verifies against this certificate's public key" — keyed by the
 * algorithm, a SHA-256 digest of the preimage, a SHA-256 digest of the
 * signature bytes and the verifying certificate's thumbprint. Every policy
 * and certificate check (schema resolution, payload validity, signer
 * authorization, resolution, validity window, chain, revocation) is
 * repeated per envelope, so a revoked, renewed or otherwise untrusted
 * certificate is rejected even when its cached crypto sub-result is
 * positive. Only positive results are cached: a cache entry therefore
 * requires a genuine successful verification with the private key, which an
 * attacker cannot forge, so hostile input cannot grow the cache — and the
 * cache is additionally bounded to
 * {@link #DEFAULT_VERIFIED_SIGNATURE_CACHE_CAPACITY} entries with LRU
 * eviction as a second line of defense (eviction only ever costs one
 * recomputed signature verification; it never changes a verdict). This
 * implements exactly the option protocol §7.3 sanctions: "Caching MAY
 * instead be applied only to the cryptographic sub-result (signature
 * verification against a specific certificate), with policy and certificate
 * checks repeated per envelope." A full verification-result cache was judged
 * premature: its invalidation rules (revocation, renewal, trust-state
 * change) are where silent acceptance bugs live.
 *
 * <p><strong>Nesting depth.</strong> Envelope recursion is capped at
 * {@link #MAX_NESTED_ENVELOPE_DEPTH} levels — a defensive strictness rule
 * mirroring {@code docs/SCHEMA.md} §3.4 rule 2 (which caps codec nesting at
 * 1000) but tighter, because each envelope level costs a verification
 * frame. Legitimate nesting (protocol §6.3/§7.5 examples) is a handful of
 * levels; 100 is generous.
 *
 * <p>Verification never throws for verification outcomes: every failure —
 * including unexpected runtime failures of codecs, bindings or resolvers on
 * hostile input — is converted into a {@link VerificationResult} carrying
 * the reason (fail closed).
 */
public final class EnvelopeVerifier {

  /**
   * The cap on nested-envelope verification depth (see the class javadoc):
   * an envelope nested more than this many levels deep is rejected.
   */
  static final int MAX_NESTED_ENVELOPE_DEPTH = 100;

  private final PersonaVersion version;
  private final CryptographyService cryptographyService;
  private final SchemaRegistry schemaRegistry;
  private final MessageBindings bindings;
  private final PersonaResolver resolver;
  private final TrustValidator trustValidator;
  private final Clock clock;

  /**
   * The default capacity of {@link #verifiedSignatureCache} in entries:
   * comfortably above any plausible working set of distinct verified
   * (preimage, signature, certificate) triples of one verifier, so eviction
   * only ever costs one recomputed signature verification — never a
   * different outcome.
   */
  static final int DEFAULT_VERIFIED_SIGNATURE_CACHE_CAPACITY = 4096;

  /**
   * The cryptographic-sub-result cache (see the class javadoc):
   * algorithm | sha256(preimage) | sha256(signature) | certificate
   * thumbprint → verified. Bounded to {@code cacheCapacity} entries with
   * access-order (LRU) eviction: an evicted entry is simply recomputed on
   * its next verification, so the bound cannot change any verdict.
   */
  private final Map<String, Boolean> verifiedSignatureCache;

  /**
   * Creates a verifier that evaluates certificate validity at the system
   * clock.
   *
   * @param version             active persona version
   * @param cryptographyService crypto primitives
   * @param schemaRegistry      payload schema registry
   * @param bindings            message bindings (payload value ⇄ domain
   *                            message)
   * @param resolver            persona resolution (protocol §9.2)
   * @param trustValidator      certificate validity/chain/revocation
   *                            checks (protocol §4)
   */
  public EnvelopeVerifier(
      PersonaVersion version,
      CryptographyService cryptographyService,
      SchemaRegistry schemaRegistry,
      MessageBindings bindings,
      PersonaResolver resolver,
      TrustValidator trustValidator) {
    this(version, cryptographyService, schemaRegistry, bindings, resolver, trustValidator,
        Clock.systemUTC());
  }

  /**
   * Creates a verifier.
   *
   * @param version             active persona version
   * @param cryptographyService crypto primitives
   * @param schemaRegistry      payload schema registry
   * @param bindings            message bindings (payload value ⇄ domain
   *                            message)
   * @param resolver            persona resolution (protocol §9.2)
   * @param trustValidator      certificate validity/chain/revocation
   *                            checks (protocol §4)
   * @param clock               the clock supplying "verification time"
   *                            (protocol §7.3 step 6)
   */
  public EnvelopeVerifier(
      PersonaVersion version,
      CryptographyService cryptographyService,
      SchemaRegistry schemaRegistry,
      MessageBindings bindings,
      PersonaResolver resolver,
      TrustValidator trustValidator,
      Clock clock) {
    this(version, cryptographyService, schemaRegistry, bindings, resolver, trustValidator,
        clock, DEFAULT_VERIFIED_SIGNATURE_CACHE_CAPACITY);
  }

  /**
   * Creates a verifier with an explicit crypto-sub-result cache capacity —
   * package-private so tests can exercise eviction without flooding the
   * default capacity.
   *
   * @param cacheCapacity maximum number of cached cryptographic sub-results
   *                      (see {@link #DEFAULT_VERIFIED_SIGNATURE_CACHE_CAPACITY})
   */
  EnvelopeVerifier(
      PersonaVersion version,
      CryptographyService cryptographyService,
      SchemaRegistry schemaRegistry,
      MessageBindings bindings,
      PersonaResolver resolver,
      TrustValidator trustValidator,
      Clock clock,
      int cacheCapacity) {
    this.version = Objects.requireNonNull(version, "version");
    this.cryptographyService = Objects.requireNonNull(cryptographyService, "cryptographyService");
    this.schemaRegistry = Objects.requireNonNull(schemaRegistry, "schemaRegistry");
    this.bindings = Objects.requireNonNull(bindings, "bindings");
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.trustValidator = Objects.requireNonNull(trustValidator, "trustValidator");
    this.clock = Objects.requireNonNull(clock, "clock");
    if (cacheCapacity < 1) {
      throw new IllegalArgumentException("cacheCapacity must be at least 1: " + cacheCapacity);
    }
    this.verifiedSignatureCache = Collections.synchronizedMap(
        new LinkedHashMap<>(256, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > cacheCapacity;
          }
        });
  }

  /**
   * Verifies a single-signed envelope (protocol §7.3).
   *
   * @param envelope the envelope to verify, may be {@code null}
   * @return verification result with a failure reason, never an exception
   */
  public VerificationResult verify(SignedEnvelope envelope) {
    try {
      verifySigned(envelope, 0);
      return VerificationResult.ok();
    } catch (Rejected e) {
      return VerificationResult.fail(e.reason);
    } catch (RuntimeException e) {
      return VerificationResult.fail("verification failed unexpectedly: " + e);
    }
  }

  /**
   * Verifies a multi-signed envelope (protocol §7.3 per entry, §7.4).
   *
   * @param envelope the envelope to verify, may be {@code null}
   * @return verification result with a failure reason, never an exception
   */
  public VerificationResult verify(MultiSignedEnvelope envelope) {
    try {
      verifyMulti(envelope, 0);
      return VerificationResult.ok();
    } catch (Rejected e) {
      return VerificationResult.fail(e.reason);
    } catch (RuntimeException e) {
      return VerificationResult.fail("verification failed unexpectedly: " + e);
    }
  }

  // -------------------------------------------------------------------------------------
  // SignedEnvelope (§7.3 for single signatures)
  // -------------------------------------------------------------------------------------

  private void verifySigned(SignedEnvelope envelope, int depth) {
    requireWithinDepth(depth);
    if (envelope == null) {
      throw new Rejected("envelope is null");
    }
    if (!envelope.isValid()) {
      throw new Rejected("envelope is not well-formed (protocol §7.3 step 1): " + envelope);
    }
    rejectInvalidSignatureSet(envelope.signatures(), "");
    DecodedPayload payload = decodePayload(envelope.messageType(), envelope.payloadFormat(),
        envelope.schemaId(), envelope.payload());
    Validatable message = payload.message();
    if (!(message instanceof SignableMessage signable)) {
      throw new Rejected("the payload of messageType '" + envelope.messageType()
          + "' does not implement SignableMessage (protocol §5.1) and cannot travel in a"
          + " SignedEnvelope");
    }
    verifyNested(payload.schema().rootRecord(), payload.value(), "", depth);
    if (!signable.canBeSignedBy(envelope.signedBy())) {
      throw new Rejected("persona '" + envelope.signedBy() + "' is not authorized to sign"
          + " messageType '" + envelope.messageType() + "' (protocol §7.3 step 5); valid"
          + " signers are " + signable.validSigners());
    }
    Persona signer = resolveAndValidate(envelope.signedBy());
    verifySignatureSet(envelope.signatures(), preimageOf(envelope), signer);
  }

  // -------------------------------------------------------------------------------------
  // MultiSignedEnvelope (§7.3 per entry, §7.4)
  // -------------------------------------------------------------------------------------

  private void verifyMulti(MultiSignedEnvelope envelope, int depth) {
    requireWithinDepth(depth);
    if (envelope == null) {
      throw new Rejected("envelope is null");
    }
    if (!envelope.isValid()) {
      throw new Rejected("envelope is not well-formed (protocol §7.3 step 1): " + envelope);
    }
    DecodedPayload payload = decodePayload(envelope.messageType(), envelope.payloadFormat(),
        envelope.schemaId(), envelope.payload());
    Validatable message = payload.message();
    if (!(message instanceof MultiSignableMessage multiSignable)) {
      throw new Rejected("the payload of messageType '" + envelope.messageType()
          + "' does not implement MultiSignableMessage (protocol §5.5) and cannot travel in a"
          + " MultiSignedEnvelope");
    }
    verifyNested(payload.schema().rootRecord(), payload.value(), "", depth);
    byte[] preimage = preimageOf(envelope);
    Set<PersonaId> verifiedSigners = new LinkedHashSet<>();
    for (MultiSignature entry : envelope.signatures()) {
      rejectInvalidSignatureSet(entry.signatures(),
          "entry of '" + entry.signedBy() + "': ");
      if (!authorized(entry.signedBy(), multiSignable.validSigners())) {
        throw new Rejected("persona '" + entry.signedBy() + "' is not authorized to co-sign"
            + " messageType '" + envelope.messageType() + "' (protocol §7.3 step 5 per entry,"
            + " §7.4); valid signers are " + multiSignable.validSigners());
      }
      Persona signer = resolveAndValidate(entry.signedBy());
      verifySignatureSet(entry.signatures(), preimage, signer);
      // §6.2: dedupe by the resolved certificate thumbprint — alice and
      // alice#<thumbprint> resolving to the same certificate are one signer.
      verifiedSigners.add(PersonaId.of(signer.certificate()));
    }
    if (!multiSignable.hasSufficientSignatures(verifiedSigners)) {
      throw new Rejected("quorum not reached for messageType '" + envelope.messageType()
          + "' (protocol §6.2, §7.4): " + verifiedSigners.size() + " distinct verified signer(s) "
          + verifiedSigners + " against a minimum of "
          + multiSignable.minimumRequiredSignatures());
    }
  }

  /** The §5.1 authorization predicate over a §5.5 signer set. */
  private static boolean authorized(PersonaId candidate, Set<PersonaId> validSigners) {
    if (validSigners == null) {
      return false;
    }
    for (PersonaId allowed : validSigners) {
      if (candidate.canSignOnBehalfOf(allowed)) {
        return true;
      }
    }
    return false;
  }

  // -------------------------------------------------------------------------------------
  // §7.3 step 1: well-formedness + signature-set policy
  // -------------------------------------------------------------------------------------

  /**
   * Rejects signature sets that are not an exact match of one of the active
   * version's valid sets, including supersets, subsets, the empty set and
   * duplicate algorithm entries (§6.1: "one entry per algorithm"; §11
   * rule 3; §12 downgrade defense).
   */
  private void rejectInvalidSignatureSet(List<Signature> signatures, String context) {
    Set<String> algorithms = new HashSet<>();
    for (Signature signature : signatures) {
      algorithms.add(signature.algorithm());
    }
    if (algorithms.size() != signatures.size()) {
      throw new Rejected(context + "signature set carries duplicate entries for the same"
          + " algorithm (protocol §6.1 requires one entry per algorithm): "
          + signatures.stream().map(Signature::algorithm).toList());
    }
    if (!version.supportsSignatureAlgorithmSet(algorithms)) {
      throw new Rejected(context + "signature set " + algorithms + " does not exactly match any"
          + " valid signature algorithm set " + version.validSignatureAlgorithmSets()
          + " (protocol §7.3 step 1, §11 rule 3, §12 downgrade defense: supersets, subsets and"
          + " the empty set are rejected)");
    }
  }

  // -------------------------------------------------------------------------------------
  // §7.3 steps 2-3: schema resolution + payload decode/materialization
  // -------------------------------------------------------------------------------------

  /**
   * Resolves the payload schema (§7.3 step 2): via {@code schemaId} when
   * present, cross-checked against the {@code messageType} entry when both
   * are registered ({@code docs/SCHEMA.md} §6 rule 4); via
   * {@code messageType} when the id is absent.
   */
  private PersonaSchema resolveSchema(String messageType, String schemaId) {
    Optional<PersonaSchema> byMessageType = schemaRegistry.byMessageType(messageType);
    PersonaSchema schema;
    if (schemaId.isEmpty()) {
      schema = byMessageType.orElseThrow(() -> new Rejected(
          "no schema registered for messageType '" + messageType + "' and the envelope carries"
              + " no schemaId to resolve one (protocol §7.3 step 2)"));
    } else {
      SchemaId id;
      try {
        id = SchemaId.of(schemaId);
      } catch (RuntimeException e) {
        throw new Rejected("malformed schemaId '" + schemaId + "': " + e.getMessage());
      }
      schema = schemaRegistry.byId(id).orElseThrow(() -> new Rejected(
          "no schema registered with schemaId '" + schemaId + "' (protocol §7.3 step 2)"));
      if (byMessageType.isPresent() && !byMessageType.get().equals(schema)) {
        throw new Rejected("schemaId '" + schemaId + "' does not match the schema registered for"
            + " messageType '" + messageType + "' (docs/SCHEMA.md §6 rule 4: a payload encoded"
            + " with schema S must not decode with schema S' — the digest and the message-type"
            + " entry must agree)");
      }
    }
    return schema;
  }

  /**
   * Decodes the payload with the codec of the envelope's {@code payloadFormat}
   * (§7.3 step 3), materializes the domain message through its binding, and
   * asserts the payload's {@code isValid()} (§11 rule 4).
   */
  private DecodedPayload decodePayload(
      String messageType, String payloadFormat, String schemaId, byte[] payload) {
    PersonaSchema schema = resolveSchema(messageType, schemaId);
    PayloadCodec codec;
    try {
      codec = PayloadFormats.require(payloadFormat);
    } catch (RuntimeException e) {
      throw new Rejected(e.getMessage());
    }
    Map<String, Object> payloadValue;
    try {
      payloadValue = codec.decode(schema, payload);
    } catch (RuntimeException e) {
      throw new Rejected("the payload of messageType '" + messageType + "' cannot be decoded as '"
          + payloadFormat + "' (protocol §7.3 step 3): " + e.getMessage());
    }
    MessageBinding binding = bindings.forMessageType(messageType)
        .orElseThrow(() -> new Rejected(
            "no message binding registered for messageType '" + messageType + "' (protocol §7.3"
                + " steps 3 and 5: the recipient must be able to materialize the payload to"
                + " assert its validity and the signer's authorization; protocol §4: a payload"
                + " that does not deserialize into the expected message type is a hard"
                + " rejection)"));
    Validatable message;
    try {
      message = binding.bind(payloadValue);
    } catch (RuntimeException e) {
      throw new Rejected("the payload of messageType '" + messageType + "' cannot be"
          + " materialized (protocol §7.3 step 3): " + e.getMessage());
    }
    if (!message.isValid()) {
      throw new Rejected("the payload of messageType '" + messageType + "' is not valid"
          + " (protocol §7.3 step 3, §11 rule 4)");
    }
    return new DecodedPayload(schema, payloadValue, message);
  }

  // -------------------------------------------------------------------------------------
  // §7.3 step 4: recursive nested verification (§6.3, §7.5, §11 rule 9)
  // -------------------------------------------------------------------------------------

  /**
   * Walks the schema alongside the decoded value and verifies every nested
   * envelope (fields, arrays, maps — everything the schema types
   * {@code envelope}/{@code multi-envelope}), fail-fast (§11 rule 9: nested
   * envelopes MUST be verified before the outer payload is verified).
   */
  private void verifyNested(TypeNode type, Object value, String path, int depth) {
    switch (type) {
      case RecordType record -> {
        Map<String, Object> recordValue = requireRecordValue(value, path);
        for (Field field : record.fields()) {
          Object fieldValue = recordValue.get(field.name());
          if (fieldValue == null) {
            throw new Rejected("the decoded payload has no value for field '" + childPath(path,
                field.name()) + "' — it did not decode per its schema");
          }
          verifyNested(field.type(), fieldValue, childPath(path, field.name()), depth);
        }
      }
      case ArrayType array -> {
        if (!(value instanceof List<?> items)) {
          throw new Rejected("the decoded payload field '" + path + "' is not an array");
        }
        for (int i = 0; i < items.size(); i++) {
          verifyNested(array.items(), items.get(i), path + "[" + i + "]", depth);
        }
      }
      case MapType map -> {
        Map<String, Object> entries = requireRecordValue(value, path);
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
          verifyNested(map.values(), entry.getValue(),
              path.isEmpty() ? entry.getKey() : path + "." + entry.getKey(), depth);
        }
      }
      case EnvelopeType envelopeType -> {
        Map<String, Object> envelopeValue = requireRecordValue(value, path);
        try {
          if (envelopeType.multi()) {
            verifyMulti(MultiSignedEnvelope.fromValue(envelopeValue), depth + 1);
          } else {
            verifySigned(SignedEnvelope.fromValue(envelopeValue), depth + 1);
          }
        } catch (Rejected e) {
          throw new Rejected("the nested envelope at '" + (path.isEmpty() ? "<root>" : path)
              + "' is not verified: " + e.reason);
        } catch (RuntimeException e) {
          throw new Rejected("the nested envelope at '" + (path.isEmpty() ? "<root>" : path)
              + "' is malformed: " + e.getMessage());
        }
      }
      default -> {
        // primitives and enums carry no nested envelopes
      }
    }
  }

  private static Map<String, Object> requireRecordValue(Object value, String path) {
    if (!(value instanceof Map<?, ?> map)) {
      throw new Rejected("the decoded payload field '" + path + "' is not a record");
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> entries = (Map<String, Object>) map;
    return entries;
  }

  private static String childPath(String path, String name) {
    return path.isEmpty() ? name : path + "." + name;
  }

  private void requireWithinDepth(int depth) {
    if (depth > MAX_NESTED_ENVELOPE_DEPTH) {
      throw new Rejected("envelope nesting exceeds the maximum depth of "
          + MAX_NESTED_ENVELOPE_DEPTH + " (defensive strictness mirroring docs/SCHEMA.md"
          + " §3.4 rule 2)");
    }
  }

  // -------------------------------------------------------------------------------------
  // §7.3 step 6: resolution + certificate checks at verification time
  // -------------------------------------------------------------------------------------

  /**
   * Resolves the signer persona and validates its certificate at
   * verification time (§7.3 step 6), surfacing the resolver or
   * {@link TrustValidator} failure reason.
   */
  private Persona resolveAndValidate(PersonaId signedBy) {
    Optional<Persona> resolved;
    try {
      resolved = resolver.resolvePersona(signedBy);
    } catch (RuntimeException e) {
      throw new Rejected("the persona '" + signedBy + "' could not be resolved (protocol §7.3"
          + " step 6): " + e.getMessage());
    }
    Persona persona = resolved.orElseThrow(() -> new Rejected(
        "the persona '" + signedBy + "' could not be resolved via the PersonaResolver (unknown id,"
            + " revoked certificate, or pin mismatch; protocol §7.3 step 6)"));
    assertThumbprintPin(signedBy, persona);
    TrustResult trust = trustValidator.validate(persona, clock.instant());
    if (!trust.trusted()) {
      throw new Rejected("the persona '" + signedBy + "' is not trusted at verification time"
          + " (protocol §7.3 step 6): " + trust.reason().orElse("?"));
    }
    return persona;
  }

  /**
   * Pin consistency at the trust boundary (§7.3 step 6): when
   * {@code signedBy} carries a thumbprint pin, the persona the resolver
   * returned must actually be pinned to that certificate. A resolver that
   * silently widens a pinned id — e.g. strips the thumbprint and resolves
   * <em>some</em> certificate of the same persona — would otherwise let an
   * envelope claim a pin nobody signed up to; the verifier, not resolver
   * etiquette, is where that widening is stopped.
   */
  private static void assertThumbprintPin(PersonaId signedBy, Persona persona) {
    if (signedBy.sha256Thumbprint().isEmpty() || signedBy.isWildcard()) {
      return;
    }
    PersonaCertificate certificate = persona.certificate();
    String resolvedThumbprint = certificate == null ? null : certificate.sha256Thumbprint();
    if (!signedBy.sha256Thumbprint().get().equals(resolvedThumbprint)) {
      throw new Rejected("the persona '" + signedBy + "' carries a certificate thumbprint pin"
          + " but resolved to certificate thumbprint '"
          + (resolvedThumbprint == null ? "<none>" : resolvedThumbprint)
          + "' (protocol §7.3 step 6: a pinned signedBy must resolve to the pinned"
          + " certificate)");
    }
  }

  // -------------------------------------------------------------------------------------
  // §7.3 step 7: preimage + signature verification (with the crypto-only cache)
  // -------------------------------------------------------------------------------------

  private static byte[] preimageOf(SignedEnvelope envelope) {
    return SigningPreimage.forSignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
        envelope.payload());
  }

  private static byte[] preimageOf(MultiSignedEnvelope envelope) {
    return SigningPreimage.forMultiSignedEnvelope(
        envelope.messageType(), envelope.payloadFormat(), envelope.schemaId(),
        envelope.payload());
  }

  /**
   * Verifies every signature of a set against the preimage and the signer's
   * certified public key (§7.3 step 7) — all algorithms of the set are
   * computed over the same preimage (§6.4).
   */
  private void verifySignatureSet(List<Signature> signatures, byte[] preimage, Persona signer) {
    PersonaCertificate certificate = signer.certificate();
    if (certificate == null || certificate.certificate() == null) {
      throw new Rejected("the persona '" + signer.getUniqueId()
          + " carries no usable certificate handle (protocol §7.3 step 7)");
    }
    for (Signature signature : signatures) {
      if (!signatureVerifies(signature, preimage, certificate)) {
        throw new Rejected("the signature with algorithm '" + signature.algorithm()
            + "' does not verify against the certificate of '" + signer.getUniqueId()
            + "' (protocol §7.3 step 7: the preimage of §6.4 is recomputed from the envelope's"
            + " own messageType, payloadFormat, schemaId and payload bytes — a relabeled"
            + " envelope or tampered payload never verifies)");
      }
    }
  }

  /**
   * The cached cryptographic sub-result. Cache hits are possible only for
   * previously <em>successful</em> verifications; see the class javadoc for
   * why negative results are never cached and why policy checks are
   * repeated regardless.
   */
  private boolean signatureVerifies(
      Signature signature, byte[] preimage, PersonaCertificate certificate) {
    String cacheKey = signature.algorithm()
        + '|' + Digests.sha256Hex(preimage)
        + '|' + Digests.sha256Hex(signature.value())
        + '|' + certificate.sha256Thumbprint();
    if (Boolean.TRUE.equals(verifiedSignatureCache.get(cacheKey))) {
      return true;
    }
    boolean verified;
    try {
      verified = cryptographyService.verifySignature(
          signature.algorithm(), certificate.certificate().getPublicKey(), preimage,
          signature.value());
    } catch (GeneralSecurityException e) {
      throw new Rejected("the signature with algorithm '" + signature.algorithm()
          + "' cannot be verified: " + e.getMessage());
    }
    if (verified) {
      verifiedSignatureCache.put(cacheKey, Boolean.TRUE);
    }
    return verified;
  }

  /** The decoded payload of §7.3 steps 2-3, shared by steps 4-7. */
  private record DecodedPayload(PersonaSchema schema, Map<String, Object> value,
      Validatable message) {}

  /** Internal fail-fast control-flow exception; never escapes {@link #verify}. */
  private static final class Rejected extends RuntimeException {

    private final String reason;

    private Rejected(String reason) {
      super(reason, null, false, false);
      this.reason = reason;
    }

    private String reason() {
      return reason;
    }
  }
}
