# Persona Conformance Suite

Language-agnostic test vectors for the Persona Protocol, plus the Java
reference runner and an independent Python verifier.

## Layout

```
vectors/               Portable test vectors (JSON). THE cross-language artifact.
  schema-digest/       §2 canonical form + SHA-1 digest, and rejected schema documents
  binary/              §3 binary payload encode/decode, kitchen sink, rejects
  json/                §4 canonical JSON payload encode/decode, rejects
  envelope/            §5 built-in envelope schemas over both codecs, §6.4 signing preimage
  identity/            §3.2 persona-id + delegation, §3.5 realm, §10.1 PEM
  crypto/              §8 Persona Version v0 policy, §11 signature algorithm sets
  manifest.json        Index of every vector file (must stay in sync)
  README.md            The vector file format contract (Persona Conformance Vectors v1)
tools/
  reference_vectors.py Independent Python verifier + golden generator (stdlib only)
src/test/java/         Java reference runner (JUnit 5)
README.md              This file
```

## Consuming the vectors from another language

Each vector file is self-contained JSON with a machine-readable version marker,
exactly one `kind`, and one entry per vector with its inputs, expected outputs,
a normative citation, and — for negative vectors — the rule that mandates the
rejection. A conforming implementation in any language (C++, Rust, Go, ...)
must:

1. Read every `*.json` file under `vectors/` recursively (except exactly
   `vectors/manifest.json`; a `manifest.json` in a subdirectory is a vector
   file), refusing unknown `vectorFormat`/`vectorFormatVersion`.
2. Dispatch on `kind` and fail the suite for an **unknown kind** — never skip.
3. Assert the expected outcomes exactly as the Java runner does, including all
   `expect: reject` vectors. Rejection means the operation fails (plus the
   declared exception contract); `rejectionMessageFragment` pins reference
   message text and is never portable.
4. Cross-check `manifest.json` (file list, per-file `kind` and `vectorCount`,
   per-kind `kinds` counts and the grand total).
5. Report the number of vectors executed, per kind.

`vectors/README.md` is the normative description of the format: the key
inventory, the value encodings (hex for binary, base64 for `bytes`, the
`@epoch-millis` and non-finite-double tokens), the per-kind contract, and the
runner rules.

Every golden value is deterministic (SHA-1 for schema digests, SHA-256 for
certificate thumbprints, no randomized algorithms anywhere), so byte-identical
reproduction is required.

## Independent verification

`tools/reference_vectors.py` is a second implementation of the canonical form,
digest, both codecs, the §6.4 signing preimage and the identity/policy rules,
written from `docs/SCHEMA.md` and `PERSONA-PROTOCOL.md` with no Java involved. It executes
every vector and reports any golden it cannot reproduce:

```bash
python3 tools/reference_vectors.py check      # verify all vectors
python3 tools/reference_vectors.py selftest   # pin the reference itself
python3 tools/reference_vectors.py manifest    # regenerate manifest.json
```

The reference also enforces the entry key contract of `vectors/README.md`
(Runner contract rule 2): a misplaced or unknown per-kind key, or a negative
vector carrying an expected output, fails the run instead of being silently
ignored. `selftest` pins that behavior against synthetic entries.

Digest goldens were additionally cross-checked with `shasum -a 1` and
`openssl sha1`, and the certificate thumbprint with `openssl x509
-fingerprint -sha256`.

## Running

```bash
./scripts/test.sh        # from the repository root
# or, directly:
./mvnw -pl persona-conformance verify
```

The Java runner resolves the vectors directory through the
`conformance.vectors.dir` system property when set, and falls back to
`src/test/resources/vectors` → `vectors` relative to the module, so the suite
runs from an IDE and from a packed JAR alike.
