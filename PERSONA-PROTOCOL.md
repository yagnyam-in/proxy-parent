# The Persona Protocol

> A language- and transport-agnostic specification for building systems around **pseudonymous cryptographic identities** and **signed, verifiable messages**. It abstracts the ideas found in the `proxy-parent` codebase so they can be reused in any project, in any language, over any transport (HTTP, gRPC, message bus, offline QR code, mesh network).
>
> Nothing in this document is tied to Java, Jackson, BouncyCastle, Objectify, Google App Engine, or any specific serialization format. Encoding-specific guidance is confined to §10 and §11.
>
> Normative keywords (**MUST**, **SHOULD**, **MAY**) follow RFC 2119.

---

## 1. Purpose

The protocol lets any two parties:

1. Interact under stable **pseudonymous identities** rather than real-world identifiers (email, phone, government ID, business name).
2. Exchange **strongly typed, digitally signed messages** whose authenticity, integrity, and provenance are independently verifiable.
3. Compose messages hierarchically — one signed message may embed others signed by different parties — enabling multi-party workflows (payments, escrow, identity assertions) without a central broker.
4. Evolve schemas, algorithms, and domain vocabularies without breaking existing peers.

The protocol is a foundation, not a product. It does not prescribe a domain (banking, identity, chat, IoT). Domains plug in as **payload types** (§7).

---

## 2. Terminology

| Term                    | Meaning                                                                                                                                                       |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Persona**             | A pseudonymous identity: a stable, verifiable public identifier backed by an X.509 certificate and a locally held key pair.                                   |
| **Persona ID**          | The unique addressable identifier of a Persona. Structurally `<id>` or `<id>#<thumbprint>`.                                                                    |
| **Persona Key**         | The private key material (and its metadata) held only by the Persona owner. Never leaves the owner's device unencrypted.                                       |
| **Certificate**         | An X.509 certificate binding a Persona ID (as subject `CN`) to a public key, issued by a Registrar.                                                            |
| **Registrar**           | The Certificate Authority that issues Persona certificates. There is exactly one root Registrar per Realm.                                                     |
| **Realm**               | A named deployment scope (e.g. `production`, `staging`). Personas from different Realms MUST NOT be conflated.                                                 |
| **Payload**             | Any typed, structured record a Persona wishes to sign.                                                                                                        |
| **Envelope**            | A wrapper that carries a payload together with its type, signer(s), and signature(s).                                                                          |
| **Resolver**            | A service that maps a Persona ID to its Persona (including its current certificate).                                                                          |
| **Message Type**        | A stable, namespaced identifier for a payload schema (e.g. `banking.PersonaAccount@v3`). Not tied to programming-language class names.                        |
| **Addressable Message** | A payload that declares its intended recipient Persona.                                                                                                       |
| **Request Message**     | A payload that carries a unique `requestId`, enabling idempotent processing.                                                                                  |
| **Alert Message**       | A signed broadcast payload addressed to a set of receiver Personas.                                                                                           |
| **Nested envelope**     | A field within a payload that is itself a signed envelope, signed by a different Persona.                                                                     |

---

## 3. Core Concepts

### 3.1 Persona

A Persona is defined by four facts, all of which are publicly verifiable:

1. Its **Persona ID** — a globally unique string within a Realm.
2. Its **certificate** — issued and signed by the Realm's Registrar.
3. Its **public key** — carried inside the certificate.
4. Its **validity window** — `validFrom`/`validTill` from the certificate.

A Persona is **not** a real-world entity. A human, a company, a service, or a device can each be represented by one or many Personas. Personas are cheap and disposable; a party MAY rotate its Persona at any time by registering a new one.

Two Personas are equal iff they share both `id` and certificate thumbprint. Two Personas that share only `id` (e.g. after re-issuance) are considered **different instances of the same logical identity**.

### 3.2 Persona ID

Canonical string form:

```
<id>                     # unpinned — matches any certificate for this id
<id>#<thumbprint>        # pinned to a specific certificate
```

