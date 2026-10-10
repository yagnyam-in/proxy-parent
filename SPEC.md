# Persona Framework Specification

> A specification derived from analyzing the `proxy-parent` codebase (modules `proxy-core-lib`, `proxy-messages`, `proxy-server-lib`). The historic term **Proxy** used throughout the code base is renamed to **Persona** here to more accurately convey its intent: a cryptographic pseudo‑identity that a real‑world entity (individual, bank, identity provider, escrow agent, service) uses to transact without exposing its underlying identity.

---

## 1. Terminology Mapping (Proxy → Persona)

The following renaming is proposed uniformly. Wherever the code uses the token `Proxy`, replace with `Persona`.

| Legacy (code)                 | Proposed (spec)                    | Meaning                                              |
| ----------------------------- | ---------------------------------- | ---------------------------------------------------- |
| Proxy                         | Persona                            | Pseudonymous identity backed by an X.509 certificate |
| ProxyId                       | PersonaId                          | Unique addressable identifier for a Persona          |
| ProxyKey                      | PersonaKey                         | Persona’s key pair (private + public)                |
| ProxyRequest                  | PersonaRequest                     | Local CSR + revocation data prior to registration    |
| ProxyCreationRequest/Response | PersonaCreationRequest/Response    | Wire messages exchanged with the registrar           |
| ProxyIdRequest/Response       | PersonaIdRequest/Response          | Assign a new unique Persona identifier               |
| Proxy Central                 | Persona Registrar (Central CA)     | Root authority issuing Persona certificates          |
| Proxy Universe                | Realm                              | Deployment scope (e.g. `production`)                 |
| Proxy Resolver                | Persona Resolver                   | Service that resolves a `PersonaId` → `Persona`      |
| ProxyBaseObject               | ValidatableObject                  | Marker interface for objects with `isValid()`        |
| ProxyAccount                  | PersonaAccount                     | Bank account tied to a Persona                       |
| ProxySubject                  | PersonaSubject                     | Identity assertion issued for a Persona              |
| ProxyWallet                   | PersonaWallet                      | Wallet owned by a Persona                            |
| SignableMessage               | SignableMessage (unchanged)        | Any payload a Persona can sign                       |
| MultiSignableMessage          | MultiSignableMessage (unchanged)   | Payload requiring signatures from multiple Personas  |
| SignableAlertMessage          | SignableAlertMessage (unchanged)   | Push/alert message addressed to Personas             |
| SignableRequestMessage        | SignableRequestMessage (unchanged) | Signed message that carries a `requestId`            |
| AddressableMessage            | AddressableMessage (unchanged)     | Message with a target Persona `address()`            |

The rest of this document uses the **Persona** vocabulary.

---

## 2. High‑Level Overview

The framework provides a **PKI‑based identity + messaging protocol** so any two parties can:

1. Register a **Persona** (a stable pseudonymous identity backed by an X.509 certificate signed by a central registrar).
2. Exchange **strongly typed JSON messages** that are:
   - Digitally signed by one or more Personas.
   - Serialized in a deterministic, wrapped envelope (`SignedMessage` / `MultiSignedMessage`) that also carries the payload type, signer(s), and signature algorithm(s).
   - Verified end‑to‑end against the signer’s certificate chain, before consumption.
3. Compose messages hierarchically — a `SignedMessage` may embed other `SignedMessage`s (e.g. a `Payment` embedding a `PersonaAccount`), and children are recursively verified.
4. Address messages to a target Persona (`AddressableMessage`) or broadcast alerts to a set of receivers (`SignableAlertMessage`).

The framework is deliberately domain‑neutral in the core, then instantiated by the `proxy-messages` module with concrete domains: banking, payments, escrow, identity, authorization, alerts, geo, and offline money.

Layering:

```
proxy-messages   ── domain payload types (banking, identity, escrow, …)
        │
        ▼
proxy-core-lib   ── framework: Persona, PKI, signing, verification, resolution
        ▲
        │
proxy-server-lib ── server‑side plumbing: entities, repositories, request de‑dup,
                    servlet utils, exception hierarchy, JWT verification adapters
```

---

## 3. Module: `proxy-core-lib`

### 3.1 Core Identity Objects

#### 3.1.1 `Persona` (was `Proxy`)
Fields (`in.yagnyam.proxy.Proxy`):

- `id: PersonaId` — required.
- `name: String` — human‑readable label (mirrors the certificate `owner`).
- `certificate: Certificate` — required.
- `publicKeyEncoded: String` — PEM‑encoded public key.
- `publicKeySha256Thumbprint: String`.

Factory: `Persona.of(Certificate)` derives `id`, `name`, and key thumbprint from the certificate.

Semantics: A Persona is authoritative because its certificate is authoritative. Two Personas with the same `id` but different certificate thumbprints are distinct instances (e.g. after re‑keying), and can be disambiguated via `PersonaId.sha256Thumbprint`.

#### 3.1.2 `PersonaId` (was `ProxyId`)
Format: `<id>` or `<id>#<sha256Thumbprint>` (SHA‑256 of the certificate).

Rules:
- Constructed via `PersonaId.of(String)`, `PersonaId.of(id, thumbprint)`, or `PersonaId.of(Certificate)`.
- `PersonaId.any()` = wildcard token `any#any` used where any signer is acceptable.
- `canSignOnBehalfOf(other)`: true when `other` is wildcard, when this shares `id` and `other` has no thumbprint pin, or when equal.
- Serialized as `uniqueId()` which is `id` or `id#thumbprint`.

