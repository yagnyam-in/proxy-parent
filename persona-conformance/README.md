# Persona Conformance Suite

Language-agnostic test vectors for the Persona Protocol, plus the Java
reference runner.

## Layout

```
vectors/               Portable test vectors (JSON). THE cross-language artifact.
  schema/              Schema digest + binary codec vectors
  crypto/              Signature / hash vectors (deterministic algorithms only)
  envelope/            Full signed-envelope golden vectors
  negative/            Tampering / policy-violation cases (expected rejections)
src/test/java/         Java reference runner (JUnit 5)
README.md              This file
```

## Consuming the vectors from another language

Each vector file is self-contained JSON with an {@code id}, a {@code kind},
the inputs, and the expected outputs. A conforming implementation in any
language (C++, Rust, Go, ...) must:

1. Read every {@code *.json} file under {@code vectors/} (recursively).
2. Dispatch on {@code kind}.
3. Assert the expected outcomes exactly as the Java runner does
   (see {@code in.yagnyam.persona.conformance}).

Golden outputs (binary hex, digests, signatures) are deterministic — the suite
only uses deterministic algorithms (e.g. {@code SHA256withRSA} for signatures,
{@code SHA-1} for schema digests), so byte-identical reproduction is required.

## Running

```bash
./scripts/test.sh        # from the repository root
# or, directly:
./mvnw -pl persona-conformance verify
```
