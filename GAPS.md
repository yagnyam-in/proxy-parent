# GAPS.md — Known gaps between the Persona Protocol and this implementation

An engineering-honest list of what the protocol specifies but this codebase
does not (yet) do, what the protocol leaves unspecified, and where the
implementation's own defenses have limits. Every entry was verified against
the code and the spec text before being listed. Nothing here is marketing.

Status legend: **open** — not addressed; **partial** — addressed in part;
**fixed-pending-merge** — fixed on a branch, not yet on `master`.

---

## 1. Alert delivery substrates are unimplemented (§7.6) — open

- **What.** §7.6 defines the alert lifecycle over a broadcast substrate:
  subscribe (`SubscribeForAlertsRequest`), deliver (push), poll
  (`PendingAlertsRequest`), acknowledge (`DeleteAlertsRequest`). None of
  these request types or any push/poll substrate exists here. The library
  provides only the wire pieces: the `AlertMessage` contract (§5.4), the
  `AlertTransportMetadata` projection `{realm, alertType, alertId}` that
  §5.4 says every alert transport MUST be given, and
  `PersonaMessenger.sendAlert`, which signs once and point-to-point
  delivers the same envelope bytes to every receiver over the configured
  `PersonaTransport`.
- **Why it matters.** Real alert delivery needs device tokens, subscription
  state, a pending-alert store and ack handling — an entire service, not a
  codec. Adopters building on mobile/web push must implement §7.6
  themselves.
- **Severity.** Medium — a whole protocol section is contract-only.
- **Direction.** A `persona-alerts` module with an SPI for substrates (FCM,
  Web Push, MQTT) plus reference implementations of the three §7.6 request
  types.

## 2. Revocation propagation and caching are unspecified (§11 rule 11) — open

