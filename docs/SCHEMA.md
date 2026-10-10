# SCHEMA.md — Persona Schema Model, Canonical Form, and Binary Encoding

> Normative. This document precisely defines the Persona Protocol's schema
> model, the SHA-1 schema-digest scheme, the Avro-style binary payload
> encoding, and the canonical JSON encoding. Any implementation in any
> language must be able to read this document plus the conformance vectors in
> `persona-conformance/vectors/` and produce byte-identical results.
>
> Normative keywords follow RFC 2119.

## 1. Schema Model

A schema is a JSON document. The root node MUST be a `record`.

```json
{
  "type": "record",
  "name": "Greeting",
  "namespace": "example",
  "messageType": "example.Greeting@v1",
  "fields": [
    {"name": "greetingId", "type": "string"},
    {"name": "from", "type": "string"},
    {"name": "to", "type": "string"},
    {"name": "body", "type": "string"},
    {"name": "issuedAt", "type": "timestamp-millis"},
    {"name": "count", "type": "int"},
    {"name": "score", "type": "double"},
    {"name": "verified", "type": "boolean"},
    {"name": "attachment", "type": "bytes"},
    {"name": "tags", "type": {"type": "array", "items": "string"}},
    {"name": "attributes", "type": {"type": "map", "values": "string"}},
    {"name": "mood", "type": {"type": "enum", "name": "Mood", "symbols": ["HAPPY", "NEUTRAL", "SAD"]}},
    {"name": "inner", "type": {"type": "record", "name": "Inner", "fields": [{"name": "x", "type": "long"}]}},
    {"name": "forwarded", "type": "envelope"}
  ]
}
```

### 1.1 Node kinds

| Node     | JSON shape                                                       | Notes                                              |
| -------- | ---------------------------------------------------------------- | -------------------------------------------------- |
| record   | `{"type": "record", "name", "namespace"?, "messageType"?, "fields": [field...]}` | Root MUST be a record.                          |
| enum     | `{"type": "enum", "name", "namespace"?, "symbols": [string...]}`  | Symbols are ordered; the order is semantic.      |
| array    | `{"type": "array", "items": <node>}`                             |                                                   |
| map      | `{"type": "map", "values": <node>}`                              | String keys.                                      |
| envelope | the JSON string `"envelope"`                                     | A nested self-describing `SignedEnvelope`.       |
| multi-envelope | the JSON string `"multi-envelope"`                          | A nested self-describing `MultiSignedEnvelope`.   |

### 1.2 Primitive types

`string`, `boolean`, `int` (signed 32-bit), `long` (signed 64-bit),
`double` (IEEE 754 binary64), `bytes` (arbitrary byte sequence),
`timestamp-millis` (instant on the UTC timeline, millisecond precision).

### 1.3 Validation rules

1. `name` MUST match `^[A-Za-z_][A-Za-z0-9_]*$`. Field names and enum symbols
   follow the same rule.
2. `namespace`, if present, MUST be dot-separated identifiers matching
   `^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$`.
3. `messageType`, if present, MUST match
   `^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*@[A-Za-z0-9][A-Za-z0-9._-]*$`
   (e.g. `example.Greeting@v1`, `persona.SignedEnvelope@v1`). The
   RECOMMENDED convention is a lowercase dot-separated namespace, an
   UpperCamel type name, and a lowercase version: `payments.PaymentAuthorization@v2`.
4. A record's field names MUST be unique. An enum's symbols MUST be unique and
   non-empty.
5. All fields are required; there are no unions or optional fields in this
   version of the model.
6. Nested records/enums are declared inline. Named type references across the
   document are not supported in this version.
7. There MUST be at most one `messageType` declaration, on the root record
   only.

## 2. Canonical Schema Form and SHA-1 Digest

Schema identity is the **SHA-1 digest of the canonical form** of the parsed
schema, rendered as 40 lowercase hex characters (`schemaId`). Canonicalization
starts from the parsed model, NOT from the raw JSON text, so source
whitespace, key order, and JSON formatting never change the digest.

### 2.1 Canonical JSON writer rules

Used for both schema canonicalization (§2) and payload canonical JSON (§4):

1. Objects serialize as `{` + entries joined by `,` + `}`; entries are
   `"key":value` with NO whitespace around `:`, `,`, `{`, `}`, `[`, `]`.
