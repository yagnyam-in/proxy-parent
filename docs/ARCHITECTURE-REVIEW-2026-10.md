# Architecture Review — Persona Protocol, final milestone pass

**Date:** 2026-10 (reviewed against the state below)
**Reviewer:** independent architecture-review agent (AGENTS.md workflow item 3)
**Scope:** design-level conformance of the assembled system to `PERSONA-PROTOCOL.md`, cross-layer invariants (§11/§12), normative-doc consistency, module boundaries, and readiness of the cross-language (C++) conformance story. Read-only review; every finding cites code or spec.

## 1. What was reviewed

| Checkout | Branch / commit | Content |
|---|---|---|
| `proxy-parent` | `master @ aa43733` | Schema layer, identity/crypto/registrar/resolver/trust, envelope layer with §6.4 type binding, PEM empty-body fix (#193) |
| `proxy-parent-conformance` | `persona/conformance-vectors @ 63fd6d4` | Reviewed at 27 files / **411 vectors**; PR #192 is now merged with 413 vectors, Java runner + independent Python reference; spec amendments §10.1 (PEM) and SCHEMA.md §3.3 (duplicate map keys) |
| `proxy-parent-p2p` | `persona/p2p-messaging @ 672fedf` | `in.yagnyam.persona.p2p` (transport, messenger, two-tier dedup), demo, refreshed `GAPS.md` |

Build verification (all green, run during this review):
- master `./scripts/test.sh`: **452 tests, 0 failures** — BUILD SUCCESS
- conformance branch: **419 tests** (incl. `ConformanceVectorSuite` 414) — BUILD SUCCESS
- p2p branch: **496 tests + PersonaMessagingIT** — BUILD SUCCESS

## 2. Per-area findings (severity-ordered)

### 2.1 Coherence against the §13 reference architecture — SOUND

Every box of the §13 diagram exists and is wired:

- **KeyStore (private)** → `PersonaKey` models plaintext-PEM or encrypted-key material, while `PemService` encodes/decodes plaintext PKCS#8 PEM. `PersonaKey.of()` creates plaintext PEM; secure-enclave placement or encryption before persistence is caller-managed under §3.3. The library excludes key material from `toString()` (`PersonaKey.java:191-195`).
- **MessageSigner** → `envelope/EnvelopeSigner.java` — implements §7.2 literally: `isValid()` + authorization asserted *before* signing (lines ~115-126), payload serialized once, §6.4 preimage, one signature per algorithm of `preferredSignatureAlgorithmSet`.
- **MessageVerifier {SchemaRegistry, PersonaResolver, CertificateService}** → `envelope/EnvelopeVerifier.java` + `schema/InMemorySchemaRegistry` + `resolver/InMemoryPersonaResolver` + `resolver/TrustValidator`. The verifier's class javadoc documents a step-by-step §7.3 mapping that matches the code.
- **Envelope codec** → `SignedEnvelope`/`MultiSignedEnvelope` encode/decode through the schema-driven codecs; payload is opaque bytes end-to-end.
- **RequestDedup** → `p2p/RequestDeduplicator.java` (p2p branch): two-tier design that is *more* rigorous than §5.2 requires — tier A identity uniqueness per `(recipient, requestId, bare signerId)`, tier B SHA-256 of the §6.4 preimage, which closes reorder/re-spelling/disjoint-quorum-split replays of a captured envelope. The javadoc's attack-by-attack analysis is exemplary.
- **Registrar CA** → `registrar/InProcessRegistrar.java`: §7.1 two-step flow, requestId uniqueness checked before minting (`InProcessRegistrar.java:206-219`), revocation-passphrase-hash registration and algorithm pinning to the active `PersonaVersion` (lines 231-236).

**Missing load-bearing pieces (vs §13/§14):** the remote tier. §9.1's layered certificate service (cache → durable store → remote Registrar) and §7.1's Registrar *HTTP client* (§14 item 5: "CSR construction and Registrar HTTP client") have no counterpart — only the in-process reference exists. `CsrService` covers CSR construction; nothing speaks to a remote Registrar. This is honestly acknowledged for alerts (GAPS #1) but **not listed in GAPS.md for the registrar/resolver remote tier** — see finding D4.

### 2.2 Cross-layer invariants — see §3 map

Two design decisions deserve explicit praise because they close attack classes the spec only implies:

1. **Resolved-identity binding** (`EnvelopeVerifier.assertResolvedIdentity`): the persona returned by the resolver must *be* the declared signer (id equals both persona id and certificate subject CN). Without it a malicious resolver could answer `alice`'s lookup with Mallory's valid certificate. This is defense beyond the letter of §7.3 and is exactly right.
2. **Crypto-sub-result-only caching** (`EnvelopeVerifier` javadoc + `signatureVerifies`): cache key = algorithm | SHA-256(preimage) | SHA-256(signature) | certificate thumbprint; positive-only; all policy/certificate checks repeated per envelope. This is precisely the caching option §7.3's last paragraph sanctions, and it makes revocation effective immediately at the verifier. The cache is LRU-bounded (`DEFAULT_VERIFIED_SIGNATURE_CACHE_CAPACITY = 4096`).

### 2.3 Findings requiring action

**F1 (Medium) — Normative-doc drift between master and the conformance branch.**
The vectors README cites `PERSONA-PROTOCOL.md §10.1` (PEM transport) and the suite pins multi-object PEM first-object semantics — but **§10.1 exists only on `persona/conformance-vectors`** (verified by diff: master `PERSONA-PROTOCOL.md` §10 has no subsections). Likewise `docs/SCHEMA.md §3.3` on the branch adds the duplicate-map-key binary rejection, while the *code already enforces it on master* (`BinaryPayloadCodec.java:475`, `entries.containsKey(key)`). Master therefore ships enforcement its own normative doc does not describe, and the branch ships doc text master lacks. Resolution: merge order matters; ideally land the §10.1 / §3.3 doc amendments on master independently of the vector suite.

**F2 (Medium) — GAPS.md (p2p branch) has gone stale in four places, against its own maintenance rule.** *(All four fixed on the p2p branch in commit `docs: refresh GAPS.md per architecture review`, alongside this review.)*
- **Entry 4, dedup bullet** described `RequestDeduplicator` as "keyed by `(sender, requestId)`: first wins" — the two-tier rewrite (commits `f4db4e5`, `77a984c`) keys tier A on `(recipient, requestId, signerId)` and tier B on `(recipient, preimageDigest)`. The sliding-window and per-process caveats remain true; the keying description was two revisions old.
- **Entry 4, crypto-cache bullet** claimed the verifier's cache "grows without bound — there is no eviction". Master `EnvelopeVerifier` has the bounded LRU (4096, access-order). Stale.
- **Entry 6** said "379 vectors"; `manifest.json` says **411**. Stale count.
- **Entry 7** (PemService NPE) was marked *fixed-pending-merge, PR #193* — but #193 is merged on master (`59bb644`) and master `PemService.java:176` carries the empty-body rejection. Per the file's own rule ("an entry leaves this file only when the fix is merged and covered by a test"), this entry was removed.

**F3 (Medium) — Cross-language signing evidence was missing from the conformance suite.**
At review time, the manifest had eleven kinds and no preimage/signature vectors; §6.4 was pinned only by Java unit tests. This was a major gap for a C++ implementation. *(Addressed on the stacked `persona/preimage-vectors` branch: 20 `envelope.signing-preimage` vectors and 15 `crypto.signature-verify` vectors, for 448 total with the now-merged base suite. The 5 Java preimage goldens match the portable vectors; signature verification is checked with fixed public-key/signature fixtures. The signature-verification vectors are verify-only because private-key material is not shipped, so exact signer-output conformance remains an adopter-level exchange. The preimage/signature branch remains unmerged.)*

**F4 (Medium) — Envelope-array breadth is uncapped (carried from GAPS #4, re-verified).**
`EnvelopeVerifier.verifyNested` recurses over `List` items with no per-envelope or per-array budget; only *depth* is capped (`MAX_NESTED_ENVELOPE_DEPTH = 100`). A schema declaring `array<envelope>` lets one payload carry thousands of nested envelopes, each costing schema resolution + certificate checks + signature verification. The codec's §3.4 collection cap (2^20) bounds count, not verification cost. This remains the largest practical DoS surface.

**F5 (Low) — §14 item 5 "PersonaResolver with caching" has no reference and no invalidation machinery.**
`InMemoryPersonaResolver` deliberately does no caching (revoked personas never resolve — safe). But the protocol's own checklist tells adopters to build a caching resolver, and §11 rule 11 mandates revocation invalidates cached resolutions, while the protocol defines no revocation *propagation* mechanism (GAPS #2). An adopter following §14 literally will build the exact component the protocol leaves unspecified. Acceptable as a documented gap; the pair (GAPS #2 + this) should cross-reference §14 item 5. *(Recorded in GAPS.md entry 7, added with this review.)*

**F6 (Low) — Realm isolation (§11 rule 12) is enforced only implicitly.**
Enforcement is: (a) `TrustValidator.chainsToTrustedRoot` — a foreign realm's certificates fail chaining because deployments pin their own realm's root; (b) `Realm.isValidRealm` grammar in `AlertMessage.isValid` (`AlertMessage.java:91`). Envelopes carry no realm field, so rule 12 reduces to root separation. That is a legitimate design, but neither PERSONA-PROTOCOL.md §7.3 nor GAPS.md says so; a foreign implementer could reasonably look for a realm check that does not exist. One sentence in §7.3 step 6 or in GAPS.md would close it.

**F7 (Low, recorded) — §5.3 address is routing metadata, not a verified binding** (GAPS #8; verified: no §7.3 step compares `address()` to the receiving endpoint). Consistent with the spec as written; the spec should say so explicitly.

**F8 (Informational) — No envelope-kind tag on the wire** (GAPS #9; verified in `PersonaMessenger.receive`: structural disambiguation single-then-multi). Works because the two shapes are incompatible at `signatures`; a convention foreign implementers must discover.

## 3. §11 / §12 enforcement map

### §11 Rules and Invariants

| # | Rule | Enforcing code | Status |
|---|---|---|---|
| 1 | Stable `messageType`, not a class name | Schema grammar `docs/SCHEMA.md` §1.3 rule 3 enforced at parse (`SchemaParser`); `MessageBindings`/`InMemorySchemaRegistry` keyed by string; envelope carries string `messageType` | ENFORCED |
| 2 | `signedBy` satisfies `canBeSignedBy` | `EnvelopeVerifier.verifySigned` (step 5) and `verifyMulti` per entry (`authorized()`); mirrored at sign time in `EnvelopeSigner.sign` | ENFORCED |
| 3 | Signature set matches a valid set exactly | `EnvelopeVerifier.rejectInvalidSignatureSet` + `PersonaVersion.supportsSignatureAlgorithmSet`; duplicates, supersets, subsets, empty set all rejected | ENFORCED (9 `crypto.signature-algorithm-set` vectors) |
| 4 | `isValid()` before signing and after deserialization | `EnvelopeSigner.requireValidPayload`; `EnvelopeVerifier.decodePayload` | ENFORCED |
| 5 | `requestId` uniqueness per (recipient, requester) | Registrar side: `InProcessRegistrar` (`:206-219`). Recipient side: `p2p/RequestDeduplicator` two-tier + `PersonaMessenger.dedupChecked` (post-verification, so the store can't be poisoned) | ENFORCED (merged in PR #195) |
| 6 | Alert receivers non-empty; transports keep `{realm, alertType, alertId}` | `AlertMessage.isValid` default (`AlertMessage.java:84-107`); `AlertTransportMetadata` projection. Transport non-stripping is N/A in-process; real substrates are GAPS #1 | ENFORCED (contract); substrate OPEN |
| 7 | `PersonaId` equality structural over `(id, thumbprint)` | `PersonaId` record + `canSignOnBehalfOf` implementing §3.2 delegation exactly (wildcard / unpinned-id match / exact equality) | ENFORCED (32 identity vectors) |
| 8 | Certificates chain to trusted root, within validity window | `TrustValidator.validate`: CA-cert rejection, id↔CN binding, pin match, window at verification-time `Instant`, issuer-DN + signature chain to pinned roots | ENFORCED |
| 9 | Nested envelopes verified before outer is verified | `EnvelopeVerifier.verifyNested` runs before outer signer-authorization and signature checks; depth cap 100 | ENFORCED (breadth uncapped — F4) |
| 10 | Exact bytes signed = exact bytes verified; §6.4 preimage | Opaque `payload` bytes end-to-end; `SigningPreimage` implements the length-prefixed format with the two domain constants; verifier recomputes from the envelope's own fields | ENFORCED (portable preimage vectors staged on `persona/preimage-vectors`, pending merge — F3) |
| 11 | Revocation invalidates cached resolutions | Reference wiring never caches resolutions (`InMemoryPersonaResolver`: revoked never resolve); `TrustValidator.RevocationCheck` runs per verification; verifier crypto-cache is revocation-safe by construction. **Distributed propagation unspecified** | PARTIAL (GAPS #2, F5) |
| 12 | Realm boundary not crossed | Implicit: trusted-root pinning (`TrustValidator.chainsToTrustedRoot`) + realm grammar on alerts. No envelope realm field | ENFORCED-BY-CONFIGURATION (F6) |

### §12 Security Considerations

| Consideration | Treatment | Status |
|---|---|---|
| Registrar compromise | Protocol non-goal (§4); single-root `InProcessRegistrar`; pinning is the adopter's trust decision (documented in `TrustValidator` javadoc) | DOCUMENTED |
| Private-key exfiltration | `PersonaKey.of()` and `PemService.encodePrivateKey()` produce plaintext PEM; `PersonaKey` can also carry caller-encrypted `CipherText` (`PersonaKey.java:52-69, 110-113`) and omits key material from `toString()` (`PersonaKey.java:187-195`). Secure-enclave placement, encryption before persistence, and storage protection are caller-managed per §3.3. Revocation passphrase hash is registered at creation (`InProcessRegistrar:116`). | PARTIAL (storage protection is caller-managed) |
| Downgrade on algorithm sets | Exact-set match, empty set rejected (`rejectInvalidSignatureSet`); all algorithm names live only in `PersonaVersionV0` (convention verified — no hard-coded algorithm strings elsewhere in policy paths) | ENFORCED |
| Replay on requests | Two-tier dedup (p2p branch), including disjoint-quorum-split replay — a class most implementations miss | ENFORCED (merged in PR #195) |
| Signature malleability (low-s) | N/A under v0 (RSA-only, `PersonaVersionV0.java:28-29`); must be revisited when Ed25519/ECDSA lands in v1 | NOTED for v1 |
| Nonce/IV reuse | Fresh `SecureRandom` IV per encryption (`BouncyCastleCryptographyService.java:41,70`); IV travels explicitly in `CipherText` | ENFORCED |
| Log leakage of payloads | `PersonaKey` excludes material from `toString` (`PersonaKey.java:19`); domain payloads are adopter code | ENFORCED (library scope) |
| Message-type squatting | Namespaced `messageType` grammar enforced at schema parse; namespace convention RECOMMENDED in SCHEMA.md §1.3 rule 3 | ENFORCED (grammar) |

## 4. Normative-document consistency

Checked: `PERSONA-PROTOCOL.md` (master) vs `docs/SCHEMA.md` (master + branch) vs `persona-conformance/vectors/README.md` (branch) vs `GAPS.md` (p2p branch).

1. **`§10.1` exists only on the conformance branch** (F1). The vectors README's citation `PERSONA-PROTOCOL.md §10.1` is dangling on master.
2. **SCHEMA.md §3.3 duplicate-map-key reject**: code-enforced on master (`BinaryPayloadCodec.java:475`), documented only on the branch (F1).
3. **GAPS.md**: four stale entries (F2) — dedup keying, crypto-cache eviction, vector count 379→411, and the already-merged #193. Because GAPS.md is the project's honesty artifact, staleness there is disproportionately damaging; its own maintenance rule demands a cleanup pass before the p2p merge. *(Done on the branch with this review.)*
4. **Vectors README ↔ SCHEMA.md**: §3.4 rule citations (rules 1, 2) resolve correctly; the documented Jackson JSON-decode depth leniency is accurately described as a reference-implementation gap rather than hidden — good practice.
5. **SPEC.md** carries no `PERSONA-PROTOCOL.md` section references (verified by grep), so §6.4/§6.2 amendments did not invalidate it.
6. No `proxy` vocabulary leaked into new code (one historical javadoc reference in `Validatable.java`, permitted by convention).

## 5. Ranked gaps for the cross-language mission

The mission: a C++ implementation proven against the conformance suite.

1. **Signing output is not pinned by a portable fixture.** The `persona/preimage-vectors` branch adds 20 preimage vectors and 15 verify-only RSA signature vectors, bringing the merged suite to 448 vectors and covering cross-language preimage construction and verification. Because the repository ships no private-key material, a foreign signer still needs an external signing/verification exchange to prove its output is accepted by another implementation. This is the remaining evidence gap for signer interoperability.
2. **Merge the remaining preimage/signature vectors** (F3): the base suite and normative amendments are now in master through PR #192; the cross-language signing vectors are the remaining evidence package.
3. **GAPS.md cleanup** (F2): cheap, and prerequisite to trusting the document that foreign implementers will read first. *(Done.)*
4. **Remote tier absence** (F5 + unlisted registrar HTTP client): not needed for conformance, but needed before the protocol can claim deployability beyond one process. *(Recorded in GAPS.md entry 7.)*
5. **Verification-cost budget** (F4): cap nested-envelope *breadth* (total envelopes per payload) before hostile schemas meet the verifier.
6. **Spec micro-clarifications** (F6 realm enforcement note; F7 §5.3 recipient binding; F8 envelope-kind rule): one or two sentences each.

## 6. Verdict

**The architecture is sound and the assembled system genuinely implements the protocol it specifies.** The §13 component model is fully present in-process; every §11 rule has named, tested enforcement code (or an honest, verified GAPS entry); the §6.4 type-binding, the resolver identity-binding check, and the two-tier dedup are *stronger* than the spec's letter. Module boundaries are clean (no wire logic in examples, all crypto policy in `PersonaVersion`, package-info everywhere, zero legacy vocabulary leakage). All three build lines are green.

What stands between this and the stated mission is not design but **evidence packaging**: the `persona/preimage-vectors` branch adds portable preimage and verify-only signature vectors, but signer-output conformance still needs an adopter-level signing/verification exchange because private keys are not shipped. The base suite and normative amendments are merged; only the signing-vector branch remains. GAPS.md's required cleanup is also merged. None of these require redesign.

**Recommendation: PASS. The base conformance suite and §10.1/§3.3 doc amendments, plus the GAPS.md refresh, have merged in PRs #192, #195, and #196. The portable preimage and verification vectors remain pending on `persona/preimage-vectors`; signer-output conformance remains an adopter-level exchange because the repository does not ship private keys.**