#### 3.1.3 `Certificate`
X.509 certificate wrapper containing serial, owner, sha256Thumbprint, alias, subject, validity window, PEM encoded certificate + public key, and issuer references. `getId()` returns `owner` — i.e. the Persona identifier.

#### 3.1.4 `CertificateChain`, `Certificates`
Aggregates for CA chain walks and bulk lookups.

#### 3.1.5 `PersonaKey` (was `ProxyKey`)
Holds the local secret material for a Persona (never transmitted):
- `id: PersonaId`
- `privateKeyEncoded: String` **or** `privateKeyEncodedEncrypted: CipherText`
- `publicKeyEncoded`, thumbprints
- Transient: decoded `PublicKey` / `PrivateKey` handles

A `PersonaKey` is valid when it carries either a plain PEM‑encoded private key or an encrypted variant.

### 3.2 Cryptographic Primitives

- `Hash { algorithm, iv, hash }` — salted hash (IV acts as salt); verified by re‑hashing with the same IV.
- `Hmac { algorithm, iv, hmac }` — keyed MAC.
- `CipherText { encryptionAlgorithm, iv?, cipherText, hmacAlgorithm?, hmac? }` — envelope for symmetric or asymmetric ciphertext. IV optional for RSA/OAEP.

### 3.3 Configuration

`PersonaVersion` (was `ProxyVersion`) — versioned crypto policy interface. `PersonaVersionV0` (was `ProxyVersionV0`) currently ships:

| Concern                       | Value                                     |
| ----------------------------- | ----------------------------------------- |
| Version                       | `v0`                                      |
| Signature algorithms          | `SHA256WithRSAEncryption`                 |
| Asymmetric encryption         | `RSA/NONE/OAEPwithSHA-256andMGF1Padding`  |
| Symmetric encryption          | `AES/CTR/NoPadding`                       |
| Certificate signature         | `SHA256WithRSAEncryption`                 |
| Key algorithm / size          | `RSA` / 2048                              |
| Preferred hash                | `SHA256`                                  |

Note: the field is a `List<Set<String>>` because a single logical signature may be composed of multiple algorithms (e.g. ECC + RSA together in future versions).

### 3.4 Message Abstractions

#### 3.4.1 `SignableMessage`
Contract for anything a single Persona can sign.
- `signer(): PersonaId` — the canonical signer.
- `validSigners(): Set<PersonaId>` — defaults to `{signer()}`, but may include delegates.
- `getMessageType(): String` — defaults to `getClass().getName()`. Used to serialize/deserialize via reflection.
- `isValid(): boolean` — business rule check.
- `cabBeSignedBy(id)` — true if `id` `canSignOnBehalfOf` any valid signer. **(Typo `cabBe*` should be corrected to `canBe*` when renaming.)**

#### 3.4.2 `SignableRequestMessage extends SignableMessage, RequestMessage`
Adds `requestId(): String` for idempotency and de‑duplication on the server.

#### 3.4.3 `AddressableMessage`
Contract for messages targeted at a Persona: `address(): PersonaId`.

#### 3.4.4 `SignableAlertMessage extends SignableMessage`
Alerts / push messages:
- `proxyUniverse()` → **`realm()`**.
- `alertId(): String`.
- `receivers(): List<PersonaId>`.
- `toFcmMap(): Map<String,String>` — minimal FCM payload with `realm`, `alertType`, `alertId`, `receiverPersonaId`.

#### 3.4.5 `MultiSignableMessage`
Contract for messages requiring signatures from multiple Personas:
- `validSigners(): Set<PersonaId>`.
- `minimumRequiredSignatures(): int`.
- `hasSufficientSignatures(Set<PersonaId>)`.

#### 3.4.6 Signed envelopes

`SignedMessage<T extends SignableMessage>`:
- `message: T` (transient, populated after deserialize).
- `type: String` — fully qualified class name of the payload.
- `payload: String` — canonical JSON of the payload.
- `signedBy: PersonaId`.
- `signatures: List<SignedMessageSignature>` (one per algorithm).
- `verified: boolean` (transient).

`SignedMessageSignature { algorithm, value }`.

`MultiSignedMessage<T extends MultiSignableMessage>`:
- Similar but `signatures: List<MultiSignedMessageSignature>`, each of which groups one signer’s per‑algorithm signatures.
- `hasSufficientSignatures()` delegates to the payload’s policy.

### 3.5 Services

#### 3.5.1 `MessageSerializerService`
Jackson‑based JSON serializer/deserializer with:
- `NON_NULL` inclusion.
- `JavaTimeModule` (ISO date strings).
Contracts: `serializeSignableMessage`, `deserializeSignedMessage`, `deserializeSignableMessage(class)`, and multi‑variant equivalents.

#### 3.5.2 `MessageSigningService`
Given a `PersonaKey`, produces a `SignedMessage<T>` or `MultiSignedMessage<T>` by:
1. Validating the payload.
2. Confirming the signer is authorized (`cabBeSignedBy` / `validateSigners`).
3. Serializing the payload once.
4. Computing one signature per configured algorithm in the preferred set.