- **What.** Verification checks revocation *at verification time* (§7.3
  step 6), and §11 rule 11 says revocation MUST invalidate cached
  resolutions — but the protocol does not define how a revocation reaches a
  resolver: no push channel, no revocation list format, no freshness
  interval. In this codebase `InProcessRegistrar` + `InMemoryPersonaResolver`
  make revocation instantaneous *because both live in one process*; that
  says nothing about a distributed deployment. The envelope verifier
  deliberately caches **only the cryptographic sub-result** ("this
  signature verifies against this certificate"), keyed by
  algorithm | SHA-256(preimage) | SHA-256(signature) | certificate
  thumbprint, and repeats every policy and certificate check per envelope —
  so a revocation takes effect at that verifier immediately, even for
  envelopes whose crypto sub-result is cached positive. Operationally this
  means: **the window of exposure is exactly the resolver's staleness**, and
  §14 item 5's "PersonaResolver with caching" comes with no invalidation
  guidance.
- **Why it matters.** A deployment that caches resolutions naively extends
  the effective lifetime of a compromised key beyond revocation.
- **Severity.** High for distributed deployments; nil for the in-process
  reference wiring.
- **Direction.** Define a signed revocation-notice type (§11 rule 11
  already allows one), a max-age norm for cached resolutions, and resolver
  invalidation hooks.

## 3. Transport security is out of scope — envelopes are signed, not encrypted — open (by design, but adopters must act)

- **What.** The protocol defines envelopes, not channels (§10, §13).
  Everything on the wire — `messageType`, `signedBy`, and the payload bytes
  themselves — is readable by the transport. There is no confidentiality,
  no forward secrecy, and no endpoint authentication beyond the signature.
  `PersonaTransport` models exactly this: bytes from A to B, nothing more.
- **Why it matters.** Payloads may contain PII (§12). An adopter who treats
  "signed" as "secure in transit" will leak payload contents and traffic
  metadata to every hop.
- **Severity.** High if missed; it is a protocol-level stance, not a bug.
- **Direction.** Run the transport over TLS/WireGuard/etc.; if payload
  confidentiality at rest-in-transit is required, encrypt payloads at the
  application layer (a sealed-payload message type) — the protocol has no
  encryption envelope today.

## 4. Denial-of-service surface — partial

Bounded and documented, but with residual surface:

- **Request dedup.** `RequestDeduplicator` is a bounded LRU (default
  10,000 entries) keyed by `(sender, requestId)`: first wins, duplicates
  rejected (§5.2). Eviction is the safe direction — a re-request after
  eviction is processed again, never wrongly rejected — which makes replay
  protection a sliding window, not absolute. The store is also
  per-process: a restart or a second node loses it. Hard exactly-once
  requirements need a persistent/shared dedup store.
- **Nested-envelope cost.** Verification recurses into every nested
  envelope (§7.5). Depth is capped at 100
  (`EnvelopeVerifier.MAX_NESTED_ENVELOPE_DEPTH`), but **breadth is not
  capped**: an array field of type `envelope` may carry thousands of
  entries, each costing schema resolution, certificate checks and
  signature verification. Combined with a single-signed payload this is a
  verification-amplification vector against any peer that accepts a schema
  with envelope arrays.
- **Crypto-sub-result cache growth.** The verifier's positive-only cache
  grows by one entry per distinct genuine (preimage, signature,
  certificate) triple. Hostile input cannot grow it (entries require a
  real successful verification), but legitimate high-volume traffic grows
  it without bound — there is no eviction.
- **Severity.** Medium.
- **Direction.** A per-envelope budget (max nested envelopes total, max
  signatures per multi-envelope), size-bounded LRU for the crypto cache,
  and payload byte-size limits at the transport edge.

## 5. Identity and realm grammar ambiguities — open (documented in the vectors)

- **What.** The conformance-vectors README
  (`persona-conformance/vectors/README.md` on branch
  `persona/conformance-vectors`) documents spec ambiguities the vectors pin
  by reference-implementation behavior rather than by spec text: §3.2's
  "any URL-safe string" id has no grammar (so `álice` and `a b` parse);
  §3.2's thumbprint rendering ("base64/hex") is ambiguous — v0 pins
  64-char lowercase hex; §3.5 defines what a Realm is but not the
  identifier grammar; §9.1 multi-object PEM files parse their first object
  only; and the §3.4-rule-1 zero-byte asymmetry between the binary and
  JSON codecs makes one value non-portable across codecs.
- **Why it matters.** Two conforming implementations can disagree on
  inputs the spec never pins; divergence here is silent until it is a
  cross-implementation rejection.
- **Severity.** Low-to-medium; visible and documented, not silent.
- **Direction.** Promote the pinned behaviors into the spec text as
  normative grammar, or keep the vectors as the arbiter and say so in
  §3.2/§3.5.

## 6. Conformance vectors and the §6.4 preimage goldens are not on `master` — open

- **What.** The machine-readable vector suite (schema-digest, binary, JSON,
  envelope, identity, crypto — 379 vectors with a Java runner and an
  independent Python reference) lives on `persona/conformance-vectors` and
  is not merged into this line. Within that suite, there are **no
  signing/preimage vectors**: the §6.4 preimage goldens
  (SigningPreimageTest pins them as unit tests) are not promoted into
  `vectors/`, so a foreign implementation has no machine-readable way to
  prove it builds the same signing preimage — exactly the place where
  cross-language signature divergence would appear first.
- **Why it matters.** §6.4 is the type-binding that makes relabeled
  envelopes fail; leaving it untested cross-language weakens the suite's
  core claim.
- **Severity.** Medium.
- **Direction.** Merge the vectors branch; add a `envelope.preimage` vector
  kind with fixed `{messageType, payloadFormat, schemaId, payload}` inputs
  and expected preimage hex.

## 7. `PemService` empty-body handling — fixed-pending-merge

- **What.** Decoding a PEM block with an empty base64 body threw a
  `NullPointerException` instead of a clean rejection.
- **Status.** Fixed on branch `persona/pem-npe` (commit `5ed027f`, PR
  #193): an empty-body CERTIFICATE PEM is rejected properly. Awaiting
  merge; re-verify on `master` after it lands.

## 8. Payload addresses are routing metadata, not a verified binding (§5.3) — open (design observation)

- **What.** §5.3 gives `AddressableMessage.address()` as a routing hook,
  and §7.3 contains no step comparing the payload's declared recipient
  with the receiving endpoint. Verified behavior of this implementation
  (see `PersonaMessengerTest.sendToAnExplicitRecipientRoutesThere`): a
  greeting addressed to bob verifies perfectly when delivered to carol.
  The envelope authenticates the *signer*, never the *intended recipient*.
- **Why it matters.** An application that assumes "verified" implies "was
  meant for me" can be confused-deputied into acting on messages addressed
  elsewhere.
- **Severity.** Low — consistent with the spec as written; but the spec
  could say so explicitly.
- **Direction.** Document in §5.3 that recipient binding is the domain
  handler's job (compare `address()` with the local persona id on intake),
  or add an optional §7.3 step asserting it.

## 9. The wire carries no envelope-kind tag (§6.1/§6.2) — open (documentation)

- **What.** Nothing on the wire distinguishes a `SignedEnvelope` from a
  `MultiSignedEnvelope`; receivers resolve the kind structurally (this
  implementation tries the single-signed shape, then the multi-signed one —
  the two are incompatible at the `signatures` field, so exactly one can
  decode). It works, but it is a convention a foreign implementation must
  discover rather than read.
- **Why it matters.** An implementer who picks an envelope schema by
  guessing, or whose codec is lenient, risks accepting the wrong shape.
- **Severity.** Low.
- **Direction.** State the structural-disambiguation rule in §6, or add an
  explicit `envelopeKind` field in a future envelope version.

---

*Maintenance rule: an entry leaves this file only when the fix is merged
and covered by a test or vector. New gaps are added with the same rigor —
verified against code and spec, never speculative.*