Where:
- `id` is any URL-safe string chosen by the Registrar (opaque to peers).
- `thumbprint` is a base64/hex encoded hash of the certificate's DER encoding, using the Realm's canonical hash algorithm (§8).

The wildcard token `any` (or `any#any`) MAY be used in schemas that intend "any signer is acceptable".

**Delegation rule.** A Persona ID `X` "can sign on behalf of" `Y` iff any of the following hold:
- `Y` is the wildcard.
- `Y.id == X.id` and `Y.thumbprint` is absent (unpinned).
- `X == Y` exactly.

Implementations MAY extend this with explicit delegation lists, but the base rule above MUST be honored.

### 3.3 Persona Key

The Persona owner's private key material. Requirements:

- MUST support at least one signature algorithm listed in the active Persona Version (§8).
- MUST be stored either as plaintext PEM in a secure enclave (hardware key store, TPM, mobile keychain), or as ciphertext protected by a user-supplied passphrase.
- SHOULD be paired with a **revocation passphrase** whose hash is registered with the Registrar at Persona creation, enabling out-of-band revocation without exposing the private key.

### 3.4 Registrar

The Registrar is the sole issuer of certificates within a Realm. Its responsibilities:

1. Issue a fresh, unused Persona ID on request.
2. Accept a Certificate Signing Request (CSR) and issue a signed certificate binding that ID to the presented public key.
3. Publish certificates (and certificate chains) at deterministic URLs so any peer can resolve them.
4. Accept revocation requests authenticated by the pre-registered revocation passphrase hash.
5. Publish its own root certificate at a well-known location so peers can pin it.

The Registrar is a **single point of trust** for the Realm. Downgrading this to a decentralized model (e.g. web-of-trust, on-chain) is a valid extension but is out of scope for this document.

### 3.5 Realm

A Realm is a namespace. Realms provide:

- Isolation between environments (`production`, `staging`, `dev-alice`).
- Namespacing for Persona IDs — the same string in different Realms refers to different Personas.
- A binding to a specific Registrar and, if desired, a specific Persona Version.

The Realm identifier MUST appear in every alert message and in every domain object whose identity is Realm-scoped (accounts, subject records, etc.).

---

## 4. Trust Model

A peer accepts an envelope as authentic when **all** of the following hold:

1. The envelope's outer structure is well-formed and its payload deserializes into the expected message type.
2. The declared `signedBy` Persona ID satisfies the payload's signer policy (`canBeSignedBy`).
3. The Persona referenced by `signedBy` is currently known to the Resolver and its certificate is within its validity window and chains to a trusted Registrar root.
4. Every declared signature verifies against that Persona's certified public key.
5. The signature algorithm(s) present exactly match one of the algorithm sets sanctioned by the active Persona Version.
6. Every nested envelope inside the payload is itself verified by the same procedure (recursively).
7. The payload's own domain rules (`isValid`) hold.

Failure of any of the above MUST be treated as a hard rejection. There is no partial trust.

**Non-goals.** The protocol does not defend against:
- A compromised Registrar (mitigation: pinning, transparency logs, out-of-band Registrar root distribution).
- A Persona owner leaking their own private key (mitigation: revocation via passphrase).
- Traffic analysis / metadata correlation (mitigation: layer above onion routing or mixnets if needed).

---

## 5. Message Model

The protocol defines five orthogonal contracts a domain payload MAY implement.

### 5.1 `SignableMessage` (base contract)

Every payload that is signed MUST provide:

- `messageType(): string` — a stable, namespaced identifier (e.g. `payments.PaymentAuthorization@v2`).
- `signer(): PersonaId` — the canonical Persona expected to sign this payload.
- `validSigners(): Set<PersonaId>` — defaults to `{signer()}`; MAY include delegates.
- `isValid(): bool` — a self-check of internal invariants.

A signer S is authorized to sign a payload P iff `S.canSignOnBehalfOf(v)` for some `v ∈ P.validSigners()`.

### 5.2 `RequestMessage`

A payload that participates in an idempotent request/response flow:

- `requestId(): string` — globally unique within the recipient's namespace.

Recipients MUST reject duplicate `requestId`s from the same Persona.