Also supports **incremental multi‑signature**: add another Persona’s signature to an existing `MultiSignedMessage`.

#### 3.5.3 `MessageVerificationService`
- Verifies each `SignedMessageSignature` against the signer’s `Persona` (resolved via `PersonaResolver`) using the configured algorithms.
- For `MultiSignedMessage`, verifies every signer independently.
- Rejects unknown algorithm sets (must match `PersonaVersion.getValidSignatureAlgorithmSets()`).
- Marks the envelope as `verified=true` on success.

#### 3.5.4 `MessageBuilder`
Rehydrates a `SignedMessage` from JSON:
1. Deserialize outer envelope.
2. Resolve payload `Class` from `type`.
3. Deserialize payload into that class.
4. Recursively walk payload fields; for any nested `SignedMessage` / `MultiSignedMessage` field or collection element, rehydrate them too (single level of recursion is invoked; the recursive call re‑enters `populate*`).
5. Validate the payload’s `isValid()`.

#### 3.5.5 `MessageFactory`
Convenience façade that combines `MessageBuilder` + `MessageVerificationService`:
- `buildSignedMessage`, `buildAndVerifySignedMessage`, and multi‑signed variants.
- `verifyChildMessages(msg)` — reflectively walks fields and verifies nested envelopes.

Design note: reflective walking is a light‑weight alternative to Jackson polymorphism annotations; downside is it operates on declared fields and does not recurse into arbitrary containers. This is acceptable given current message shapes.

#### 3.5.6 `PersonaResolver` (was `ProxyResolver`)
`Optional<Persona> resolvePersona(PersonaId)`.

Implementations:
- `SimplePersonaResolver` — delegates to `CertificateService` and wraps the certificate.
- `RemotePersonaResolver` — HTTP GET `<root>/<id>?sha256Thumbprint=<thumb>`.
- `CacheBackedPersonaResolver` — Guava‑cache wrapper (size + TTL).

#### 3.5.7 `CertificateService`
`getCertificate(id, thumbprint)`, `getCertificates(id, thumbprint)`, `getCertificateChain(id, thumbprint)`.

Implementations:
- `RemoteCertificateService` — HTTPS calls to `<caRoot>/certificate`, `/certificates`, `/certificate-chain` with optional `?sha256Thumbprint=…`.
- `CacheBackedCertificateService` — caches by both `owner` and `serialNumber`, keyed by `certificateId` with SHA‑256 predicate filter.

#### 3.5.8 `PemService`
PEM encode/decode for `PublicKey`, `PrivateKey`, `X509Certificate`, and PKCS#10 `CertificationRequest`. Registers BouncyCastle provider on load.

#### 3.5.9 `CryptographyService` + `BcCryptographyService`
Facade over signing, verification, hashing, HMAC, and symmetric/asymmetric encryption using BouncyCastle. Also carries a static `asAesKey` helper.

#### 3.5.10 `CertificateRequestService` + `BcCertificateRequestService`
Builds a PKCS#10 CSR (`X500Principal(CN=<personaId>)`) using a given signature algorithm and key pair.

#### 3.5.11 `NetworkService`
Thin wrapper over Google HTTP Client:
- Timeouts, default headers, JSON marshalling using `MessageSerializerService`.
- `HttpResponse` autoclosable wrapper exposing `getContent(failIfFailure)`.
- Convenience methods: `get`, `getValue`, `postValueWithHeaders`, `postAndGetValueWithHeaders`, `postEmptyAndGetValueWithHeaders`, etc.

### 3.6 Persona Bootstrap Flow

Packages under `in.yagnyam.proxy.bootstrap` (rename to `in.yagnyam.persona.bootstrap`):

Sequence to obtain a Persona:

```
Client                                        Persona Registrar
  │                                                     │
  │  PersonaIdRequest { requestId }                     │
  ├────────────────────────────────────────────────────►│
  │                                       PersonaIdResponse { requestId, personaId }
  │◄────────────────────────────────────────────────────┤
  │  generateKeyPair() -> PersonaKey                    │
  │  buildCsr(personaId, keyPair)                       │
  │  hashRevocationPassPhrase()                         │
  │                                                     │
  │  PersonaCreationRequest {                           │
  │     requestId,                                      │
  │     personaId,                                      │
  │     revocationPassPhraseHash,                       │
  │     certificateRequestEncoded (PEM CSR)             │
  │  }                                                  │
  ├────────────────────────────────────────────────────►│
  │                             PersonaCreationResponse (SignedMessage<…> or bare)
  │◄────────────────────────────────────────────────────┤
```

Components involved:
- `PersonaIdFactory` – calls `POST /persona-id`, returns fresh `personaId`.
- `PersonaKeyFactory` / `BcPersonaKeyFactory` – generates RSA key pair.
- `PersonaRequestFactory` / `BcPersonaRequestFactory` – produces a local `PersonaRequest` bundle: `{id, revocationPassPhraseHash, requestEncoded}`.
- `PersonaFactory` – posts the `PersonaCreationRequest`, accepts either a `SignableMessage` response or a signed envelope (`SignedMessage<PersonaCreationResponse>`), and returns the resulting `Persona`.

The default registrar URLs are `https://cs.pxy.yagnyam.in/persona-id` and `.../persona`; these must be configurable in the renamed framework.

### 3.7 Authentication (JWT)

