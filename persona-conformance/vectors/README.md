# Persona Conformance Vectors v1

Portable, language-agnostic test vectors for the Persona Protocol. These files
are the source of truth for cross-language behavior: any implementation
(C++, Rust, Go, ...) proves conformance by executing every vector in this
directory, exactly as the Java runner in `../src/test/java/` does.

Normative references:

* `docs/SCHEMA.md` — the schema model (§1), canonical form + digest (§2),
  binary encoding (§3), canonical JSON (§4), built-in schemas (§5).
* `PERSONA-PROTOCOL.md` — the wire/behavior protocol
  (§3 core concepts: persona-id grammar §3.2, realm §3.5; §7 protocols;
  §8 crypto policy; §10 encoding choices, with PEM transport in §10.1;
  §11 rules and invariants).
  Paths cited from vectors as `PERSONA-PROTOCOL.md §x` refer to the repository
  root copy; vectors also cite `docs/SCHEMA.md §x`.

## Layout

```
vectors/
  manifest.json               Index of every file (regenerate after changes)
  schema-digest/              §2 canonical form + SHA-1 schema digest, and rejects
  binary/                     §3 binary payload encode/decode + rejects
  json/                       §4 canonical JSON payload encode/decode + rejects
  envelope/                   §5 built-in envelope schemas over both codecs;
                              §6.4 signing preimage
  identity/                   §3 persona-id / delegation / realm / PEM
  crypto/                     §8 Persona Version v0 policy + §11 algorithm sets;
                              §6.4/§8 golden signature verification
```

## File format

Every `*.json` file under this directory is a *vector file*, with exactly one
exclusion: the file `manifest.json` at the root of this directory. A file
named `manifest.json` in a *subdirectory* is NOT excluded — it is a vector
file like any other. A runner discovers files by walking the tree recursively
(sorted, so failures are deterministic) and dispatching on `kind`.

Top-level keys (all required):

| Key                   | Meaning |
| --------------------- | ------- |
| `vectorFormat`        | Always the string `persona-conformance-vectors`. A runner MUST refuse a file whose marker differs. |
| `vectorFormatVersion` | Always the integer `1`. A runner MUST refuse an unknown version instead of guessing. |
| `kind`                | The vector kind implemented by every entry in this file. One kind per file: a runner MUST fail hard if an entry's expected keys do not belong to `kind`. |
| `description`         | Human-readable contract of the file. |
| `spec`                | String or array of strings: normative citations. |
| `vectors`             | Array of vector entries. |

Every vector entry has:

| Key            | Meaning |
| -------------- | ------- |
| `id`           | Globally unique, stable, human-readable: `<phase>/<topic>/<case>`. A runner MUST fail on duplicate ids across the suite. |
| `spec`         | The normative citation this vector pins (e.g. `docs/SCHEMA.md §3.1`). |
| `description`  | What the vector proves, including any documented leniency. |
| `expect`       | `reject` for negative vectors. Absent means `accept`. A runner MUST fail on any other value. |
| `rejectionRule`| Required when `expect` is `reject`: the rule (with citation) that mandates the rejection, so a foreign implementation knows *why* and can map it to its own error surface. |
| `rejectionMessageFragment` | Optional, reject vectors only: a substring of the **Java reference implementation's** failure message, pinned only where that message already names the violated rule (e.g. "element decoded without consuming any input byte"). Message text is implementation-specific: a foreign implementation MUST NOT be required to reproduce it. Vectors without this key pin only the failure itself (plus the exception contract where one is declared, see "Runner contract"); vectors with it additionally pin that the reference implementation rejects *for the cited reason*. Currently carried by the §3.4-rule-targeted vectors in `binary/decode-reject.json` (zero-progress, collection-cap, boolean, timestamp-floor, non-finite-double, duplicate-map-key, negative-block-count), by `binary/decode-reject-cumulative-cap.json`, and by the four nesting-depth reject vectors in `binary/nesting-depth{,-encode}.json` and `json/nesting-depth-encode.json`. |

Accepted vectors carry inputs and expected outputs per kind (below). Rejected
vectors carry only the input — never a golden output.

## Value encoding

`value`, `expectedValue` and nested values inside them encode the schema value
model (§1.2) in JSON:

