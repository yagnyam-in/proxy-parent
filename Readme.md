# Persona Parent

Home of the **Persona Protocol** — pseudonymous cryptographic identities
(`Persona`), signed verifiable messages, and a schema registry with SHA-1
digest IDs. See [`PERSONA-PROTOCOL.md`](PERSONA-PROTOCOL.md) for the protocol
and [`SPEC.md`](SPEC.md) for the Proxy → Persona migration mapping.

## Modules

| Module                 | Contents                                                       |
| ---------------------- | -------------------------------------------------------------- |
| `persona-core-lib`     | Identity, crypto policy, schema registry + binary codec, envelopes, signing/verification, registrar, P2P messenger |
| `persona-conformance`  | Language-agnostic conformance vectors + Java reference runner  |
| `persona-examples`     | Runnable examples (persona-to-persona messaging demo)          |
| `proxy-*` (legacy)     | Frozen pre-rewrite framework, built only with `-Plegacy`       |

## Environment

JDK 21 is required for the `persona-*` modules:

```shell
brew install openjdk@21     # if not already installed
source scripts/env.sh
```

## Build

```shell
./scripts/build.sh          # clean install of persona-* modules
./scripts/test.sh           # unit tests + conformance vectors
./mvnw -Plegacy verify      # legacy modules (requires JDK 8)
```

## Update Maven Wrapper

```shell
./mvnw -N io.takari:maven:wrapper
```

## Dependency Check

```shell
./mvnw org.owasp:dependency-check-maven:check
```

## Development workflow

See [AGENTS.md](AGENTS.md) — implementation and review are performed by
separate agents; every PR gets an automated deep review + security review.