### 5.3 `AddressableMessage`

A payload that declares its intended recipient:

- `address(): PersonaId` — the target Persona for the message.

This enables routing without inspecting the payload contents.

### 5.4 `AlertMessage` (SignableMessage + broadcast)

A payload intended for one or more subscribers:

- `realm(): string` — Realm identifier.
- `alertId(): string` — unique alert identifier for deduplication.
- `receivers(): List<PersonaId>` — target audience.
- `alertType(): string` — same as `messageType()`; surfaced separately so a client can dispatch without decoding the full body.

Alert transports (e.g. FCM, Web Push, MQTT) MUST be given at least `{realm, alertType, alertId}` in the transport-level metadata.

### 5.5 `MultiSignableMessage`

A payload requiring signatures from multiple Personas (e.g. escrow proposals):

- `validSigners(): Set<PersonaId>` — the set of Personas allowed to sign.
- `minimumRequiredSignatures(): int` — quorum threshold.
- `hasSufficientSignatures(Set<PersonaId>): bool` — completeness check.

Multi-signable payloads MAY be signed incrementally: each additional signature yields a new envelope that supersedes the previous.

---

## 6. Envelope Format

An envelope is an **encoding-agnostic** structure. Any binary or textual encoding (JSON, Avro, Protobuf, CBOR, MessagePack) is acceptable so long as the fields below are represented faithfully and signature bytes are stable (§10).

### 6.1 `SignedEnvelope`

```
SignedEnvelope {
    messageType   : string          // stable message-type identifier
    payload       : bytes           // canonical serialized payload (opaque to the envelope)
    payloadFormat : string          // "json", "avro", "protobuf", ...
    schemaId      : string?         // optional schema fingerprint for payloadFormat
    signedBy      : string          // PersonaId of the signer
    signatures    : [Signature]     // one entry per algorithm in the active algorithm set
}

Signature {
    algorithm : string              // e.g. "SHA256withRSA"
    value     : bytes               // signature bytes (base64 in textual encodings)
}
```

Design rules:

- `payload` is **opaque bytes**. The signer computed its signature over these exact bytes; verifiers MUST NOT re-encode before verifying. This decouples signature stability from serializer quirks.
- `messageType` is authoritative for routing; peers MUST resolve the payload schema from `messageType` (and optionally `schemaId`), NOT from a programming-language class name.
- A single logical signature MAY be a *set* of concrete signatures (e.g. RSA + Ed25519 during a migration). The set as a whole MUST match one of the algorithm sets declared by the active Persona Version.

### 6.2 `MultiSignedEnvelope`

```
MultiSignedEnvelope {
    messageType   : string
    payload       : bytes
    payloadFormat : string
    schemaId      : string?
    signatures    : [MultiSignature]
}

MultiSignature {
    signedBy   : string
    signatures : [Signature]        // one per algorithm in the active algorithm set
}
```

`hasSufficientSignatures()` is evaluated against the set of distinct, verified
signer identities. The `signedBy` set MUST be deduplicated by the **resolved
certificate thumbprint** before the quorum is counted: the spellings `alice`
and `alice#<thumbprint>` that resolve to the same certificate are one signer,
never two. A quorum MUST count each distinct verified certificate at most once.
This envelope-layer deduplication governs even when a payload overrides
`hasSufficientSignatures()` with a custom rule: the payload rule always
evaluates the distinct-certificate set, so two certificates of one re-keyed
persona (§3.1) count as two signers at the envelope layer and are further
collated (e.g. by bare persona id) only by the payload rule itself.

### 6.3 Nested envelopes

A payload MAY contain fields whose type is `SignedEnvelope` or `MultiSignedEnvelope`. Recipients MUST recursively verify every nested envelope before treating the outer payload as verified.

Recommended pattern:

```
PaymentAuthorization {
    paymentAuthorizationId : string
    payerAccount           : SignedEnvelope   // signed by the payer's bank Persona
    amount                 : Amount
    payees                 : [Payee]
}
```

### 6.4 Signature preimage (type binding)