`in.yagnyam.proxy.authentication`:
- `AuthenticationToken` — POJO covering `keyId`, `issuer`, `audiences`, `subject`, `expirationTime`, and typed attribute maps (string / string list / date).
- `AuthenticationTokenService` — signs `AuthenticationToken` into a compact JWS (`RS256`, `x5t#S256` header set from the signing cert), and parses/validates JWTs given an expected audience.
- `CertificateVerificationKeyResolver` — jose4j `VerificationKeyResolver` that looks up the certificate by JWT `kid` (= certificate serial number) via `CertificateResolver`.
- `CertificateResolver` / `RemoteCertificateResolver` — resolves an `X509Certificate` by serial number, with in‑process LoadingCache.

### 3.8 Constants and Services Catalog

- `Constants.PROXY_CENTRAL = "proxy-central"` → **`Constants.PERSONA_REGISTRAR = "persona-registrar"`**.
- `Services` — currently exposes `BANKING`, `IDENTITY` as offered service types. Should be extended (and converted to an enum) to include `PAYMENTS`, `ESCROW`, `ALERTS`, `AUTHORIZATION`, `WALLET`, `GEO`, `SACK`.
- `ProxyUniverse.PRODUCTION` → **`Realm.PRODUCTION`**.

### 3.9 Utilities

`in.yagnyam.proxy.utils`:
- `StringUtils`, `DateUtils`, `CollectionUtils`, `RandomUtils` — small helpers.
- `EncodingUtils`, `JsonUtils` — encoding/JSON helpers.
- `CertificateUtils` — populates transient `X509Certificate` from PEM.
- `ProxyUtils` (→ `PersonaUtils`) — enriches Persona with decoded certificate, null‑safe `isValid()`.
- `Throwing`, `Errors` — checked exception wrappers.

---

## 4. Module: `proxy-messages`

Domain payloads that implement `SignableMessage`, `MultiSignableMessage`, `AddressableMessage`, `SignableAlertMessage`, or `SignableRequestMessage`. Grouped by domain.

### 4.1 Banking (`in.yagnyam.proxy.messages.banking`)

Core objects and enums:
- `Currency` — set of ISO codes: `INR, EUR, USD, GBP, AUD, JPY` (should become enum).
- `Amount { currency, value }` with `add`, `subtract`, `compareTo`, currency‑guarded arithmetic.
- `PersonaAccount` (was `ProxyAccount`) — signed by the issuing bank Persona; carries `personaAccountId`, `ownerPersonaId`, validity window, `currency`, `permissions[]`, `maximumAmountPerTransaction`.
- `PersonaAccountId` — `{ realm, bankPersonaId, accountId }`.
- `ProxyAccountPermissionEnum` (→ `AccountPermission`) — `InquireAccountDetails`, `InquireBalance`, `AuthorizeProxy`, `MakePayment`.
- `NonProxyAccount` — external (real) bank account for withdrawal endpoints; fields include `bank`, `accountNumber`, `accountHolder`, `currency`, optional `ifscCode`, `email`, `phone`, `address`.
- `BankingServiceProvider` — advertises a bank Persona: `proxyId`, `realm`, `supportedCurrencies`, `apiEndpoint`, `name`; signed by `persona-banking` root.

Wallet:
- `wallet.ProxyWalletCreationRequest` / `Response` — create a `PersonaWallet` for a given owner + bank + currency.

Requests/responses:
- `AccountBalanceRequest/Response`, `AccountDetailsRequest/Response`.
- `CreateProxyAccountRequest`.
- `DepositRequest` (embeds signed `PersonaAccount`), `DepositRequestCreationRequest/Response`, `DepositRequestStatusRequest/Response`, `DepositRequestCancelRequest/Response`, `DepositStatusEnum`.
- `Withdrawal` (embeds signed `PersonaAccount`, targets a `NonProxyAccount`), `WithdrawalResponse`, `WithdrawalStatusRequest/Response`, `WithdrawalStatusEnum`.
- `SignedProxyAccounts` — bulk envelope.

Bunq subdomain:
- `banking.bunq.AuthorizeBunqAccountRequest/Response` — link a Bunq account.

Alerts (`banking.alerts`):
- `AccountUpdatedAlert`, `DepositUpdatedAlert`, `WithdrawalUpdatedAlert` — implement `SignableAlertMessage`; enrich `toFcmMap` with account/bank details.

### 4.2 Payments (`in.yagnyam.proxy.messages.payments`)

- `PaymentRequest` — payee side; embeds signed `PersonaAccount`, `Amount`, optional `description`, `transaction`.
- `PaymentAuthorization` (SignableRequestMessage) — payer side; embeds payer’s signed `PersonaAccount`, an `Amount`, and a list of `Payee`.
- `Payee { paymentEncashmentId, payeeType, proxyId?, emailHash?, phoneHash?, secretHash? }` — polymorphic payee identifier.
- `PayeeTypeEnum` — `ProxyId`, `Email`, `Phone`, `AnyoneWithSecret`.
- `PaymentEncashment` (`AddressableMessage`) — payee’s claim of a `PaymentAuthorization`, embedding payee’s signed `PersonaAccount`; `secret` required unless `payeeType == ProxyId`.
- `PaymentConfirmation` — record of settled encashment.
- `PaymentAuthorizationRegistered`, `PaymentEncashmentRegistered`.
- `PaymentAuthorizationStatusRequest/Response`, `PaymentEncashmentStatusRequest/Response`.
- `PaymentAuthorizationStatusEnum`, `PaymentEncashmentStatusEnum`.
- `PaymentForPaymentRequest` — a payment made against a `PaymentRequest`.