| Schema type | Vector encoding |
| ----------- | --------------- |
| `string`, `enum` | JSON string |
| `boolean` | JSON `true` / `false` |
| `int`, `long` | JSON number (an integer; runners must parse it as an exact integer, not a double) |
| `double` | JSON number, or the tokens below |
| `bytes` | base64 string (standard alphabet, padded) of the exact byte sequence |
| `timestamp-millis` | ISO-8601 instant string (`2026-10-10T12:00:00Z`), or the `@epoch-millis:<int>` token outside the years 1–9999 ISO window (e.g. `@epoch-millis:-62135596800000`), or JSON `null` for the null instant |
| `null` (value model) | JSON `null` |
| `array` | JSON array, elements in order |
| `map` | JSON object (key order in the file is irrelevant; encoding order is pinned by the codecs) |
| `record` | JSON object, one key per declared field, all fields present |

Extension tokens (they keep the files valid JSON while covering values JSON
cannot spell):

* `@epoch-millis:<signed integer>` — a `timestamp-millis` as epoch
  milliseconds. Used when the instant is before year 1 or after year 9999,
  where an ISO-8601 rendering would be non-canonical.
* The strings `"NaN"`, `"Infinity"`, `"-Infinity"` for a `double` field mean
  the IEEE-754 non-finite values (a JSON number can never carry them).
* A `double` written as a JSON number is exact when it carries a decimal
  point or exponent; runners must not re-round it through a float32.

Binary payloads are hex strings (`inputHex`, `expectedHex`), lowercase,
no separators; the decoded byte sequence is UTF-8 where it is text
(§3.1: strings are UTF-8, no byte order mark, no NUL terminator).
`expectedJson` and `inputJson` are *raw document strings* — the exact bytes a
codec must produce or consume — not embedded JSON values.

## Vector kinds