Signatures are computed over a **signing preimage** that binds the payload
bytes to the envelope fields that determine how the payload is interpreted.
This prevents type-confusion attacks in which an attacker re-labels a valid
signed payload with a different `messageType` or schema and the signature
still verifies.

The preimage is an unambiguous byte sequence built from:

```
domain       = "persona.signed-envelope@v1"          // for SignedEnvelope
             | "persona.multi-signed-envelope@v1"     // for MultiSignedEnvelope

preimage     = uvarint(len(domain))       || domain (UTF-8)
             || uvarint(len(messageType)) || messageType (UTF-8)
             || uvarint(len(payloadFormat)) || payloadFormat (UTF-8)
             || uvarint(len(schemaId))    || schemaId (UTF-8; empty if absent)
             || uvarint(len(payload))     || payload (raw bytes)
```

Where `uvarint(n)` is the unsigned LEB128 encoding of the byte length `n`.
All five fields are uniformly length-prefixed so the preimage is
self-framing and unambiguous — in particular between payloads where one is a
prefix of the other; the `domain` string still versions the format itself.
All signature algorithms in an envelope's signature set are computed over this
same preimage. The distinct `domain` constants prevent signature transplant
between single-signed and multi-signed envelopes carrying identical payloads.

---

## 7. Protocols

### 7.1 Persona Registration

Two-step flow, both steps MAY be performed by the client:

```
Client                                                  Registrar
  │                                                            │
  │ 1) POST /persona-id  { requestId }                         │
  ├───────────────────────────────────────────────────────────►│
  │                                                            │
  │◄───────────────────────────────────────────────────────────┤
  │      { requestId, personaId }                              │
  │                                                            │
  │ 2) Locally: generate key pair, build CSR (CN=personaId),   │
  │             hash the revocation passphrase.                │
  │                                                            │
  │ 3) POST /persona {                                         │
  │        requestId,                                          │
  │        personaId,                                          │
  │        revocationPassPhraseHash,                           │
  │        certificateRequestEncoded                           │
  │    }                                                       │
  ├───────────────────────────────────────────────────────────►│
  │                                                            │
  │◄───────────────────────────────────────────────────────────┤
  │      Persona { id, certificate, publicKey, ... }           │
  │      (optionally wrapped in a SignedEnvelope signed by     │
  │       the Registrar)                                       │
```

Rules:

- `requestId` MUST be a UUIDv4 or equivalent unpredictable identifier.
- The Registrar MUST reject a second request with the same `requestId`.
- The client MUST NOT trust a bare (unsigned) response in production; it SHOULD prefer a `SignedEnvelope` signed by the Registrar Persona.
- The revocation passphrase MUST be hashed with a per-Persona salt before submission (see §8).

### 7.2 Signing

Given a payload `P` and a Persona Key `K`:

```
1. Assert P.isValid() and K.id.canSignOnBehalfOf(v) for some v in P.validSigners().
2. Serialize P once with the chosen payload format → payloadBytes.
3. Compute the signing preimage (§6.4) from
       {messageType, payloadFormat, schemaId, payloadBytes}.
4. For each algorithm A in ActiveVersion.preferredSignatureAlgorithmSet:
       sig[A] = sign(A, K.privateKey, preimage)
5. Emit SignedEnvelope {
       messageType   = P.messageType(),
       payload       = payloadBytes,
       payloadFormat = ...,
       schemaId      = ...,
       signedBy      = K.id,
       signatures    = [Signature(A, sig[A]) for each A]
   }
```

Implementations MUST NOT re-serialize `P` between signing and transmission.

**Key-family constraint.** A Persona Version's signature algorithm sets MUST
be single-key-family: every algorithm in a set MUST be computable with the
signer's one certified private key and verifiable with the certified public
key. Sets that mix families (e.g. RSA + Ed25519 in one set) are NOT valid
under this version of the protocol; they require per-algorithm certified key
bindings (a future extension where the Registrar certifies a key descriptor
per family) and MUST be rejected until that extension is specified. Migration
across key families is instead performed by registering a new Persona
instance (§3.1) and crossing over via `validSignatureAlgorithmSets` that keep
the old family's single-algorithm sets valid during the transition.
Signatures cover the preimage (§6.4), which includes the payload bytes
exactly as they appear in the envelope.