Alerts (`payments.alerts`):
- `PaymentAuthorizationUpdatedAlert`, `PaymentEncashmentUpdatedAlert`.

Offline (`payments.offline`):
- `OfflineMoney` — signed by a bank Persona; establishes a spendable ceiling for the owner Persona (id, validity, `maxAmount`).
- `OfflinePayment` — signed by payer; wraps a signed `PaymentRequest` + set of signed `OfflineMoney` totalling ≥ the amount.
- `BuyOfflineMoneyRequest/Response`.

### 4.3 Escrow (`in.yagnyam.proxy.messages.escrow`)

- `EscrowAccount` — signed by bank; carries payer/payee/escrow Persona ids, `balance`, `title`.
- `EscrowAccountId { realm, bankPersonaId, accountId }`.
- `EscrowAccountStatusEnum`.
- `EscrowAccountCreationRequest/Response`, `EscrowAccountDetailsRequest/Response`.

Negotiation (`escrow.negotiation`):
- `EscrowAccountProposal` — payer proposes escrow (amount, title, bank).
- `EscrowAccountProposalPayeeAck` — payee acknowledges.
- `EscrowAccountProposalEscrowAck` — escrow agent acknowledges.
- `EscrowAccountProposalStatus`.

Alerts (`escrow.alerts`):
- `EscrowAccountUpdatedAlert`.

### 4.4 Identity (`in.yagnyam.proxy.messages.identity`)

- `PersonaSubject` (was `ProxySubject`) — an identity assertion issued by an Identity Provider Persona to a Persona holder, targeted to a Relying Party Persona (`relyingPartyId`, `relyingPartyProxyId`).
- `PersonaSubjectId { identityProviderProxyId, subjectId }` (Rename → `PersonaSubjectId { identityProviderPersonaId, subjectId }`).
- `SubjectIdentity` — signed by identity provider; contains full `SubjectDetails` + validity window.
- `SubjectDetails` — a map of `SubjectFieldEnum` → typed values.
- `SubjectFieldEnum { subjectIdType, nationality, aadhaarNumber, name, gender, age, is18Plus, dateOfBirth }` with `SubjectFieldTypeEnum { STRING, NUMBER, BOOLEAN, DATE }`.
- `SubjectIdTypeEnum`.
- `CreateProxySubjectRequest`.
- `PersonaSubjectDetailsRequest/Response`.
- `IdentityServiceProvider` — Persona advertised by `persona-identity` root.

Aadhaar sub‑flow (`identity.aadhaar`):
- `AadhaarVerificationRequest` → `AadhaarVerificationChallenge` → `AadhaarVerificationChallengeResponse`. All `SignableRequestMessage` and `AddressableMessage`.

Self‑declared (`identity.self`):
- `IdentityVerificationRequest`.

### 4.5 Authorization (`in.yagnyam.proxy.messages.authorization`)

Purpose: prove control of an email/phone number and bind that channel to a Persona.

- `EmailAuthorizationRequest`, `EmailAuthorizationChallenge`, `EmailAuthorizationChallengeResponse`, `EmailAuthorization`.
- `PhoneNumberAuthorizationRequest`, `PhoneNumberAuthorizationChallenge`, `PhoneNumberAuthorizationChallengeResponse`, `PhoneNumberAuthorization`.
- `SendSecretsRequest` (routed via a router Persona) — pushes hashed secrets to email or phone recipients, used by the payments “Email/Phone/AnyoneWithSecret” encashment flow.
- `SendSecretsResponse`.
- `FetchSecretForEmailRequest/Response`, `FetchSecretForPhoneNumberRequest/Response` — retrieve the secret for a verified channel.

### 4.6 Alerts (`in.yagnyam.proxy.messages.alerts`)

Cross‑domain alert transport:
- `SubscribeForAlertsRequest/Response` — registers a Persona + device (FCM token) with an alert provider Persona.
- `PendingAlertsRequest/Response` — poll pending alerts.
- `DeleteAlertsRequest/Response` — acknowledge / clear.

### 4.7 Geo (`in.yagnyam.proxy.messages.geo`)

- `GeoPoint { latitude, longitude }`.
- `Circle` — center + radius region.
- `FindCircleRequest/Response`, `FindCircleSession`, `FindCircleSessionId`.
- Alert: `CircleFoundAlert`.

### 4.8 Sack (`in.yagnyam.proxy.messages.sack`)

“Money sack” — a portable balance container.
- `MoneySack` — signed by a sack‑provider Persona; owner + balance + validity.
- `MoneySackId { realm, sackProviderPersonaId, sackId }`.
- `CreateMoneySackRequest`.

### 4.9 Mobile (`in.yagnyam.proxy.messages.mobile`)

Compact envelopes for mobile intents:
- `IdentityRequest` — request a `SubjectIdentity`.
- `PaymentRequest` (`REQUEST_PAYMENT_ACTION` = `in.yagnyam.proxy.REQUEST_PAYMENT`) — trigger a payment intent to a payee.