| Kind | Phase | Keys (accept) | Contract |
| ---- | ----- | -------------- | -------- |
| `schema.digest` | `schema-digest/` | `schema` or `schemaRef`, `expectedCanonicalForm`, `expectedSchemaId` | Parse the schema document, canonicalize the *parsed model* (§2.2), render it as compact JSON, SHA-1 the UTF-8 bytes and compare 40 lowercase hex characters. `schemaRef` names a built-in (§5) every implementation must register. |
| `payload.binary.encode` | `binary/` | `schema`/`schemaRef`, `value`, `expectedHex` | Encode the value with the §3 binary codec. |
| `payload.binary.decode` | `binary/` | `schema`/`schemaRef`, `inputHex`, `expectedValue` | Decode the §3 binary codec; trailing bytes and truncation MUST be rejected (`expect: reject` cases with `rejectionRule`). |
| `payload.json.encode` | `json/` | `schema`/`schemaRef`, `value`, `expectedJson` | Encode the value as §4 canonical JSON. |
| `payload.json.decode` | `json/` | `schema`, `inputJson`, `expectedValue` | Decode §4 JSON. The accepted decode vectors pin specific decoder leniencies beyond the canonical form: unsorted record/map keys, timestamps without a fraction and with numeric UTC offsets, and noncanonical base64 trailing bits (each leniency is marked in the vector's description). A conforming decoder MUST accept those inputs; canonical output is required only of the encoder. |
| `identity.persona-id` | `identity/` | `input`, `expectedId`, `expectedThumbprint`, `expectedCanonical`, `expectedWildcard` | Parse the `<id>#<thumbprint>` grammar of §3.2. `expectedThumbprint` is `null` when unpinned. |
| `identity.persona-id-delegation` | `identity/` | `signer`, `signedFor`, `expected` | The §3.2 delegation truth table `X.canSignOnBehalfOf(Y)`; `signedFor` may be `null` (never delegable). |
| `identity.realm` | `identity/` | `input`, `expected` | Realm identifier validity (§3.5). See the caveat in that file's `description`. |
| `identity.pem` | `identity/` | `objectType`, `pem`, `expected` | §10.1 PEM decode of `certificate` / `publicKey` / `privateKey` / `csr`. Accept vectors pin the derived facts (owner, serial, validity window, SHA-256 DER thumbprint, re-encoded PEM byte identity); reject vectors pin §10.1 (malformed input, object-type mismatches). The suite ships **no private-key material** (repository security policy): `privateKey` coverage is reject-only, and a foreign implementation pairs it with a throwaway key of its own. |
| `crypto.policy-v0` | `crypto/` | `policy`, `field`, `expected` | The §8 Persona Version v0 policy, field by field. |
| `crypto.signature-algorithm-set` | `crypto/` | `signatureAlgorithms`, `expected` | §11 rule 3 exact-match acceptance of an envelope's signature algorithm set against the version's valid sets. |
| `crypto.signature-verify` | `crypto/` | `publicKeyPem`, `algorithm`, `preimageHex`, `signatureHex`, `expected` (boolean verdict) | Golden RSASSA-PKCS1-v1_5 signature verification over §6.4 signing preimages (every `preimageHex` is pinned by an `envelope.signing-preimage` vector cited in the description, so payload fields → preimage → signature is provable end-to-end). Accept vectors assert the boolean verdict (`true`/`false`); `expected: false` pins real failure modes without new key material: a valid signature over a different preimage (§11 rule 10 — the bytes verified are not the bytes signed), flipped bytes, truncated/empty/wrong-length signatures, a noncanonical signature representative `s ≥ n` (RFC 8017 §8.2.2: RSAVP1 rejects any representative outside `[0, n-1]` — fed straight to modular exponentiation, `s+n` would reduce back to the golden's representative and wrongly verify; the JCA SunRsaSign and BouncyCastle both return false for it), a signature from a different key pair, and a wrong-digest algorithm. The single `expect: reject` vector pins that an unknown algorithm name MUST error per §8 + §11 rule 3 (§12 downgrade attacks). **Verify-only by policy**: the suite ships **no private-key material** (see `identity.pem`), so the sign side cannot be goldenized portably — the fixture RSA-2048 key pair (§8 key size) was generated once, used to derive the goldens, and destroyed; only the public key (PEM SubjectPublicKeyInfo), the preimages and the golden signature bytes are committed. PKCS#1 v1.5 is deterministic, so the goldens are stable across implementations. |
| `envelope.signing-preimage` | `envelope/` | `domain`, `messageType`, `payloadFormat`, `schemaId`, `payloadHex`, `expectedHex` | The §6.4 signing preimage, byte-exact: `uvarint(len(domain)) \|\| domain \|\| uvarint(len(messageType)) \|\| messageType \|\| uvarint(len(payloadFormat)) \|\| payloadFormat \|\| uvarint(len(schemaId)) \|\| schemaId \|\| uvarint(len(payload)) \|\| payload`, all strings UTF-8, `uvarint` the unsigned LEB128 encoding of the *byte* length. `payloadHex` is the exact payload as lowercase hex (the suite's byte-input convention). `schemaId` is a string or JSON `null`; `null` (an absent schemaId) and `""` denote the same preimage, pinned by the `schema-id-absent-null` / `schema-id-empty-string` pair. The kind is **accept-only**: the preimage formula takes the domain as given, so §6.4 mandates no rejection a vector could pin (a malformed vector is a suite bug, not a protocol rejection). |

A schema-driven vector names its schema in exactly one of three ways: `schema`
(inline schema document text), `schemaRef` (a §5 built-in), or
`generatedSchema` — harness sugar for schemas whose type tree is too deep to
be fed to a JSON parser (the §3.4 rule 2 nesting vectors: a 1000-level type
tree is a JSON document nested thousands of levels deep, deeper than common
parser limits such as Jackson's default `StreamReadConstraints` of 1000). The
only template is
`{"template":"nested-array","elementType":"int","depth":N}`: a one-field
record `Nest` whose field `v` is `N` nested `{"type":"array","items":...}`
levels over `int`, exactly as if spelled out as a `schema` document. A runner
builds it directly in its schema model. Note the depth vectors also nest
*values* ~1000 levels deep inside the vector document, so a runner's JSON
parser for vector files must accept documents that deep (the Java runner
raises its parser's nesting constraint for this reason).

Envelope vectors are ordinary payload vectors whose `schemaRef` is one of the
§5 built-ins (`persona.SignedEnvelope@v1`, `persona.MultiSignedEnvelope@v1`,
`persona.Signature@v1`, `persona.MultiSignature@v1`): the envelope is *a record
with a `bytes` payload*, which is the whole point of §5 — a foreign
implementation reuses its own codec.

## Runner contract

1. **Discover** every `*.json` under `vectors/` recursively, excluding exactly
   one file: `manifest.json` at the root of `vectors/`. Any other `*.json` —
   including a `manifest.json` in a subdirectory — is a vector file.
2. **Refuse** any file whose `vectorFormat`/`vectorFormatVersion` is unknown,
   and fail the run for an unknown `kind` or an entry whose keys do not belong
   to the file's `kind`. Unknown vector kinds MUST fail the suite — skipping is
   how a port silently diverges. A malformed vector file is a bug in the
   suite, never a passing vector: in the Java runner it raises
   `VectorFormatException`, an `AssertionError` (not an `Exception`), so it can
   never satisfy a reject vector's failure assertion, and all vector input
   extraction happens outside the asserted operation.
3. **Execute every vector.** A reject vector asserts that the operation
   *fails* — no more, no less: the suite cannot prove a failure was caused by
   the cited `rejectionRule`. Where the implementation declares an exception
   contract the runner also asserts it (schema parse and both payload codecs
   reject with `IllegalArgumentException`; the persona-id grammar rejects with
   `RuntimeException` — `IllegalArgumentException` for a malformed string,
   `NullPointerException` for a null input, a reference wart; PEM reject
   vectors assert only `Exception` because §10.1 mandates no exception type).
   Vectors carrying `rejectionMessageFragment` additionally pin a substring of
   the reference implementation's failure message — reference behavior, never
   portable; see the key's table entry.
4. **Report counts**: files, vectors, and per-kind executed counts. The Java
   runner additionally asserts a minimum per kind, so deleting vectors is loud.
5. **Cross-check `manifest.json`**: the file index must list exactly the
   discovered files with the right `kind` and `vectorCount`, and its `kinds`
   per-kind counts and grand `vectorCount` must match discovery. The manifest
   is the source of truth for vector counts; this README deliberately does not
   repeat them.

The `../src/test/java/in/yagnyam/persona/conformance/` runner is the executable
definition of these rules.

## Independent verification

`../tools/reference_vectors.py` is a **second, independent implementation**
(pure Python 3, standard library only, no Java code involved). It implements
the canonical form, the digest, the binary and JSON codecs, the §6.4 signing
preimage, the persona-id grammar, the delegation rule, the realm grammar, the
v0 policy and RSASSA-PKCS1-v1_5 signature verification (SPKI DER parsing plus
modular exponentiation and DigestInfo comparison, per RFC 8017 §8.2.2) from
the specification text, and it *executes every vector in this directory*:

```
python3 tools/reference_vectors.py check      # every vector in manifest.json
python3 tools/reference_vectors.py selftest   # pins the reference itself
python3 tools/reference_vectors.py manifest    # regenerate manifest.json
```

Every mechanically reproducible golden in this directory was produced by that
reference implementation, and the digest goldens were cross-checked a third
time with `shasum -a 1` and `openssl sha1`; the certificate thumbprint with
`openssl x509 -fingerprint -sha256`. The signature goldens of
`crypto/signature-verify.json` are the one exception to "produced by the
reference": signing needs the deliberately unshipped private key, so they were
derived once with `openssl dgst -sha256 -sign`, cross-checked byte-identically
against a pure-Python PKCS#1 v1.5 signing derivation and against the Java
`CryptographyService.getSignature` (three independent implementations agreeing
on identical bytes), and are verified at check time by the reference's
RSASSA-PKCS1-v1_5 verifier. The reference deliberately does **not**
re-implement X.509/PKCS#8 parsing; for PEM it verifies the thumbprint
computations it can derive without one — the certificate's SHA-256-over-DER,
the public key's SHA-256 over the SubjectPublicKeyInfo body, and the CSR's
public-key thumbprint via a minimal DER walk that extracts the
SubjectPublicKeyInfo — and it skips (reports, never hides) the remaining
X.509-derived fields, the PEM re-encodings and all PEM rejections; those are
covered by the Java runner against BouncyCastle, and by the `openssl` commands
recorded in `identity/pem.json`.

## Documented ambiguities and leniencies

These are places where the specification text does not fully pin behavior; the
vectors pin the reference implementation's behavior and mark it in the vector
`description` so the divergence is visible instead of silent:

* **§3.4 rule 1, array vs. map zero-byte asymmetry** — a non-empty array whose
  element record encodes to zero bytes is *accepted* by the JSON encoder (the
  JSON encoding is self-describing, so the payload is not an amplification
  vector) but *rejected* by the binary codec, whose reader must reject
  zero-progress elements. See
  `json/compound-encode.json` → `array-of-zero-byte-elements` and the matching
  reject in `binary/encode-reject.json`. The same value is not portable across
  codecs in this version.
* **§3.2 thumbprint rendering** — "base64/hex encoded" is ambiguous; every
  Persona Version v0 hash (SHA-256) pins the 64-character lowercase-hex
  rendering, so uppercase or base64 spellings are rejected
  (`identity/persona-id.json` → `thumbprint-uppercase-hex`,
  `thumbprint-base64`).
* **§3.2 id alphabet** — the id is "any URL-safe string" with no grammar; the
  reference validates only the `#` structure and the thumbprint, so
  `álice` and `a b` parse (`identity/persona-id.json`).
* **§3.5 realm grammar** — §3.5 defines what a Realm is but not the identifier
  grammar; the truth table in `identity/realm.json` is the reference
  implementation's grammar, documented as such in the file description.
* **§10.1 multi-object PEM** — parsing a PEM *file* means parsing its **first**
  object; chains travel as multi-object PEM files
  (`identity/pem.json` → `certificate-with-a-csr-appended`).
* **§3.4 rule 2 on the JSON decode side** — not pinned by this suite. The
  Java reference JSON decoder delegates document parsing to Jackson, whose own
  document-nesting constraint (default 1000) fires at the same magnitude as
  rule 2 with a generic "not valid JSON" error, so neither the accepting nor
  the rejecting boundary vector can attribute the outcome to the rule in the
  reference runner. Rule 2 is pinned for the JSON *writer* and for the binary
  codec in both directions (`json/nesting-depth-encode.json`,
  `binary/nesting-depth{,-encode}.json`). Every implementation MUST still
  accept 1000 and reject 1001 array/map levels on JSON decode per §3.4 rule 2;
  closing the reference gap is tracked as a `persona-core-lib` issue, not a
  vector.
* **§3.4 rule 1 cumulative cap, vector size** — the cap is 2^20 items, so
  isolating the *cumulative* counter needs two individually-valid blocks whose
  sum exceeds 2^20 progress-making elements: `binary/decode-reject-cumulative-cap.json`
  carries ~1 MB of payload (~2.1 MB of hex) on purpose; there is no smaller
  honest shape. The *writer*-side cap has no compact vector at all (an
  over-cap value must list every element in the vector file), so writer-side
  enforcement is covered by the `persona-core-lib` unit tests and noted here
  instead of pinned.
* **Timestamps outside ISO-8601's year range** — the vectors use the
  `@epoch-millis` extension token because JSON cannot carry
  `+000001-01-01T00:00:00Z`. On *decode*, both codecs accept the
  expanded-year ISO-8601 rendering (`-292275055-05-16T16:47:05.000Z`) that
  §4.1 itself names for the timestamp floor, as long as the value stays within
  the inclusive `[-9223372036854775000, 9223372036854775807]` epoch-millis
  range of §3.4 rule 7 (`json/decode.json` → `timestamp-expanded-year-at-floor`,
  and one second below it in `json/decode-reject.json`). On *encode*, the
  signed expanded-year renderings are pinned by
  `json/primitive-encode/timestamp-expanded-year-positive` (`+10000-...`),
  `timestamp-year-zero` (`0000-...`), `timestamp-expanded-year-negative`
  (`-0001-...`) and `timestamp-expanded-year-at-floor`.

## Adding vectors

1. Add entries to a file of the right `kind` (or a new file with a full
   top-level block; one `kind` per file).
2. Regenerate the index: `python3 tools/reference_vectors.py manifest`.
3. Run `python3 tools/reference_vectors.py check` — it fails on any golden the
   independent reference disagrees with, and on a stale manifest.
4. Run `./scripts/test.sh` from the repository root; the Java runner asserts
   the new counts.