### 7.3 Verification

Given an incoming envelope `E`:

```
1. Assert E is well-formed and its signatures set matches one of the
   ActiveVersion.validSignatureAlgorithmSets.
2. Resolve schema for E.messageType (and E.schemaId if present).
3. Deserialize E.payload → P using that schema. Assert P.isValid().
4. Recursively verify every nested SignedEnvelope / MultiSignedEnvelope
   found in fields or collections of P (fail-fast).
5. Assert P.canBeSignedBy(E.signedBy).
6. Resolve Persona for E.signedBy via the Resolver.
   - Assert the Persona's certificate is within its validity window.
   - Assert its certificate chains to a trusted Registrar root.
   - Assert the certificate is not revoked, per the Registrar's current
     revocation status (CRL, OCSP, or a signed revocation notice as per
     rule 11 below). Revocation status MUST be checked at verification
     time; a cached negative answer MUST respect the TTL/invalidation
     rules of §9.
7. Recompute the signing preimage (§6.4) from E's
   {messageType, payloadFormat, schemaId, payload}.
   For each Signature in E.signatures:
       assert verify(A.algorithm, Persona.publicKey, preimage, A.value)
8. Mark E as verified.
```

If any step fails, the envelope MUST be rejected. Verification results MAY be
cached, but the cache key MUST include `messageType`, a hash of the preimage
(or of the payload together with the type-binding fields), `signedBy`, the
signature set, and the thumbprint of the certificate that was used to verify.
Cache entries MUST be invalidated when the resolved certificate is revoked,
renewed, or otherwise changes trust state. Caching MAY instead be applied only
to the cryptographic sub-result (signature verification against a specific
certificate), with policy and certificate checks repeated per envelope.

### 7.4 Multi-Signature

Initial signing produces a `MultiSignedEnvelope` with one entry. Additional signers extend the `signatures` array. Verification runs independently against each entry. Completeness is a separate check driven by `hasSufficientSignatures()`.

Ordering of signatures MUST NOT affect verification outcome.

### 7.5 Nested Verification

The recursive verification of nested envelopes gives the protocol its power. Example: a payment authorization signed by Alice references an account envelope signed by Alice's bank. The recipient (a payee) never contacts the bank directly, yet gains cryptographic proof that:

- Alice authorized this payment.
- Alice's bank certified Alice's account.
- Both are current, uncompromised, and issued under the same Realm's Registrar.

The recipient's verification runtime does the whole traversal in one pass.

### 7.6 Alerts

Alerts flow over a broadcast substrate (mobile push, web push, MQTT, WebSocket, mesh). The substrate carries `{realm, alertType, alertId, receiverPersonaId}` as transport-level metadata. The full `SignedEnvelope` MAY be fetched separately (poll model) or attached inline (push model).

Recommended alert lifecycle:

- **Subscribe**: `SubscribeForAlertsRequest` — a signed request that binds a receiver Persona to a device token or subscription URL.
- **Deliver**: transport-specific push.
- **Poll**: `PendingAlertsRequest` — a signed request that fetches undelivered alerts.
- **Acknowledge**: `DeleteAlertsRequest` — a signed request that clears delivered alerts.

---

## 8. Cryptographic Policy (Persona Version)

Concrete algorithms are versioned as a group so peers can negotiate compatibility.

A **Persona Version** declares:

| Field                                 | Purpose                                                                                     |
| ------------------------------------- | ------------------------------------------------------------------------------------------- |
| `version`                             | Opaque version identifier (e.g. `v0`, `v1`).                                                |
| `validSignatureAlgorithmSets`         | List of accepted **sets**. An envelope's `Signature[]` MUST match one set exactly.          |
| `preferredSignatureAlgorithmSet`      | The set new signatures SHOULD use.                                                          |
| `validAsymmetricEncryptionAlgorithms` | Accepted algorithms for RSA/EC-style encryption.                                            |
| `preferredAsymmetricEncryptionAlgorithm` | Default choice.                                                                          |
| `validSymmetricEncryptionAlgorithms`  | Accepted symmetric encryption suites.                                                       |
| `preferredSymmetricEncryptionAlgorithm` | Default choice.                                                                          |
| `certificateSignatureAlgorithm`       | Algorithm used by the Registrar to sign certificates.                                       |
| `keyGenerationAlgorithm`              | Key family (e.g. `RSA`, `Ed25519`).                                                         |
| `keySize`                             | Bit length (or `0` for curve-typed families).                                               |
| `preferredHashAlgorithm`              | Used for thumbprints and passphrase hashes.                                                 |

Recommended `v0` baseline (from the reference codebase):

| Algorithm class          | Value                                    |
| ------------------------ | ---------------------------------------- |
| Signature                | `SHA256withRSA`                          |
| Asymmetric encryption    | `RSA/NONE/OAEPwithSHA-256andMGF1Padding` |
| Symmetric encryption     | `AES/CTR/NoPadding`                      |
| Key generation           | RSA / 2048                               |
| Hash                     | `SHA-256`                                |

A future `v1` SHOULD add Ed25519 for signatures and X25519 for asymmetric encryption while keeping RSA in `validSignatureAlgorithmSets` for backward compatibility.

---

## 9. Persistence and Resolution

The protocol requires two lookups at runtime:

### 9.1 Certificate Service

`getCertificate(id, thumbprint?)` → `Certificate?`
`getCertificates(id, thumbprint?)` → `[Certificate]`
`getCertificateChain(id, thumbprint?)` → `CertificateChain?`

Implementations SHOULD be layered:

```
Local cache (in-memory)
      │ miss
      ▼
Local durable store (DB)
      │ miss
      ▼
Remote Registrar or peer directory
```

Certificates SHOULD be indexed by both `id` (owner) and `serialNumber` (Registrar-assigned). Cached entries SHOULD respect the certificate's own validity window as an upper bound on cache TTL.

### 9.2 Persona Resolver

`resolvePersona(personaId)` → `Persona?`

Typically implemented as a thin wrapper over the Certificate Service that additionally attaches decoded public-key handles for signature verification.

Caching guidance: cache MUST be invalidated on certificate revocation events; if revocation events cannot be delivered, TTLs SHOULD be short (minutes-to-hours, not days).

---

## 10. Encoding Choices

The envelope is deliberately encoding-agnostic. In practice, three choices dominate:

| Property                          | JSON             | Avro binary          | Protobuf              |
| --------------------------------- | ---------------- | -------------------- | --------------------- |
| Wire size                         | Largest          | Smallest             | Small                 |
| Human debuggable                  | Yes              | No (schema needed)   | No (schema needed)    |
| Determinism for signatures        | Requires care    | Requires care        | Requires care         |
| Schema evolution                  | Manual           | Excellent            | Excellent             |
| Cross-language SDKs               | Universal        | Data-lake ecosystem  | RPC / mobile ecosystem|

**Critical rule for signature stability.** Regardless of encoding, an envelope MUST carry `payload` as an **opaque byte string** that was signed exactly once, before it was ever wrapped. Verifiers MUST NOT re-serialize the payload. This rule sidesteps every subtle non-determinism (map key ordering, whitespace, union tag choice, floating-point rendering).

Recommended path:

- Start with JSON for tooling and human readability.
- Move to Avro or Protobuf domain-by-domain, starting with high-volume, low-value traffic (alerts, status polls).
- Never mix encodings within a single signed envelope: `payloadFormat` on the envelope defines exactly one encoding.

---

## 11. Rules and Invariants (Normative)