2. Object keys are sorted by the UTF-8 byte sequence of the key.
3. Arrays serialize in their semantic order.
4. Strings escape exactly: `"` as `\"`, `\` as `\\`, U+0008 as `\b`,
   U+0009 as `\t`, U+000A as `\n`, U+000C as `\f`, U+000D as `\r`, other
   code points below U+0020 as `\u00xx` (two lowercase hex digits).
   All other characters (including non-ASCII) are emitted as raw UTF-8.
5. Integers/longs render as decimal digits. Doubles render per §4.4.

### 2.2 Canonical form of schema nodes

Each node canonicalizes to a JSON value:

- primitive / envelope / multi-envelope: the bare JSON string
  (e.g. `"string"`, `"envelope"`).
- record: `{"fields":[<field>,...],"messageType":"..."?,"name":"...","namespace":"..."?,"type":"record"}`
  where `<field>` = `{"name":"...","type":<node>}` and the `fields` array keeps
  the declared order (field order is semantic). Keys within each object are
  sorted per §2.1 rule 2. `messageType`/`namespace` keys are present only when
  declared.
- enum: `{"name":"...","namespace":"..."?,"symbols":[...],"type":"enum"}`
- array: `{"items":<node>,"type":"array"}`
- map: `{"type":"map","values":<node>}`

`schemaId = lowercase_hex(SHA-1(UTF-8(canonical_form)))`

Example — the schema `{"type":"record","name":"Simple","fields":[{"name":"a","type":"string"}]}`
canonicalizes to:

```
{"fields":[{"name":"a","type":"string"}],"name":"Simple","type":"record"}
```

## 3. Binary Payload Encoding (payloadFormat = `persona-binary`)

Avro-style: schema-driven, no field names, no tags on the wire. A decoder
needs the schema (resolved via the envelope's `schemaId`) to decode.

### 3.1 Primitive encodings

| Type              | Encoding                                                              |
| ----------------- | --------------------------------------------------------------------- |
| boolean           | one byte: `0x01` true, `0x00` false                                    |
| int               | zigzag(n) then unsigned LEB128 varint                                  |
| long              | zigzag(n) then unsigned LEB128 varint                                  |
| double            | 8 bytes, IEEE 754 binary64, **little-endian**                           |
| string            | `long` byte-length L, then L UTF-8 bytes                                |
| bytes             | `long` byte-length L, then L raw bytes                                 |
| timestamp-millis  | `long` of epoch milliseconds                                            |

`zigzag(n) = (n << 1) ^ (n >> 31)` for int (arithmetic shift), i.e. the
standard zigzag; LEB128 is the classic unsigned variable-length encoding,
least-significant group first, 7 bits per byte, high bit = continuation.

### 3.2 Compound encodings

| Node     | Encoding                                                                        |
| -------- | -------------------------------------------------------------------------------- |
| record   | fields concatenated in declared order, each encoded per its type                  |
| enum     | `int` = zero-based index into `symbols`                                           |
| array    | zero or more blocks, then a terminator: each block is a positive `long` count followed by exactly count items; terminator is a `0` `long`. Writers MUST emit a single block for non-empty arrays. Readers MUST loop over blocks. |
| map      | same block structure as array; each item is key (string) then value. Writers MUST emit entries in ascending UTF-8 byte order of their keys. |
| envelope | the nested envelope's fields encoded as a record per the built-in `persona.SignedEnvelope@v1` schema (§5) |
| multi-envelope | same, per `persona.MultiSignedEnvelope@v1`                                   |

### 3.3 Decoder strictness

Decoders MUST reject: trailing bytes after the root record; truncated input;
an enum index outside `symbols`; a negative block count; UTF-8 decode
failures. Errors are hard rejections.

### 3.4 Reference implementation strictness (normative)

The rules in this subsection are normative for the reference implementation
and RECOMMENDED for every implementation of §3 and §4. They exist to keep
decoding safe against hostile input and to keep every decoded value
round-trippable.

1. **Collection cap (writer and reader).** No array or map may hold more than
   `2^20` (1048576) items — and the cap is on the *cumulative* item count
   across all blocks of a single array or map, not per block. Writers reject a
   value above the cap before encoding any byte; readers reject any single
   block count above the cap eagerly, and reject a block that would push the
   running total past the cap. Additionally, a reader rejects any collection
   element that decodes without consuming at least one input byte: a
   non-empty array of zero-field records is the only such shape, and without
   the rule a handful of bytes could otherwise allocate millions of empty
   records.
2. **Nesting depth.** Values nested more than 1000 array/map levels deep are
   rejected on encode (so a cyclic value graph is a hard encoding error, not
   a stack overflow) and on decode.
3. **Varint leniency.** Readers accept non-minimal (padded) LEB128 varints;
   writers always emit the minimal form.
4. **Booleans.** Exactly `0x01` (true) or `0x00` (false); any other byte is
   rejected.
5. **UTF-8.** Decoding is strict: malformed sequences, overlong forms (e.g.
   `0xC0 0x80`) and encoded surrogates are rejected.
6. **Strings.** String values, enum symbols and map keys must be well-formed
   UTF-16 with no unpaired surrogates — on encode and on decode, in both
   codecs.
7. **timestamp-millis range.** Values MUST lie within
   [-9223372036854775000, 9223372036854775807] epoch milliseconds. The bottom
   808 values of the signed 64-bit range cannot round-trip `Instant`
   re-encoding (the `epochSecond × 1000` computation overflows) and are
   rejected on decode in both codecs.
8. **Duplicate keys.** JSON parsing — schema documents and canonical JSON
   payloads — rejects duplicate object keys rather than keeping the last
   spelling.
9. **Non-finite doubles.** `NaN` and ±Infinity are rejected on encode (§4.3)
   and on decode in both codecs: a binary payload carries the raw IEEE 754
   bits, a JSON number beyond the double range overflows to infinity.
10. **Base64 (JSON codec).** `bytes` values decode only from standard base64
    with padding: the length must be a multiple of 4 and the alphabet the
    standard one; unpadded or otherwise lenient input is rejected.

## 4. Canonical JSON Payload Encoding (payloadFormat = `json`)

The payload as a canonical JSON string using the writer rules of §2.1, with
type renderings:

### 4.1 Type renderings

- string → JSON string; bytes → base64 (standard alphabet, with padding) as a JSON string
- boolean → `true`/`false`; int/long → decimal digits
- double → §4.4
- timestamp-millis → ISO-8601 UTC with exactly 3 fractional digits: `2026-10-10T12:00:00.000Z`
- enum → its symbol as a JSON string
- record → JSON object (keys sorted per §2.1)
- array → JSON array; map → JSON object (keys sorted)
- envelope → nested JSON object per §5 with `payload` and signature `value` fields rendered as base64 strings

Conformance vectors for this format SHOULD restrict `timestamp-millis`
values to years 1–9999: years outside that range render as signed expanded
years (e.g. `-292275055-05-16T16:47:05.000Z`), which many ISO-8601 parsers
reject.

### 4.2 Value model

A decoded payload is a map from field name to value. Java reference
representation: `Map<String,Object>` where values are `String`, `Boolean`,
`Integer`, `Long`, `Double`, `byte[]`, `java.time.Instant`,
`List<Object>`, `Map<String,Object>`. Enums decode to their symbol `String`.

### 4.3 Encoder strictness

Encoders MUST reject: missing fields; unknown fields; values not matching the
field type (`int` vs `long` are strict: a JSON-declared `int` field accepts
`Integer`, a `long` accepts `Long` or `Integer` narrowed without loss);
non-finite doubles.

### 4.4 Double rendering

Doubles render with the shortest decimal representation that round-trips to
the same double (Ryu-style, as produced by Java 19+ `Double.toString`). The
conformance vectors only exercise values whose shortest representation is
identical across mainstream implementations (avoid tie-breaking edge cases).

## 5. Built-in Envelope Schemas

These schemas are registered by every `SchemaRegistry` at construction
(`PersonaSchemas` bootstrap). `Signature` and `MultiSignature` are declared
**inline** inside the envelope schemas (named references are not supported,
§1.3 rule 6); the standalone registrations exist only for digest stability
testing.

```json
{"type":"record","name":"Signature","namespace":"persona","messageType":"persona.Signature@v1",
 "fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}

{"type":"record","name":"MultiSignature","namespace":"persona","messageType":"persona.MultiSignature@v1",
 "fields":[{"name":"signedBy","type":"string"},
           {"name":"signatures","type":{"type":"array","items":{"type":"record","name":"Signature","fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}}}]}

{"type":"record","name":"SignedEnvelope","namespace":"persona","messageType":"persona.SignedEnvelope@v1",
 "fields":[{"name":"messageType","type":"string"},
           {"name":"payloadFormat","type":"string"},
           {"name":"schemaId","type":"string"},
           {"name":"payload","type":"bytes"},
           {"name":"signedBy","type":"string"},
           {"name":"signatures","type":{"type":"array","items":{"type":"record","name":"Signature","fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}}}]}

{"type":"record","name":"MultiSignedEnvelope","namespace":"persona","messageType":"persona.MultiSignedEnvelope@v1",
 "fields":[
   {"name":"messageType","type":"string"},
   {"name":"payloadFormat","type":"string"},
   {"name":"schemaId","type":"string"},
   {"name":"payload","type":"bytes"},
   {"name":"signatures","type":{
      "type":"array",
      "items":{
         "type":"record",
         "name":"MultiSignature",
         "fields":[
            {"name":"signedBy","type":"string"},
            {"name":"signatures","type":{
               "type":"array",
               "items":{
                  "type":"record",
                  "name":"Signature",
                  "fields":[
                     {"name":"algorithm","type":"string"},
                     {"name":"value","type":"bytes"}
                  ]
               }
            }}
         ]
      }
   }}
 ]}
```

Wire form of an envelope = canonical JSON (§4) of the envelope record, with
`schemaId` empty string when absent and `payload`/`value` rendered as base64.

## 6. Conformance Rules

1. `schemaId` depends only on the canonical form: two documents that parse to
   the same model MUST produce the same digest, and any semantic difference
   MUST change the digest (best-effort; SHA-1 collisions are out of scope —
   SHA-1 is chosen deliberately per project requirements for digest brevity,
   not for collision resistance against adversarial schema authors; schema
   registration is a trusted operation).
2. `decode(encode(v)) == v` for every valid payload value `v`.
3. Encodings are byte-deterministic: identical schema + identical value
   produce identical bytes on every platform.
4. A payload encoded with schema S MUST NOT decode with schema S' ≠ S except
   by coincidence of isomorphic structure; verifiers MUST resolve the schema
   via `schemaId` (and check it against the `messageType` registry entry when
   both are present).