### 4.10 Bunq (`in.yagnyam.proxy.messages.bunq`)

- `AccountAuthorizationRequest` — bind a Bunq `apiToken` to a Persona.

---

## 5. Module: `proxy-server-lib`

Server‑side plumbing used by any service that implements a Persona domain.

### 5.1 Exceptions

`ServiceException` (RuntimeException with `HttpStatus`) — factories create specific subtypes:
- `BadRequestException` (400)
- `NotFoundException` (404)
- `UnauthorizedException` (401)
- `ForbiddenException` (403)
- `InternalServerErrorException` (500)

Uses Spring `HttpStatus` for status code carrying.

### 5.2 Utilities

- `DataValidation` — guard methods (`assertNotEmpty`, `assertNotNull`, `assertValidSerialNumber`) that throw `BadRequestException`.
- `IdGenerator` — Objectify‑backed monotonic id allocator (`allocateId`, `allocateIds`).
- `ServletUtils` — safe casts and header extraction from `ServletRequest`.

### 5.3 Persistence (`server.db`, `server.model`)

Google Objectify (App Engine Datastore) based.

Entities:
- `CertificateEntity` — @Id `serialNumber`; indexed `owner`, `sha256Thumbprint`; validity window; PEM encoded certificate; issuer refs.
- `PrivateKeyEntity` — @Id `id`; owner metadata (name, email, phone, address); indexed `certificateSerialNumber`, `certificateSha256Thumbprint`; encrypted‑ready `revocationPassPhrase`; PEM private key + certificate; transient decoded `PrivateKey` + `X509Certificate`.
- `RequestEntity` — @Id `requestId`; `requestType`, `creationTime`, `expiryTime`, `requestPayload` — used for de‑duplication of incoming requests.

Repositories:
- `CertificateRepository` (interface) + `DataStoreCertificateRepository` (implementation).
- `PrivateKeyRepository` — CRUD for `PrivateKeyEntity`.
- `RequestRepository` — CRUD for `RequestEntity`.

### 5.4 Services

- `DbBackedCertificateService` — implements core `CertificateService`. Read path: local DB by owner → local DB by serial → remote → cache the result. Uses `PemService` to hydrate `X509Certificate` before returning.
- `PrivateKeyService` — fetches `PrivateKeyEntity`, decodes PEM certificate and private key on the fly.
- `RequestService` — records incoming requests for idempotency; `assertNewRequest(requestId, requestType)` throws `BadRequest` on duplicate.
- `VerificationKeyResolverAdapter` — wires the JWT verification chain against `CertificateService`.

---

## 6. End‑to‑End Message Lifecycle

Signing (client side):

```
payload  ──► MessageSigningService.singleSign(payload, PersonaKey)
              │
              ├─ validate payload.isValid()
              ├─ ensure PersonaKey.id canSignOnBehalfOf payload.validSigners()
              ├─ serialize payload  → canonical JSON
              ├─ for each algorithm in PersonaVersion.preferredSignatureAlgorithmSet:
              │      signature = crypto.getSignature(alg, priv, json)
              └─ emit SignedMessage { type, payload, signedBy, signatures }
```

Verification (peer side):

```
raw JSON ──► MessageBuilder.buildSignedMessage(raw, ExpectedClass)
              ├─ deserialize envelope
              ├─ resolve payload class from `type`
              ├─ deserialize payload
              ├─ walk fields → rehydrate nested SignedMessage/MultiSignedMessage
              └─ payload.isValid()

          ──► MessageVerificationService.verify(signedMessage)
              ├─ enforce algorithm set matches PersonaVersion policy
              ├─ resolvePersona(signedBy) via PersonaResolver
              ├─ crypto.verifySignature(alg, cert, payload, value) per signature
              └─ set verified=true

MessageFactory chains both, and recursively verifies nested envelopes.
```

Multi‑signing:

```
initiate: MultiSignedMessage m = signer.multiSign(payload)
add:      m = signer2.multiSign(m)
verify:   verifier.verify(m)  → each MultiSignedMessageSignature validated
policy:   m.hasSufficientSignatures() = payload.hasSufficientSignatures(actualSigners)
```

---

## 7. Wire Envelope (JSON Shape)

`SignedMessage`:

```json
{
  "type": "in.yagnyam.persona.messages.banking.PersonaAccount",
  "payload": "<canonical JSON of the PersonaAccount>",
  "signedBy": "yagnyam-bank#abc123…",
  "signatures": [
    { "algorithm": "SHA256WithRSAEncryption", "value": "<base64>" }
  ]
}
```

`MultiSignedMessage`:

```json
{
  "type": "...",
  "payload": "…",
  "signatures": [
    {
      "signedBy": "alice#…",
      "signatures": [{ "algorithm": "SHA256WithRSAEncryption", "value": "…" }]
    },
    {
      "signedBy": "bob#…",
      "signatures": [{ "algorithm": "SHA256WithRSAEncryption", "value": "…" }]
    }
  ]
}
```

Payload embeds are themselves `SignedMessage` / `MultiSignedMessage` values as nested JSON objects; they are recursively rehydrated and verified.

---

## 8. Invariants and Rules