1. Every `SignableMessage` MUST provide a stable `messageType` that is not tied to a programming-language class name.
2. `SignedEnvelope.signedBy` MUST satisfy `payload.canBeSignedBy(signedBy)`.
3. `SignedEnvelope.signatures` MUST match one of `activeVersion.validSignatureAlgorithmSets` exactly; supersets and subsets are rejected.
4. `SignableMessage.isValid()` MUST be checked both before signing and after deserialization.
5. `RequestMessage.requestId()` MUST be unique per (recipient, requester). Recipients MUST enforce this.
6. `AlertMessage.receivers()` MUST be non-empty. Transports MUST NOT strip `{realm, alertType, alertId}` from message metadata.
7. `PersonaId` equality is structural over `(id, thumbprint)`. Cross-instance signing is governed by `canSignOnBehalfOf`.
8. Certificates MUST chain to a trusted Registrar root and be within their validity window at verification time.
9. Nested envelopes MUST be verified before the outer payload is considered verified.
10. The bytes that were signed MUST be the exact bytes that are verified; no re-serialization at any layer. Signatures are computed over the signing preimage (§6.4), which authenticates the payload bytes together with the envelope's `messageType`, `payloadFormat`, and `schemaId`.
11. Revocation of a Persona MUST invalidate cached resolutions; verifiers SHOULD accept a signed-and-timestamped revocation notice from the Registrar or from the Persona owner (authenticated by the pre-registered passphrase hash).
12. A Realm boundary MUST NOT be crossed: envelopes signed under one Realm's Registrar MUST NOT be accepted by peers of another Realm.

---

## 12. Security Considerations

- **Registrar compromise** is catastrophic. Mitigate with: transparency logs, out-of-band root distribution, short-lived intermediate CAs, HSM-backed root keys, quorum-controlled issuance.
- **Private key exfiltration** is the Persona owner's failure mode. Mitigate with: hardware key stores, encrypted-at-rest key material, revocation on suspicion.
- **Downgrade attacks** on the algorithm set: peers MUST reject any signature set that does not exactly match one of the sets declared in `validSignatureAlgorithmSets`. An empty set `{}` is never acceptable. A proper subset of the preferred set is acceptable only when that subset is itself explicitly listed in `validSignatureAlgorithmSets` (e.g. an RSA-only set retained during an RSA→RSA+Ed25519 migration).
- **Replay attacks** on request messages: `requestId` de-duplication mitigates for exactly-once semantics; timestamp claims inside payloads mitigate for freshness.
- **Signature malleability** in some algorithms (e.g. ECDSA `s` malleability): implementations SHOULD enforce low-`s` normalization.
- **Nonce/IV reuse** in symmetric encryption: implementations MUST use fresh cryptographically random IVs per encryption; the envelope carries the IV explicitly.
- **Log leakage of payloads**: payloads MAY contain PII (hashed or clear). Logs SHOULD scrub before writing; the reference codebase excludes sensitive fields from `toString`.
- **Message-type squatting**: `messageType` values SHOULD be namespaced by an authority the peers trust (`payments.PaymentAuthorization@v2`, not `PaymentAuthorization`).

---

## 13. Reference Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│                         Persona Client                            │
│                                                                   │
│  ┌────────────┐   ┌──────────────┐   ┌────────────────────────┐  │
│  │ KeyStore   │   │ MessageSigner│   │ MessageVerifier        │  │
│  │  (private) │   │              │   │  ├─ SchemaRegistry     │  │
│  └────────────┘   └──────────────┘   │  ├─ PersonaResolver    │  │
│                          │            │  └─ CertificateService │  │
│                          ▼            └────────────────────────┘  │
│                     ┌──────────┐              │                   │
│                     │ Envelope │              │                   │
│                     └──────────┘              │                   │
└──────────────────────────┬────────────────────┼───────────────────┘
                           │                    │
                     over transport             │
              (HTTP, gRPC, MQTT, QR, mesh)      │
                           │                    │
┌──────────────────────────┼────────────────────┼───────────────────┐
│                         Persona Peer / Service                    │
│                          │                    │                   │
│                          ▼                    ▼                   │
│                    ┌──────────┐        ┌──────────────┐          │
│                    │ Envelope │        │ RequestDedup │          │
│                    └────┬─────┘        └──────────────┘          │
│                         │                                         │
│                         ▼                                         │
│                    MessageVerifier                                │
│                         │                                         │
│                         ▼                                         │
│                    Domain Handler                                 │
└───────────────────────────────────────────────────────────────────┘

                    ┌──────────────────┐
                    │   Registrar CA   │
                    │  (issue / revoke)│
                    └──────────────────┘
