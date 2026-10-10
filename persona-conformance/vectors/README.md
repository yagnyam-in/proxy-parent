# Persona Conformance Vectors v1

Portable, language-agnostic test vectors for the Persona Protocol. These files
are the source of truth for cross-language behavior: any implementation
(C++, Rust, Go, ...) proves conformance by executing every vector in this
directory, exactly as the Java runner in `../src/test/java/` does.

Normative references:

* `docs/SCHEMA.md` — the schema model (§1), canonical form + digest (§2),
  binary encoding (§3), canonical JSON (§4), built-in schemas (§5).
* `PERSONA-PROTOCOL.md` — the wire/behavior protocol
  (§3 identity, §7 registration, §8 crypto policy, §9 encoding, §11 verification).
  Paths cited from vectors as `PERSONA-PROTOCOL.md §x` refer to the repository
  root copy; vectors also cite `docs/SCHEMA.md §x`.

## Layout

```
vectors/
  manifest.json               Index of every file (regenerate after changes)
  schema-digest/              §2 canonical form + SHA-1 schema digest, and rejects
  binary/                     §3 binary payload encode/decode + rejects
  json/                       §4 canonical JSON payload encode/decode + rejects
  envelope/                   §5 built-in envelope schemas over both codecs
  identity/                   §3 persona-id / delegation / realm / PEM
  crypto/                     §8 Persona Version v0 policy + §11 algorithm sets
```

## File format

Every `*.json` file under this directory except `manifest.json` is a *vector
file*. A runner discovers files by walking the tree recursively (sorted, so
failures are deterministic) and dispatching on `kind`.

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
| `payload.json.decode` | `json/` | `schema`, `inputJson`, `expectedValue` | Decode §4 canonical JSON; non-canonical-but-parsable documents are accepted only where §4.4 says so. |
| `identity.persona-id` | `identity/` | `input`, `expectedId`, `expectedThumbprint`, `expectedCanonical`, `expectedWildcard` | Parse the `<id>#<thumbprint>` grammar of §3.2. `expectedThumbprint` is `null` when unpinned. |
| `identity.persona-id-delegation` | `identity/` | `signer`, `signedFor`, `expected` | The §3.2 delegation truth table `X.canSignOnBehalfOf(Y)`; `signedFor` may be `null` (never delegable). |
| `identity.realm` | `identity/` | `input`, `expected` | Realm identifier validity (§3.5). See the caveat in that file's `description`. |
| `identity.pem` | `identity/` | `objectType`, `pem`, `expected` | §9.1 PEM decode of `certificate` / `publicKey` / `privateKey` / `csr`. Accept vectors pin the derived facts (owner, serial, validity window, SHA-256 DER thumbprint, re-encoded PEM byte identity); reject vectors pin §9.1 object-type mismatches. The suite ships **no private-key material** (repository security policy): `privateKey` coverage is reject-only, and a foreign implementation pairs it with a throwaway key of its own. |
| `crypto.policy-v0` | `crypto/` | `policy`, `field`, `expected` | The §8 Persona Version v0 policy, field by field. |
| `crypto.signature-algorithm-set` | `crypto/` | `signatureAlgorithms`, `expected` | §11 rule 3 exact-match acceptance of an envelope's signature algorithm set against the version's valid sets. |

Envelope vectors are ordinary payload vectors whose `schemaRef` is one of the
§5 built-ins (`persona.SignedEnvelope@v1`, `persona.MultiSignedEnvelope@v1`,
`persona.Signature@v1`, `persona.MultiSignature@v1`): the envelope is *a record
with a `bytes` payload*, which is the whole point of §5 — a foreign
implementation reuses its own codec.

## Runner contract

1. **Discover** every `*.json` under `vectors/` except `manifest.json`,
   recursively.
2. **Refuse** any file whose `vectorFormat`/`vectorFormatVersion` is unknown,
   and fail the run for an unknown `kind` or an entry whose keys do not belong
   to the file's `kind`. Unknown vector kinds MUST fail the suite — skipping is
   how a port silently diverges.
3. **Execute every vector.** Rejections are checked by asserting that the
   operation fails; the failure message should include the `rejectionRule`
   citation so a foreign implementation can confirm it rejects for the
   mandated reason and not by accident.
4. **Report counts**: files, vectors, and per-kind executed counts. The Java
   runner additionally asserts a minimum per kind, so deleting vectors is loud.
5. **Cross-check `manifest.json`**: the file index must list exactly the
   discovered files with the right `kind` and `vectorCount`.

The `../src/test/java/in/yagnyam/persona/conformance/` runner is the executable
definition of these rules.

## Independent verification

`../tools/reference_vectors.py` is a **second, independent implementation**
(pure Python 3, standard library only, no Java code involved). It implements
the canonical form, the digest, the binary and JSON codecs, the persona-id
grammar, the delegation rule, the realm grammar and the v0 policy from the
specification text, and it *executes every vector in this directory*:

```
python3 tools/reference_vectors.py check      # all 379 vectors
python3 tools/reference_vectors.py selftest   # pins the reference itself
python3 tools/reference_vectors.py manifest    # regenerate manifest.json
```

Every mechanically reproducible golden in this directory was produced by that
reference implementation, and the digest goldens were cross-checked a third
time with `shasum -a 1` and `openssl sha1`; the certificate thumbprint with
`openssl x509 -fingerprint -sha256`. The reference deliberately does **not**
re-implement X.509/PKCS#8 parsing, so it verifies the PEM thumbprint
computation and skips (reports, never hides) the X.509-derived fields and PEM
rejections; those are covered by the Java runner against BouncyCastle, and by
the `openssl` commands recorded in `identity/pem.json`.

## Documented ambiguities and leniencies

These are places where the specification text does not fully pin behavior; the
vectors pin the reference implementation's behavior and mark it in the vector
`description` so the divergence is visible instead of silent:

* **§3.4 rule 1, array vs. map zero-byte asymmetry** — a non-empty array whose
  element record encodes to zero bytes is *accepted* by the binary codec (the
  per-element zero-progress rule only fires when the block count itself is
  zero) but *rejected* by the JSON encoder. See
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
* **§9.1 multi-object PEM** — parsing a PEM *file* means parsing its **first**
  object; chains travel as multi-object PEM files
  (`identity/pem.json` → `certificate-with-a-csr-appended`).
* **Timestamps outside ISO-8601's year range** — the vectors use the
  `@epoch-millis` extension token because JSON cannot carry
  `+000001-01-01T00:00:00Z`. On *decode*, both codecs accept the
  expanded-year ISO-8601 rendering (`-292275055-05-16T16:47:05.000Z`) that
  §4.1 itself names for the timestamp floor, as long as the value stays within
  the inclusive `[-9223372036854775000, 9223372036854775807]` epoch-millis
  range of §3.4 rule 7 (`json/decode.json` → `timestamp-expanded-year-at-floor`,
  and one second below it in `json/decode-reject.json`).

## Adding vectors

1. Add entries to a file of the right `kind` (or a new file with a full
   top-level block; one `kind` per file).
2. Regenerate the index: `python3 tools/reference_vectors.py manifest`.
3. Run `python3 tools/reference_vectors.py check` — it fails on any golden the
   independent reference disagrees with, and on a stale manifest.
4. Run `./scripts/test.sh` from the repository root; the Java runner asserts
   the new counts.