1. Every `SignableMessage` MUST declare a canonical `signer()` and pass `isValid()` before signing.
2. `SignedMessage.signedBy` MUST satisfy `payload.cabBeSignedBy(signedBy)` (rename to `canBeSignedBy` on migration).
3. `SignedMessage.signatures` MUST correspond exactly to one of the algorithm sets declared by `PersonaVersion.getValidSignatureAlgorithmSets()`.
4. `SignableRequestMessage.requestId()` MUST be unique per Persona (server enforces via `RequestService.assertNewRequest`).
5. `SignableAlertMessage.receivers()` MUST be non‑empty; alert `toFcmMap()` MUST include `realm`, `alertType`, `alertId`.
6. `PersonaId` equality is structural: same `id` AND same `sha256Thumbprint` (or both wildcards). Cross‑instance signing is governed by `canSignOnBehalfOf`.
7. `PersonaVersion` is authoritative for allowed algorithms — hard‑coded strings elsewhere should be replaced by lookups against `PersonaVersion`.
8. `Amount` arithmetic is currency‑guarded and throws on mismatched currencies.
9. Payments are non‑forgeable at the payer boundary: `PaymentEncashment.isValid()` requires either the payee proxy match or a valid `secret` for hash‑based `Payee`s.
10. `OfflinePayment.isValid()` requires the summed `maxAmount` of its `OfflineMoney` tokens to cover the requested `Amount`.

---

## 9. Migration Notes (Proxy → Persona)

When performing the rename:

- Java packages: `in.yagnyam.proxy` → `in.yagnyam.persona`. Sub‑packages preserved (`.services`, `.bootstrap`, `.authentication`, `.config`, `.utils`, `.messages.*`, `.server.*`).
- Public constants: `PROXY_CENTRAL` → `PERSONA_REGISTRAR`; `ProxyUniverse.PRODUCTION` → `Realm.PRODUCTION`.
- Alert FCM keys: `proxyUniverse` → `realm`, `receiverProxyId` → `receiverPersonaId`.
- Class typos to correct in the same pass:
  - `cabBeSignedBy` → `canBeSignedBy` (`SignableMessage`, `SignedMessage`).
  - `checkerframework` reference stays; verify Guava/BouncyCastle/jose4j versions during the rename.
- HTTP endpoints (default URLs):
  - `POST /proxy-id` → `POST /persona-id`.
  - `POST /proxy` → `POST /persona`.
  - `GET /certificate/{id}` remains (`CertificateService`), but the root path variable `caCertificateRoot` should be renamed to `caRoot` and documented.
- Wire compatibility: since `type` in `SignedMessage` is the fully qualified class name, renaming Java packages breaks the wire. Introduce a compatibility layer:
  - Server side: accept both `in.yagnyam.proxy.*` and `in.yagnyam.persona.*` types by aliasing during class resolution in `MessageBuilder`.
  - Emit new envelopes with `in.yagnyam.persona.*` once a majority of peers have been upgraded.
  - Consider decoupling `type` from Java class names by introducing a stable `messageType` string registry (recommended long‑term).

---

## 10. Suggested Improvements Discovered During Analysis

1. Convert `Services`, `Currency`, and status enums (`WithdrawalStatusEnum`, `DepositStatusEnum`, `PaymentEncashmentStatusEnum`, `PaymentAuthorizationStatusEnum`, `EscrowAccountStatusEnum`) into `enum` types (some already are — align the rest).
2. Include a Signature Timestamp and a stable message Id inside the payload (see TODOs in `SignedMessage`). This enables:
   - Signature caching by `(messageId, signatureHash)`.
   - Better audit trails.
3. Move `type` off Java class names into a versioned string registry (`banking.PersonaAccount@v1`, …). This decouples wire format from Java refactors and enables cross‑language clients.
4. Replace ad‑hoc URL construction in `RemoteCertificateService` / `RemotePersonaResolver` with a builder that validates thumbprint format.
5. Cache `CertificateChain` in `CacheBackedCertificateService` (currently marked TODO).
6. Introduce a canonical JSON writer so signatures are stable across Jackson versions (guarantee UTF‑8 + no‑whitespace + key order).
7. Push key algorithm negotiation out of `PersonaVersionV0` into a per‑Persona capability descriptor to support ECC / post‑quantum migration.
8. Replace `PROXY_CENTRAL` hard‑code in `PersonaCreationResponse.signer()` with a configurable registrar Persona.

---

## 11. Reference Class Index

Full inventory used to derive this specification.

**`proxy-core-lib` (core framework, ~50 types):**
`Persona`, `PersonaId`, `PersonaKey`, `PersonaRequest`, `Certificate`, `CertificateChain`, `Certificates`, `CipherText`, `Hash`, `Hmac`, `Constants`, `Services`, `Realm`, `PersonaBaseObject`, `SignableMessage`, `SignableRequestMessage`, `SignableAlertMessage`, `MultiSignableMessage`, `AddressableMessage`, `RequestMessage`, `SignedMessage`, `SignedMessageSignature`, `MultiSignedMessage`, `MultiSignedMessageSignature`. Config: `PersonaVersion`, `PersonaVersionV0`. Bootstrap: `PersonaIdFactory`, `PersonaIdRequest`, `PersonaIdResponse`, `PersonaFactory`, `PersonaCreationRequest`, `PersonaCreationResponse`, `PersonaKeyFactory`, `BcPersonaKeyFactory`, `PersonaRequestFactory`, `BcPersonaRequestFactory`. Services: `MessageSerializerService`, `MessageBuilder`, `MessageSigningService`, `MessageVerificationService`, `DummyMessageVerificationService`, `MessageFactory`, `CryptographyService`, `BcCryptographyService`, `PemService`, `NetworkService`, `CertificateService`, `CertificateRequestService`, `BcCertificateRequestService`, `RemoteCertificateService`, `CacheBackedCertificateService`, `PersonaResolver`, `SimplePersonaResolver`, `RemotePersonaResolver`, `CacheBackedPersonaResolver`. Authentication: `AuthenticationToken`, `AuthenticationTokenService`, `CertificateResolver`, `RemoteCertificateResolver`, `CertificateVerificationKeyResolver`. Utils: `PersonaUtils`, `CertificateUtils`, `StringUtils`, `DateUtils`, `CollectionUtils`, `RandomUtils`, `EncodingUtils`, `JsonUtils`, `Throwing`, `Errors`.

