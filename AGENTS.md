# AGENTS.md — Development Workflow for the Persona Protocol

This repository hosts two generations of the same framework:

| Generation | Modules | Status |
| ---------- | ------- | ------ |
| **Legacy (Proxy)** | `proxy-core-lib`, `proxy-messages`, `proxy-server-lib` | Frozen. Kept for reference. Built only with `-Plegacy` (requires JDK 8; does not build under JDK 21). |
| **Current (Persona)** | `persona-core-lib`, `persona-conformance`, `persona-examples` | Active development. Java 21, built by default. |

Normative documents for the current generation:

- `PERSONA-PROTOCOL.md` — the wire/behavior protocol (RFC-2119 style).
- `SPEC.md` — migration spec mapping Proxy → Persona naming and concepts.
- `persona-conformance/vectors/` — machine-readable test vectors; the source of truth for cross-language behavior.

## Environment

- JDK 21 (Homebrew keg: `/opt/homebrew/opt/openjdk@21`), see `scripts/env.sh`.
- Maven via `./mvnw` (wrapper 3.6.3).
- All shell commands in this file assume the repo root as working directory.

## Build & Test

```bash
source scripts/env.sh          # sets JAVA_HOME for the session
./scripts/build.sh             # clean install of persona-* modules
./scripts/test.sh              # unit tests + conformance vectors
./mvnw -Plegacy verify         # (optional) legacy build on a JDK 8 machine
```

## Multi-Agent Workflow (single developer)

The human is the sole developer; agents do the heavy lifting. **The agent that
writes a change must never be the agent that reviews it.**

1. **Implementation agent** — works on a feature branch (`persona/<topic>` or
   `conformance/<topic>`). Runs `./scripts/build.sh` and `./scripts/test.sh`
   before committing. Commit messages: `feat:`, `fix:`, `test:`, `docs:`,
   `refactor:`, `chore:`.
2. **Local review agent** — a *different* agent instance reviews the diff
   (correctness, protocol conformance, security). Findings are filed and fixed
   by the implementation agent (or a new one) before pushing.
3. **Architecture review** — at each milestone (module boundaries, envelope
   format, schema-digest scheme), a separate agent reviews the design against
   `PERSONA-PROTOCOL.md` and records findings in `docs/` or `GAPS.md`.
4. **GitHub Droid review** — every PR opened against `master` automatically
   receives a deep code review + security review from the Factory Droid GitHub
   App (see `.github/workflows/droid-review.yml`). Push early, push often:
   branches are cheap; PRs can be opened as soon as the build is green.
5. **Merge** — only after local review, architecture review (when applicable),
   and the GitHub Droid review pass.

## Module Map (current generation)

```
persona-core-lib       Identity, crypto, schema registry + binary codec,
                       envelopes, signing/verification, registrar, P2P messenger
persona-conformance    Language-agnostic test vectors (vectors/) + Java runner
persona-examples       Runnable examples (P2P demo)
```

## Conventions

- Java package root: `in.yagnyam.persona`. Never introduce the `proxy`
  vocabulary in new code; legacy terms appear only in `SPEC.md` history notes.
- Wire identifiers (`messageType`, `schemaId`, algorithm names) are strings,
  never Java class names.
- `payload` in an envelope is opaque bytes: sign once, verify the exact bytes,
  never re-serialize between signing and verification.
- Schema IDs are SHA-1 digests (40-char lowercase hex) of the schema's canonical
  form, defined precisely in `persona-core-lib` `in.yagnyam.persona.schema`.
- Crypto policy lives in `PersonaVersion`; nothing else may hard-code algorithm
  names.
- New public types need a unit test; new wire behavior needs a conformance
  vector.
