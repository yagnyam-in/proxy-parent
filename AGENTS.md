# AGENTS.md — Development Workflow for the Persona Protocol

This repository hosts two generations of the same framework:

| Generation | Modules | Status |
| ---------- | ------- | ------ |
| **Legacy (Proxy)** | `proxy-core-lib`, `proxy-messages`, `proxy-server-lib` | Frozen. Kept for reference. Built only with `-Plegacy` (requires JDK 8; does not build under JDK 21). |
| **Current (Persona)** | `persona-core-lib`, `persona-conformance`, `persona-examples` | Active development. Java 21, built by default. |

Current state (October 2026): Persona Version **v1** (Ed25519 signatures,
`X25519/HKDF-SHA256/AES-256-GCM` encryption) is the default crypto policy
(`PersonaVersion.latestVersion()`); v0 (RSA/SHA-256) is retained for backward
compatibility and cross-family negative tests. The conformance suite is fully
merged on `master`; `master` is the only long-lived development branch
(Dependabot's auto-managed `dependabot/*` branches aside).

Normative documents for the current generation:

- `PERSONA-PROTOCOL.md` — the wire/behavior protocol (RFC-2119 style).
- `SPEC.md` — migration spec mapping Proxy → Persona naming and concepts.
- `persona-conformance/vectors/` — machine-readable test vectors; the source of truth for cross-language behavior.
- `docs/SCHEMA.md` — schema-digest and codec rules, referenced normatively by the code.
- `GAPS.md` — verified gaps between protocol and implementation. An entry leaves this file only when its fix is merged and covered by a test or vector; update it when a gap closes.

## Environment

- JDK 21 (Homebrew keg: `/opt/homebrew/opt/openjdk@21`), see `scripts/env.sh`.
- Maven via `./mvnw` (wrapper 3.6.3).
- All shell commands in this file assume the repo root as working directory.

## Build & Test

```bash
source scripts/env.sh          # sets JAVA_HOME for the session
./scripts/build.sh             # clean install of persona-* modules
./scripts/test.sh              # unit tests + conformance vectors (Java)
./mvnw -Plegacy verify         # (optional) legacy build on a JDK 8 machine
python3 persona-conformance/tools/reference_vectors.py check     # independent Python reference: golden values + manifest freshness
python3 persona-conformance/tools/reference_vectors.py manifest  # regenerate vectors/manifest.json after adding/removing/renaming a vector file
```

`manifest.json` is generated, never hand-edited; the Python reference `check`
and the Java runner both fail when it is stale. Run `check` whenever vectors
change.

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

## GitHub Mechanics

- **Droid review triggers.** `.github/workflows/droid-review.yml` fires on
  `opened`, `ready_for_review` and `reopened` only — **not** on pushes
  (`synchronize`). After pushing new commits to an open PR, close and reopen
  it to attach the review to the new head.
- **Dependabot.** Closing a Dependabot PR does not stop it; Dependabot
  recreates the PR on the next scan. Suppress with a
  `@dependabot ignore this dependency` comment. There is no `dependabot.yml`
  on `master`, so ignore rules live in GitHub's internal state. Dependabot
  noise mostly targets the parent pom's legacy pins (e.g. Jackson 2.10.x),
  which feed only the frozen `proxy-*` modules; the `persona-*` modules pin
  their own versions at module level (Jackson 2.22.x, BouncyCastle 1.86+,
  AssertJ 3.27.x).
- **Branch hygiene.** Some PRs were squash-merged (#190, #194), so their
  branches are not ancestors of `master` even though the content landed. Do
  not judge a branch by ancestry alone — verify with `git cherry`, the squash
  diff, or `gh pr view --json mergeCommit,headRefOid` before deleting. Delete
  merged branches after every merge.
- **Worktrees.** The repo runs a single checkout on `master`. When stacking
  work, add a sibling worktree per topic
  (`git worktree add ../proxy-parent-<topic> -b persona/<topic>`) and remove
  it after merge.
- **Commit messages.** Multi-line commit messages go through
  `git commit -F <file>`; heredocs break on shell quoting in this
  environment.

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
  names. In tests, derive expected algorithms and key material from the
  `PersonaVersion` under test (e.g. `registrar.version()`), never hard-code
  them.
- BouncyCastle reports some algorithm names in upper case (`SHA256WITHRSA`);
  assertions compare case-insensitively or with `isIn(...)`.
- New public types need a unit test; new wire behavior needs a conformance
  vector.

## Open decisions (the human's call — agents do not decide these)

- **Jackson's role.** Persona code uses Jackson only as a strict JSON reader
  (`SchemaParser`, `JsonPayloadCodec`); canonical JSON is written by hand.
  Options: keep as-is, slim to `jackson-core` streaming, or hand-roll the
  parser.
- **Legacy security advisories.** GitHub lists advisories against the frozen
  `proxy-*` pins (Jackson 2.10.x etc.). Options: bump the legacy pins anyway
  (Jackson 2.18 still targets Java 8) or drop/archive the legacy modules.
- **Dependabot config.** A `dependabot.yml` with explicit legacy ignore rules
  would make the GitHub-internal ignores durable and self-documenting.