```

Every component above is language-agnostic and independently implementable.

---

## 14. Reusability Checklist

To adopt the Persona Protocol in a new project:

1. Choose a **Realm** identifier and stand up a **Registrar** (or reuse an existing one).
2. Pick an initial **Persona Version** (signature/encryption algorithms). Start with `v0` above.
3. Decide the **payload encoding** (JSON to bootstrap, Avro/Protobuf later).
4. Define a **message-type namespace** for your domain (`myproject.something.Thing@v1`).
5. Implement or import:
   - Key generation / PEM encoding.
   - CSR construction and Registrar HTTP client (`POST /persona-id`, `POST /persona`).
   - `SignedEnvelope` codec (encode + decode + opaque payload handling).
   - `MessageSigner` and `MessageVerifier` with recursive nested-envelope traversal.
   - `PersonaResolver` with caching.
   - `RequestService` for `requestId` de-duplication (server-side).
6. Model each of your domain records as a `SignableMessage` implementation with a stable `messageType`, a `signer()`, and an `isValid()`.
7. For multi-party workflows, use `MultiSignableMessage` + quorum policies.
8. For asynchronous notification, layer `AlertMessage` on your push substrate.
9. Instrument verification failures — they are the earliest indicator of protocol drift or compromise.

---

## 15. Glossary

- **CA (Certificate Authority)** — Synonym for Registrar in this document.
- **CSR (Certificate Signing Request)** — A PKCS#10 structure sent to the Registrar to obtain a signed certificate.
- **Envelope** — A `SignedEnvelope` or `MultiSignedEnvelope` carrying a payload and its signatures.
- **Payload** — The domain record inside an envelope, signed as opaque bytes.
- **Persona** — The pseudonymous identity primitive of this protocol.
- **Persona ID** — `<id>` or `<id>#<thumbprint>`.
- **Persona Version** — The versioned crypto policy for a Realm.
- **Realm** — A named deployment scope.
- **Registrar** — The Realm's CA.
- **Resolver** — Persona-ID → Persona lookup service.
- **Signable / MultiSignable / Alert / Request / Addressable Message** — The five orthogonal message contracts (§5).
- **Thumbprint** — A cryptographic fingerprint of a certificate's DER encoding.

---

## Appendix A. Non-normative examples

### A.1 A minimal domain message

```
messageType : "example.Greeting@v1"

Greeting {
    greetingId : string          // stable id
    from       : PersonaId       // signer
    to         : PersonaId       // recipient
    body       : string
    issuedAt   : timestamp
}
```

- `signer()` returns `from`.
- `isValid()` requires non-empty `greetingId` and `body`, valid `from`/`to`, and `issuedAt` within a reasonable clock skew.
- Envelope on the wire: a `SignedEnvelope` with `messageType = "example.Greeting@v1"`, opaque `payload` bytes, `signedBy = from`.

### A.2 A cross-party workflow using nested envelopes

```
messageType : "example.Endorsement@v1"

Endorsement {
    endorsementId  : string
    originalOffer  : SignedEnvelope      // Offer signed by the offerer's bank
    endorsedBy     : PersonaId           // signer of THIS envelope
    endorsedAmount : Amount
}
```

Verification of a delivered `Endorsement` envelope produces cryptographic proof that:

- `endorsedBy` endorsed the outer amount.
- The bank Persona referenced in `originalOffer.signedBy` certified the terms.

Neither party needs live access to the other's servers to check this.

### A.3 A multi-party quorum

```
messageType : "example.Charter@v1"

Charter {
    charterId : string
    founders  : [PersonaId]              // 3 founders
    minSigs   : int = 2                  // any 2 of 3
    terms     : string
}
```

`validSigners() = founders`, `minimumRequiredSignatures() = 2`. Delivered inside a `MultiSignedEnvelope` with two of the three founders' signatures, the envelope is considered "complete".

---