**`proxy-messages` (domain, grouped):**
- banking: `Currency`, `Amount`, `PersonaAccount`, `PersonaAccountId`, `AccountPermission`, `NonProxyAccount`, `BankingServiceProvider`, `SignedProxyAccounts`, `AccountBalanceRequest/Response`, `AccountDetailsRequest/Response`, `CreateProxyAccountRequest`, `DepositRequest`, `DepositRequestCreationRequest/Response`, `DepositRequestStatusRequest/Response`, `DepositRequestCancelRequest/Response`, `DepositStatusEnum`, `Withdrawal`, `WithdrawalResponse`, `WithdrawalStatusRequest/Response`, `WithdrawalStatusEnum`.
- banking.wallet: `ProxyWalletCreationRequest/Response`.
- banking.bunq: `AuthorizeBunqAccountRequest/Response`.
- banking.alerts: `AccountUpdatedAlert`, `DepositUpdatedAlert`, `WithdrawalUpdatedAlert`.
- payments: `PaymentRequest`, `PaymentForPaymentRequest`, `PaymentAuthorization`, `PaymentAuthorizationRegistered`, `PaymentAuthorizationStatusRequest/Response`, `PaymentAuthorizationStatusEnum`, `Payee`, `PayeeTypeEnum`, `PaymentEncashment`, `PaymentEncashmentRegistered`, `PaymentEncashmentStatusRequest/Response`, `PaymentEncashmentStatusEnum`, `PaymentConfirmation`.
- payments.alerts: `PaymentAuthorizationUpdatedAlert`, `PaymentEncashmentUpdatedAlert`.
- payments.offline: `OfflineMoney`, `OfflinePayment`, `BuyOfflineMoneyRequest/Response`.
- escrow: `EscrowAccount`, `EscrowAccountId`, `EscrowAccountStatusEnum`, `EscrowAccountCreationRequest/Response`, `EscrowAccountDetailsRequest/Response`.
- escrow.negotiation: `EscrowAccountProposal`, `EscrowAccountProposalPayeeAck`, `EscrowAccountProposalEscrowAck`, `EscrowAccountProposalStatus`.
- escrow.alerts: `EscrowAccountUpdatedAlert`.
- identity: `PersonaSubject`, `PersonaSubjectId`, `CreateProxySubjectRequest`, `PersonaSubjectDetailsRequest/Response`, `SubjectIdentity`, `SubjectDetails`, `SubjectFieldEnum`, `SubjectFieldTypeEnum`, `SubjectIdTypeEnum`, `IdentityServiceProvider`.
- identity.aadhaar: `AadhaarVerificationRequest`, `AadhaarVerificationChallenge`, `AadhaarVerificationChallengeResponse`.
- identity.self: `IdentityVerificationRequest`.
- authorization: `EmailAuthorizationRequest`, `EmailAuthorizationChallenge`, `EmailAuthorizationChallengeResponse`, `EmailAuthorization`, `PhoneNumberAuthorizationRequest`, `PhoneNumberAuthorizationChallenge`, `PhoneNumberAuthorizationChallengeResponse`, `PhoneNumberAuthorization`, `SendSecretsRequest`, `SendSecretsResponse`, `FetchSecretForEmailRequest/Response`, `FetchSecretForPhoneNumberRequest/Response`.
- alerts: `SubscribeForAlertsRequest/Response`, `PendingAlertsRequest/Response`, `DeleteAlertsRequest/Response`.
- geo: `GeoPoint`, `Circle`, `FindCircleRequest/Response`, `FindCircleSession`, `FindCircleSessionId`.
- geo.alerts: `CircleFoundAlert`.
- sack: `MoneySack`, `MoneySackId`, `CreateMoneySackRequest`.
- mobile: `IdentityRequest`, `PaymentRequest`.
- bunq: `AccountAuthorizationRequest`.

**`proxy-server-lib`:**
`ServiceException`, `BadRequestException`, `NotFoundException`, `UnauthorizedException`, `ForbiddenException`, `InternalServerErrorException`, `DataValidation`, `IdGenerator`, `ServletUtils`. Services: `DbBackedCertificateService`, `PrivateKeyService`, `RequestService`, `VerificationKeyResolverAdapter`. DB: `CertificateRepository`, `DataStoreCertificateRepository`, `PrivateKeyRepository`, `RequestRepository`. Model: `CertificateEntity`, `PrivateKeyEntity`, `RequestEntity`.
