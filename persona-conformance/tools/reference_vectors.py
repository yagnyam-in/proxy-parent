#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Independent reference implementation of the Persona Schema / payload encodings.

This script is the independent second opinion for the golden values committed in
``persona-conformance/vectors/``.  It is written from the normative documents
only:

  * ``docs/SCHEMA.md``   §1 schema model, §2 canonical form + SHA-1 digest,
                         §3 persona-binary payload encoding, §4 canonical JSON
                         payload encoding, §5 built-in envelope schemas,
                         §6 conformance rules;
  * ``PERSONA-PROTOCOL.md`` §3.2 persona-id grammar, §3.5 realm, §8 Persona
                         Version v0 crypto policy.

It deliberately does **not** import, call, or mirror the Java implementation in
``persona-core-lib``: the LEB128/zigzag/LE-double logic, the canonical JSON
writer and the schema canonicalizer are re-derived here, and every digest is
recomputed with :mod:`hashlib`.  The Java conformance runner executes the same
vectors through ``persona-core-lib``; agreement between the two is the point.

Usage::

    python3 tools/reference_vectors.py check  [--vectors DIR] [--verbose]
    python3 tools/reference_vectors.py hash   SCHEMA_JSON
    python3 tools/reference_vectors.py encode SCHEMA_JSON VALUE_JSON
    python3 tools/reference_vectors.py json   SCHEMA_JSON VALUE_JSON
    python3 tools/reference_vectors.py selftest

``check`` walks a vectors directory, recomputes every golden value and exits
non-zero on the first mismatch (reporting all mismatches it finds).  ``encode``
and ``json`` print the hex / canonical JSON encoding of a value for a schema and
are handy when adding a new vector by hand.  ``hash`` prints the canonical form
and the schema digest.

Pure standard library, Python 3.8+.  No network access, no key generation.
"""

from __future__ import annotations

import base64
import binascii
import hashlib
import json
import os
import re
import struct
import sys
import datetime

# The §3.4 rule 2 nesting-depth vectors nest 1000 collection levels deep,
# which exceeds the default recursion limit of the codecs below.
sys.setrecursionlimit(100000)

VECTOR_FORMAT = "persona-conformance-vectors"
VECTOR_FORMAT_VERSION = 1

# ---------------------------------------------------------------------------
# Errors
# ---------------------------------------------------------------------------


class VectorError(Exception):
    """A malformed vector file or an unsupported/ambiguous vector."""


class Reject(Exception):
    """A schema, payload or identifier that the specification requires to be
    rejected.  ``rule`` carries the normative citation, ``detail`` the reason."""

    def __init__(self, rule, detail):
        super().__init__("%s: %s" % (rule, detail))
        self.rule = rule
        self.detail = detail


# ---------------------------------------------------------------------------
# Strict JSON
# ---------------------------------------------------------------------------


def _reject_duplicate_keys(pairs):
    seen = set()
    for key, _ in pairs:
        if key in seen:
            raise Reject("SCHEMA.md §3.4 rule 8", "duplicate object key %r" % key)
        seen.add(key)
    return dict(pairs)


def _reject_constant(token):
    raise Reject("SCHEMA.md §3.4 rule 9", "non-finite JSON number literal %r" % token)


def json_loads(text, what):
    """Parses JSON rejecting duplicate keys and non-finite literals (§3.4 rules 8, 9)."""
    try:
        return json.loads(
            text,
            object_pairs_hook=_reject_duplicate_keys,
            parse_constant=_reject_constant,
        )
    except Reject:
        raise
    except ValueError as error:
        raise Reject("RFC 8259", "%s is not valid JSON: %s" % (what, error))


# ---------------------------------------------------------------------------
# Canonical JSON writer (docs/SCHEMA.md §2.1)
# ---------------------------------------------------------------------------

_ESCAPES = {
    '"': '\\"',
    "\\": "\\\\",
    "\b": "\\b",
    "\t": "\\t",
    "\n": "\\n",
    "\f": "\\f",
    "\r": "\\r",
}


def is_well_formed_utf16(text):
    """True when ``text`` has no unpaired UTF-16 surrogate (§3.4 rule 6)."""
    index = 0
    length = len(text)
    while index < length:
        code = ord(text[index])
        if 0xD800 <= code <= 0xDBFF:
            if index + 1 >= length:
                return False
            low = ord(text[index + 1])
            if not 0xDC00 <= low <= 0xDFFF:
                return False
            index += 2
        elif 0xDC00 <= code <= 0xDFFF:
            return False
        else:
            index += 1
    return True


def require_well_formed(text, what):
    if not is_well_formed_utf16(text):
        raise Reject(
            "SCHEMA.md §3.4 rule 6",
            "%s contains an unpaired surrogate and cannot be encoded as UTF-8" % what,
        )
    return text


def write_string(text):
    """Canonical JSON string literal (§2.1 rule 4)."""
    require_well_formed(text, "string")
    out = ['"']
    for char in text:
        escape = _ESCAPES.get(char)
        if escape is not None:
            out.append(escape)
        elif ord(char) < 0x20:
            out.append("\\u00%02x" % ord(char))
        else:
            out.append(char)
    out.append('"')
    return "".join(out)


def utf8_key_order(mapping):
    return sorted(mapping.items(), key=lambda item: item[0].encode("utf-8"))


def write_sorted_object(rendered):
    """``{`` + ``"key":value`` joined by ``,`` + ``}`` with UTF-8-sorted keys."""
    parts = []
    for key, value in utf8_key_order(rendered):
        parts.append(write_string(key) + ":" + value)
    return "{" + ",".join(parts) + "}"


# ---------------------------------------------------------------------------
# Schema model (docs/SCHEMA.md §1) and canonical form (§2)
# ---------------------------------------------------------------------------

PRIMITIVES = (
    "string",
    "boolean",
    "int",
    "long",
    "double",
    "bytes",
    "timestamp-millis",
)

NAME_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
NAMESPACE_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$")
MESSAGE_TYPE_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*@[A-Za-z0-9][A-Za-z0-9._-]*$")

RECORD_KEYS = {"type", "name", "namespace", "messageType", "fields"}
ENUM_KEYS = {"type", "name", "namespace", "symbols"}
ARRAY_KEYS = {"type", "items"}
MAP_KEYS = {"type", "values"}
FIELD_KEYS = {"name", "type"}


def _node(kind, **kwargs):
    node = {"kind": kind}
    node.update(kwargs)
    return node


def parse_schema(schema_text):
    """Parses a schema document (§1) and validates it (§1.3)."""
    if not isinstance(schema_text, str):
        raise VectorError("a schema must be a JSON string, found %r" % type(schema_text))
    document = json_loads(schema_text, "schema document")
    if not isinstance(document, dict):
        raise Reject("SCHEMA.md §1.1", "schema root must be a JSON object")
    root = _parse_node(document, True, "root")
    if root["kind"] != "record":
        raise Reject("SCHEMA.md §1.1", "schema root must be a record, found %s" % root["kind"])
    return root


def _require_text(node, key, where):
    if key not in node:
        raise Reject("SCHEMA.md §1.1", "%s is missing required key %r" % (where, key))
    value = node[key]
    if not isinstance(value, str):
        raise Reject("SCHEMA.md §1.1", "%s key %r must be a JSON string" % (where, key))
    return value


def _reject_unknown_keys(node, allowed, where):
    unknown = [key for key in node if key not in allowed]
    if unknown:
        raise Reject(
            "SCHEMA.md §1.1",
            "%s has unknown key(s) %s; allowed keys are %s"
            % (where, sorted(unknown), sorted(allowed)),
        )


def _parse_node(node, root, where):
    if isinstance(node, str):
        if node in PRIMITIVES:
            return _node("primitive", name=node)
        if node == "envelope":
            return _node("envelope", multi=False)
        if node == "multi-envelope":
            return _node("envelope", multi=True)
        raise Reject(
            "SCHEMA.md §1.3 rule 6",
            "%s: unknown type name %r; nested records and enums must be declared inline" % (where, node),
        )
    if not isinstance(node, dict):
        raise Reject("SCHEMA.md §1.1", "%s: a type node must be a JSON string or object" % where)
    kind = _require_text(node, "type", where)
    if kind == "record":
        return _parse_record(node, root, where)
    if kind == "enum":
        return _parse_enum(node, where)
    if kind == "array":
        _reject_unknown_keys(node, ARRAY_KEYS, where)
        if "items" not in node:
            raise Reject("SCHEMA.md §1.1", "%s: array node is missing 'items'" % where)
        return _node("array", items=_parse_node(node["items"], False, where + " items"))
    if kind == "map":
        _reject_unknown_keys(node, MAP_KEYS, where)
        if "values" not in node:
            raise Reject("SCHEMA.md §1.1", "%s: map node is missing 'values'" % where)
        return _node("map", values=_parse_node(node["values"], False, where + " values"))
    if kind in PRIMITIVES or kind in ("envelope", "multi-envelope"):
        raise Reject(
            "SCHEMA.md §1.1",
            "%s: %r is a bare-string node kind and must be written as a JSON string" % (where, kind),
        )
    raise Reject("SCHEMA.md §1.1", "%s: unknown node type %r" % (where, kind))


def _parse_record(node, root, where):
    _reject_unknown_keys(node, RECORD_KEYS, where)
    name = _require_text(node, "name", where)
    if not NAME_RE.match(name):
        raise Reject("SCHEMA.md §1.3 rule 1", "%s: invalid record name %r" % (where, name))
    namespace = node.get("namespace")
    if namespace is not None:
        if not isinstance(namespace, str) or not NAMESPACE_RE.match(namespace):
            raise Reject("SCHEMA.md §1.3 rule 2", "%s: invalid namespace %r" % (where, namespace))
    message_type = node.get("messageType")
    if message_type is not None:
        if not isinstance(message_type, str) or not MESSAGE_TYPE_RE.match(message_type):
            raise Reject(
                "SCHEMA.md §1.3 rule 3", "%s: invalid messageType %r" % (where, message_type)
            )
        if not root:
            raise Reject(
                "SCHEMA.md §1.3 rule 7",
                "%s: record %r declares a messageType but only the root record may" % (where, name),
            )
    if "fields" not in node:
        raise Reject("SCHEMA.md §1.1", "%s: record %r is missing 'fields'" % (where, name))
    fields_json = node["fields"]
    if not isinstance(fields_json, list):
        raise Reject("SCHEMA.md §1.1", "%s: 'fields' must be a JSON array" % where)
    fields = []
    names = set()
    for field_json in fields_json:
        if not isinstance(field_json, dict):
            raise Reject("SCHEMA.md §1.1", "%s: every field must be a JSON object" % where)
        _reject_unknown_keys(field_json, FIELD_KEYS, where + " field")
        field_name = _require_text(field_json, "name", where + " field")
        if not NAME_RE.match(field_name):
            raise Reject("SCHEMA.md §1.3 rule 1", "%s: invalid field name %r" % (where, field_name))
        if field_name in names:
            raise Reject(
                "SCHEMA.md §1.3 rule 4",
                "%s: record %r declares field %r more than once" % (where, name, field_name),
            )
        names.add(field_name)
        if "type" not in field_json:
            raise Reject("SCHEMA.md §1.1", "%s: field %r is missing 'type'" % (where, field_name))
        fields.append(
            {"name": field_name, "type": _parse_node(field_json["type"], False, where + " field " + field_name)}
        )
    return _node("record", name=name, namespace=namespace, messageType=message_type, fields=fields)


def _parse_enum(node, where):
    _reject_unknown_keys(node, ENUM_KEYS, where)
    name = _require_text(node, "name", where)
    if not NAME_RE.match(name):
        raise Reject("SCHEMA.md §1.3 rule 1", "%s: invalid enum name %r" % (where, name))
    namespace = node.get("namespace")
    if namespace is not None:
        if not isinstance(namespace, str) or not NAMESPACE_RE.match(namespace):
            raise Reject("SCHEMA.md §1.3 rule 2", "%s: invalid namespace %r" % (where, namespace))
    if "symbols" not in node:
        raise Reject("SCHEMA.md §1.1", "%s: enum %r is missing 'symbols'" % (where, name))
    symbols_json = node["symbols"]
    if not isinstance(symbols_json, list):
        raise Reject("SCHEMA.md §1.1", "%s: 'symbols' must be a JSON array" % where)
    symbols = []
    seen = set()
    for symbol in symbols_json:
        if not isinstance(symbol, str):
            raise Reject("SCHEMA.md §1.1", "%s: every enum symbol must be a JSON string" % where)
        if not NAME_RE.match(symbol):
            raise Reject("SCHEMA.md §1.3 rule 1", "%s: invalid enum symbol %r" % (where, symbol))
        if symbol in seen:
            raise Reject(
                "SCHEMA.md §1.3 rule 4", "%s: enum %r declares symbol %r twice" % (where, name, symbol)
            )
        seen.add(symbol)
        symbols.append(symbol)
    if not symbols:
        raise Reject("SCHEMA.md §1.3 rule 4", "%s: enum %r must declare at least one symbol" % (where, name))
    return _node("enum", name=name, namespace=namespace, symbols=symbols)


def canonical_form(node):
    """Canonical form of a schema node (docs/SCHEMA.md §2.2)."""
    kind = node["kind"]
    if kind == "primitive":
        return write_string(node["name"])
    if kind == "envelope":
        return write_string("multi-envelope" if node["multi"] else "envelope")
    if kind == "enum":
        members = {"name": write_string(node["name"])}
        if node["namespace"] is not None:
            members["namespace"] = write_string(node["namespace"])
        members["symbols"] = "[" + ",".join(write_string(s) for s in node["symbols"]) + "]"
        members["type"] = write_string("enum")
        return write_sorted_object(members)
    if kind == "array":
        return write_sorted_object({"items": canonical_form(node["items"]), "type": write_string("array")})
    if kind == "map":
        return write_sorted_object({"type": write_string("map"), "values": canonical_form(node["values"])})
    if kind == "record":
        fields = "[" + ",".join(
            write_sorted_object({"name": write_string(f["name"]), "type": canonical_form(f["type"])})
            for f in node["fields"]
        ) + "]"
        members = {"fields": fields}
        if node["messageType"] is not None:
            members["messageType"] = write_string(node["messageType"])
        members["name"] = write_string(node["name"])
        if node["namespace"] is not None:
            members["namespace"] = write_string(node["namespace"])
        members["type"] = write_string("record")
        return write_sorted_object(members)
    raise VectorError("unknown node kind %r" % kind)


def schema_digest(node):
    """``schemaId`` = lowercase hex SHA-1 of the canonical form (§2)."""
    return hashlib.sha1(canonical_form(node).encode("utf-8")).hexdigest()


# ---------------------------------------------------------------------------
# Built-in envelope schemas (docs/SCHEMA.md §5)
# ---------------------------------------------------------------------------

BUILTIN_SCHEMA_JSON = {
    "persona.Signature@v1": (
        '{"type":"record","name":"Signature","namespace":"persona",'
        '"messageType":"persona.Signature@v1",'
        '"fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}'
    ),
    "persona.MultiSignature@v1": (
        '{"type":"record","name":"MultiSignature","namespace":"persona",'
        '"messageType":"persona.MultiSignature@v1",'
        '"fields":[{"name":"signedBy","type":"string"},'
        '{"name":"signatures","type":{"type":"array","items":{"type":"record","name":"Signature",'
        '"fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}}}]}'
    ),
    "persona.SignedEnvelope@v1": (
        '{"type":"record","name":"SignedEnvelope","namespace":"persona",'
        '"messageType":"persona.SignedEnvelope@v1",'
        '"fields":[{"name":"messageType","type":"string"},{"name":"payloadFormat","type":"string"},'
        '{"name":"schemaId","type":"string"},{"name":"payload","type":"bytes"},'
        '{"name":"signedBy","type":"string"},'
        '{"name":"signatures","type":{"type":"array","items":{"type":"record","name":"Signature",'
        '"fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}}}]}'
    ),
    "persona.MultiSignedEnvelope@v1": (
        '{"type":"record","name":"MultiSignedEnvelope","namespace":"persona",'
        '"messageType":"persona.MultiSignedEnvelope@v1",'
        '"fields":[{"name":"messageType","type":"string"},{"name":"payloadFormat","type":"string"},'
        '{"name":"schemaId","type":"string"},{"name":"payload","type":"bytes"},'
        '{"name":"signatures","type":{"type":"array","items":{"type":"record",'
        '"name":"MultiSignature","fields":[{"name":"signedBy","type":"string"},'
        '{"name":"signatures","type":{"type":"array","items":{"type":"record","name":"Signature",'
        '"fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}}}]}}}]}'
    ),
}

BUILTIN_SCHEMAS = {name: parse_schema(text) for name, text in BUILTIN_SCHEMA_JSON.items()}


# ---------------------------------------------------------------------------
# Binary payload encoding (docs/SCHEMA.md §3)
# ---------------------------------------------------------------------------

MAX_COLLECTION_ELEMENTS = 1 << 20
MAX_NESTING_DEPTH = 1000
MIN_EPOCH_MILLIS = -9223372036854775000
MAX_EPOCH_MILLIS = 9223372036854775807

INT_MIN = -(2 ** 31)
INT_MAX = 2 ** 31 - 1
LONG_MIN = -(2 ** 63)
LONG_MAX = 2 ** 63 - 1


def unsigned_varint(value):
    """Classic LEB128: least-significant 7-bit group first, high bit = continuation."""
    if value < 0:
        raise VectorError("unsigned_varint requires a non-negative value")
    out = bytearray()
    while value & ~0x7F:
        out.append((value & 0x7F) | 0x80)
        value >>= 7
    out.append(value)
    return bytes(out)


def zigzag32(value):
    return ((value << 1) ^ (value >> 31)) & 0xFFFFFFFF


def zigzag64(value):
    return ((value << 1) ^ (value >> 63)) & 0xFFFFFFFFFFFFFFFF


def _encodes_to_zero_bytes(node):
    if node["kind"] == "record":
        return all(_encodes_to_zero_bytes(field["type"]) for field in node["fields"])
    return False


def encode_binary(node, value, depth=0):
    """Encodes a value of ``node`` into the persona-binary format (§3.1, §3.2)."""
    kind = node["kind"]
    if kind == "primitive":
        name = node["name"]
        if name == "string":
            data = require_well_formed(_require(value, str, "string"), "string")
            return unsigned_varint(zigzag64(len(data.encode("utf-8")))) + data.encode("utf-8")
        if name == "bytes":
            data = _require(value, bytes, "bytes")
            return unsigned_varint(zigzag64(len(data))) + data
        if name == "boolean":
            if not isinstance(value, bool):
                raise Reject("SCHEMA.md §4.3", "boolean field given %r" % (value,))
            return b"\x01" if value else b"\x00"
        if name == "int":
            if isinstance(value, bool) or not isinstance(value, int) or not INT_MIN <= value <= INT_MAX:
                raise Reject("SCHEMA.md §4.3", "int field given %r" % (value,))
            return unsigned_varint(zigzag32(value))
        if name == "long":
            if isinstance(value, bool) or not isinstance(value, int) or not LONG_MIN <= value <= LONG_MAX:
                raise Reject("SCHEMA.md §4.3", "long field given %r" % (value,))
            return unsigned_varint(zigzag64(value))
        if name == "double":
            if isinstance(value, bool) or not isinstance(value, float):
                raise Reject("SCHEMA.md §4.3", "double field given %r" % (value,))
            if value != value or value in (float("inf"), float("-inf")):
                raise Reject("SCHEMA.md §3.4 rule 9", "non-finite double %r cannot be encoded" % value)
            return struct.pack("<d", value)
        if name == "timestamp-millis":
            if isinstance(value, bool) or not isinstance(value, int):
                raise Reject("SCHEMA.md §4.3", "timestamp-millis given %r" % (value,))
            if not MIN_EPOCH_MILLIS <= value <= MAX_EPOCH_MILLIS:
                raise Reject(
                    "SCHEMA.md §3.4 rule 7", "epoch millis %d is outside the timestamp-millis range" % value
                )
            return unsigned_varint(zigzag64(value))
        raise VectorError("unknown primitive %r" % name)
    if kind == "enum":
        symbol = _require(value, str, "enum")
        if symbol not in node["symbols"]:
            raise Reject(
                "SCHEMA.md §3.2", "%r is not a symbol of enum %r" % (symbol, node["name"])
            )
        return unsigned_varint(zigzag32(node["symbols"].index(symbol)))
    if kind == "array":
        items = _require(value, list, "array")
        if len(items) > MAX_COLLECTION_ELEMENTS:
            raise Reject("SCHEMA.md §3.4 rule 1", "array above the collection cap")
        child = _depth(depth)
        if items and _encodes_to_zero_bytes(node["items"]):
            raise Reject(
                "SCHEMA.md §3.4 rule 1",
                "a non-empty array whose element type encodes to zero bytes cannot round-trip",
            )
        out = bytearray()
        if items:
            out += unsigned_varint(zigzag64(len(items)))
            for item in items:
                out += encode_binary(node["items"], item, child)
        out += b"\x00"
        return bytes(out)
    if kind == "map":
        entries = _require(value, dict, "map")
        if len(entries) > MAX_COLLECTION_ELEMENTS:
            raise Reject("SCHEMA.md §3.4 rule 1", "map above the collection cap")
        for key in entries:
            require_well_formed(key, "map key")
        child = _depth(depth)
        out = bytearray()
        if entries:
            out += unsigned_varint(zigzag64(len(entries)))
            for key, entry in utf8_key_order(entries):
                out += unsigned_varint(zigzag64(len(key.encode("utf-8")))) + key.encode("utf-8")
                out += encode_binary(node["values"], entry, child)
        out += b"\x00"
        return bytes(out)
    if kind == "record":
        return _encode_record(node, _require(value, dict, "record"), depth)
    if kind == "envelope":
        builtin = BUILTIN_SCHEMAS[
            "persona.MultiSignedEnvelope@v1" if node["multi"] else "persona.SignedEnvelope@v1"
        ]
        return _encode_record(builtin, _require(value, dict, "envelope"), depth)
    raise VectorError("unknown node kind %r" % kind)


def _encode_record(record, value, depth):
    unknown = [key for key in value if key not in {f["name"] for f in record["fields"]}]
    if unknown:
        raise Reject(
            "SCHEMA.md §4.3",
            "record %r has unknown field(s) %s" % (record["name"], sorted(unknown)),
        )
    out = bytearray()
    for field in record["fields"]:
        if field["name"] not in value:
            raise Reject(
                "SCHEMA.md §1.3 rule 5",
                "record %r is missing field %r; all fields are required" % (record["name"], field["name"]),
            )
        out += encode_binary(field["type"], value[field["name"]], depth)
    return bytes(out)


def _depth(depth):
    if depth >= MAX_NESTING_DEPTH:
        raise Reject("SCHEMA.md §3.4 rule 2", "value nested more than %d collection levels deep" % MAX_NESTING_DEPTH)
    return depth + 1


def _require(value, expected, what):
    kind = {bytes: "bytes", int: "an integer", list: "an array", dict: "an object"}.get(
        expected, expected.__name__
    )
    ok = isinstance(value, expected)
    if expected is int:
        ok = ok and not isinstance(value, bool)
    if expected is not bytes and expected is not int:
        ok = ok and not isinstance(value, (bytes, bytearray))
    if not ok:
        raise Reject(
            "SCHEMA.md §4.3",
            "%s must be %s, found %r (encoder type strictness)" % (what, kind, value),
        )
    return value


class _Reader(object):
    """Byte reader with the §3.3 and §3.4 strictness rules."""

    def __init__(self, data):
        self.data = data
        self.position = 0

    def remaining(self):
        return len(self.data) - self.position

    def read_byte(self, what):
        if self.position >= len(self.data):
            raise Reject("SCHEMA.md §3.3", "truncated input: expected %s" % what)
        byte = self.data[self.position]
        self.position += 1
        return byte

    def read_unsigned_varint(self, what):
        value = 0
        for shift in range(0, 64, 7):
            group = self.read_byte(what)
            if shift == 63 and (group & 0x7F) > 1:
                raise Reject("SCHEMA.md §3.1", "varint overflow while reading %s" % what)
            value |= (group & 0x7F) << shift
            if not group & 0x80:
                return value
        raise Reject("SCHEMA.md §3.1", "varint longer than 10 bytes while reading %s" % what)

    def read_zigzag_long(self, what):
        raw = self.read_unsigned_varint(what)
        return (raw >> 1) ^ -(raw & 1)

    def read_zigzag_int(self, what):
        value = self.read_zigzag_long(what)
        if not INT_MIN <= value <= INT_MAX:
            raise Reject("SCHEMA.md §3.1", "int value %d out of range while reading %s" % (value, what))
        return value

    def read_length(self, what):
        length = self.read_zigzag_long("length of " + what)
        if length < 0:
            raise Reject("SCHEMA.md §3.1", "negative length %d while reading %s" % (length, what))
        if length > self.remaining():
            raise Reject("SCHEMA.md §3.3", "truncated input: %s needs %d byte(s)" % (what, length))
        return length

    def read_bytes(self, what):
        length = self.read_length(what)
        payload = self.data[self.position : self.position + length]
        self.position += length
        return payload

    def read_string(self, what):
        payload = self.read_bytes(what)
        try:
            return payload.decode("utf-8", "strict")
        except UnicodeDecodeError as error:
            raise Reject("SCHEMA.md §3.4 rule 5", "%s is not valid UTF-8: %s" % (what, error))

    def read_boolean(self, what):
        value = self.read_byte(what)
        if value == 1:
            return True
        if value == 0:
            return False
        raise Reject("SCHEMA.md §3.4 rule 4", "invalid boolean byte 0x%02x" % value)

    def read_double(self, what):
        payload = self.data[self.position : self.position + 8]
        if len(payload) != 8:
            raise Reject("SCHEMA.md §3.3", "truncated input: expected %s" % what)
        self.position += 8
        value = struct.unpack("<d", payload)[0]
        if value != value or value in (float("inf"), float("-inf")):
            raise Reject("SCHEMA.md §3.4 rule 9", "non-finite double cannot be decoded")
        return value

    def read_block_count(self, what):
        count = self.read_zigzag_long("block count of " + what)
        if count < 0:
            raise Reject("SCHEMA.md §3.3", "negative block count %d" % count)
        if count > MAX_COLLECTION_ELEMENTS:
            raise Reject("SCHEMA.md §3.4 rule 1", "block count %d above the collection cap" % count)
        return count

    def read_instant(self, what):
        millis = self.read_zigzag_long(what)
        if millis < MIN_EPOCH_MILLIS:
            raise Reject(
                "SCHEMA.md §3.4 rule 7", "epoch millis %d below the timestamp-millis floor" % millis
            )
        return millis


def decode_binary(node, data, depth=0):
    """Decodes persona-binary bytes into the §4.2 value model."""
    reader = _Reader(data)
    value = _read_value(reader, node, depth)
    if reader.remaining() != 0:
        raise Reject(
            "SCHEMA.md §3.3", "%d trailing byte(s) after the root record" % reader.remaining()
        )
    return value


def _read_value(reader, node, depth):
    kind = node["kind"]
    if kind == "primitive":
        name = node["name"]
        if name == "string":
            return reader.read_string("string")
        if name == "bytes":
            return reader.read_bytes("bytes")
        if name == "boolean":
            return reader.read_boolean("boolean")
        if name == "int":
            return reader.read_zigzag_int("int")
        if name == "long":
            return reader.read_zigzag_long("long")
        if name == "double":
            return reader.read_double("double")
        if name == "timestamp-millis":
            return reader.read_instant("timestamp-millis")
        raise VectorError("unknown primitive %r" % name)
    if kind == "enum":
        index = reader.read_zigzag_int("enum index")
        if index < 0 or index >= len(node["symbols"]):
            raise Reject("SCHEMA.md §3.3", "enum index %d outside the symbols" % index)
        return node["symbols"][index]
    if kind == "array":
        child = _depth(depth)
        items = []
        while True:
            count = reader.read_block_count("array")
            if count == 0:
                return items
            if len(items) + count > MAX_COLLECTION_ELEMENTS:
                raise Reject("SCHEMA.md §3.4 rule 1", "cumulative element count above the cap")
            for _ in range(count):
                before = reader.position
                items.append(_read_value(reader, node["items"], child))
                if reader.position == before:
                    raise Reject(
                        "SCHEMA.md §3.4 rule 1", "collection element decoded without consuming a byte"
                    )
    if kind == "map":
        child = _depth(depth)
        entries = {}
        while True:
            count = reader.read_block_count("map")
            if count == 0:
                return entries
            if len(entries) + count > MAX_COLLECTION_ELEMENTS:
                raise Reject("SCHEMA.md §3.4 rule 1", "cumulative element count above the cap")
            for _ in range(count):
                before = reader.position
                key = reader.read_string("map key")
                if key in entries:
                    raise Reject("SCHEMA.md §3.3", "map key %r appears more than once" % key)
                entries[key] = _read_value(reader, node["values"], child)
                if reader.position == before:
                    raise Reject(
                        "SCHEMA.md §3.4 rule 1", "collection element decoded without consuming a byte"
                    )
    if kind == "record":
        return _read_record(reader, node, depth)
    if kind == "envelope":
        builtin = BUILTIN_SCHEMAS[
            "persona.MultiSignedEnvelope@v1" if node["multi"] else "persona.SignedEnvelope@v1"
        ]
        return _read_record(reader, builtin, depth)
    raise VectorError("unknown node kind %r" % kind)


def _read_record(reader, record, depth):
    value = {}
    for field in record["fields"]:
        value[field["name"]] = _read_value(reader, field["type"], depth)
    return value


# ---------------------------------------------------------------------------
# Canonical JSON payload encoding (docs/SCHEMA.md §4)
# ---------------------------------------------------------------------------

# Section §4.4 restricts vectors to doubles whose shortest round-tripping decimal
# representation is identical across mainstream implementations.  This table is
# the whitelist the reference uses; a vector outside it is a vector bug.  It is a
# list of (little-endian bit pattern,literal) pairs because 0.0 and -0.0 compare
# equal in Python yet have distinct renderings.
SAFE_DOUBLES = [
    (struct.pack("<d", 0.0), "0.0"),
    (struct.pack("<d", -0.0), "-0.0"),
    (struct.pack("<d", 1.0), "1.0"),
    (struct.pack("<d", -1.0), "-1.0"),
    (struct.pack("<d", 1.5), "1.5"),
    (struct.pack("<d", -2.25), "-2.25"),
    (struct.pack("<d", 0.001), "0.001"),
    (struct.pack("<d", 100.0), "100.0"),
    (struct.pack("<d", -0.5), "-0.5"),
    (struct.pack("<d", 3.25), "3.25"),
    (struct.pack("<d", 0.125), "0.125"),
    (struct.pack("<d", 150.0), "150.0"),
]


def double_literal(value):
    if value != value or value in (float("inf"), float("-inf")):
        raise Reject(
            "SCHEMA.md §4.3", "non-finite double %r cannot be encoded" % value
        )
    bits = struct.pack("<d", value)
    for known, literal in SAFE_DOUBLES:
        if bits == known:
            return literal
    raise VectorError(
        "double %r has no implementation-independent shortest representation (SCHEMA.md §4.4)" % value
    )


def _civil_from_days(days):
    """Howard Hinnant's civil_from_days, so expanded years work without datetime.

    Python's ``//`` already floors, so the era uses plain floor division (the
    C formulation shifts by 146096/399 first only because C division truncates
    toward zero)."""
    z = days + 719468
    era = z // 146097
    doe = z - era * 146097
    yoe = (doe - doe // 1460 + doe // 36524 - doe // 146096) // 365
    y = yoe + era * 400
    doy = doe - (365 * yoe + yoe // 4 - yoe // 100)
    mp = (5 * doy + 2) // 153
    d = doy - (153 * mp + 2) // 5 + 1
    m = mp + 3 if mp < 10 else mp - 9
    return (y + (1 if m <= 2 else 0), m, d)


def iso_from_millis(millis):
    """ISO-8601 UTC with exactly three fractional digits (§4.1)."""
    if not MIN_EPOCH_MILLIS <= millis <= MAX_EPOCH_MILLIS:
        raise Reject(
            "SCHEMA.md §3.4 rule 7",
            "epoch millis %d is outside the timestamp-millis range" % millis,
        )
    seconds, milli = divmod(millis, 1000)
    days = seconds // 86400
    rest = seconds % 86400
    year, month, day = _civil_from_days(days)
    hour, rest = divmod(rest, 3600)
    minute, second = divmod(rest, 60)
    if 0 <= year <= 9999:
        return "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ" % (year, month, day, hour, minute, second, milli)
    return "%d-%02d-%02dT%02d:%02d:%02d.%03dZ" % (year, month, day, hour, minute, second, milli)


def encode_json(node, value, depth=0):
    """Canonical JSON payload text (§4)."""
    kind = node["kind"]
    if kind == "primitive":
        name = node["name"]
        if name == "string":
            return write_string(require_well_formed(_require(value, str, "string"), "string"))
        if name == "bytes":
            return write_string(base64.b64encode(_require(value, bytes, "bytes")).decode("ascii"))
        if name == "boolean":
            return "true" if _require(value, bool, "boolean") else "false"
        if name == "int":
            value = _require(value, int, "int")
            if not INT_MIN <= value <= INT_MAX:
                raise Reject("SCHEMA.md §4.3", "int field given %r" % (value,))
            return str(value)
        if name == "long":
            value = _require(value, int, "long")
            if not LONG_MIN <= value <= LONG_MAX:
                raise Reject("SCHEMA.md §4.3", "long field given %r" % (value,))
            return str(value)
        if name == "double":
            return double_literal(_require(value, float, "double"))
        if name == "timestamp-millis":
            return write_string(iso_from_millis(_require(value, int, "timestamp-millis")))
        raise VectorError("unknown primitive %r" % name)
    if kind == "enum":
        symbol = _require(value, str, "enum")
        if symbol not in node["symbols"]:
            raise Reject(
                "SCHEMA.md §4.1", "%r is not a symbol of enum %r" % (symbol, node["name"])
            )
        return write_string(symbol)
    if kind == "array":
        items = _require(value, list, "array")
        if len(items) > MAX_COLLECTION_ELEMENTS:
            raise Reject("SCHEMA.md §3.4 rule 1", "array above the collection cap")
        child = _depth(depth)
        return "[" + ",".join(encode_json(node["items"], item, child) for item in items) + "]"
    if kind == "map":
        entries = _require(value, dict, "map")
        if len(entries) > MAX_COLLECTION_ELEMENTS:
            raise Reject("SCHEMA.md §3.4 rule 1", "map above the collection cap")
        child = _depth(depth)
        rendered = {}
        for key, entry in entries.items():
            require_well_formed(key, "map key")
            rendered[key] = encode_json(node["values"], entry, child)
        return write_sorted_object(rendered)
    if kind == "record":
        return _encode_json_record(node, _require(value, dict, "record"), depth)
    if kind == "envelope":
        builtin = BUILTIN_SCHEMAS[
            "persona.MultiSignedEnvelope@v1" if node["multi"] else "persona.SignedEnvelope@v1"
        ]
        return _encode_json_record(builtin, _require(value, dict, "envelope"), depth)
    raise VectorError("unknown node kind %r" % kind)


def _encode_json_record(record, value, depth):
    unknown = [key for key in value if key not in {f["name"] for f in record["fields"]}]
    if unknown:
        raise Reject(
            "SCHEMA.md §4.3",
            "record %r has unknown field(s) %s" % (record["name"], sorted(unknown)),
        )
    rendered = {}
    for field in record["fields"]:
        if field["name"] not in value:
            raise Reject(
                "SCHEMA.md §1.3 rule 5",
                "record %r is missing field %r; all fields are required" % (record["name"], field["name"]),
            )
        rendered[field["name"]] = encode_json(field["type"], value[field["name"]], depth)
    return write_sorted_object(rendered)


def decode_json(node, text, depth=0):
    """Decodes canonical JSON payload text into the §4.2 value model."""
    if not isinstance(text, str):
        raise VectorError("a payload must be a JSON string")
    try:
        document = json_loads(text, "payload")
    except Reject as reject:
        if reject.rule == "RFC 8259":
            raise Reject("SCHEMA.md §4", "payload is not valid JSON")
        raise
    return _decode_json_value(node, document, depth)


def _decode_json_value(node, document, depth):
    kind = node["kind"]
    if kind == "primitive":
        name = node["name"]
        if name == "string":
            return require_well_formed(_require_json_string(document, "string"), "string")
        if name == "bytes":
            return _decode_base64(_require_json_string(document, "bytes"))
        if name == "boolean":
            if not isinstance(document, bool):
                raise Reject("SCHEMA.md §4.1", "expected a JSON boolean")
            return document
        if name == "int":
            if isinstance(document, bool) or not isinstance(document, int) or not INT_MIN <= document <= INT_MAX:
                raise Reject("SCHEMA.md §4.1", "expected a JSON integer in the signed 32-bit range")
            return document
        if name == "long":
            if isinstance(document, bool) or not isinstance(document, int) or not LONG_MIN <= document <= LONG_MAX:
                raise Reject("SCHEMA.md §4.1", "expected a JSON integer in the signed 64-bit range")
            return document
        if name == "double":
            if isinstance(document, bool) or not isinstance(document, (int, float)):
                raise Reject("SCHEMA.md §4.1", "expected a JSON number")
            value = float(document)
            if value != value or value in (float("inf"), float("-inf")):
                raise Reject("SCHEMA.md §3.4 rule 9", "non-finite double")
            return value
        if name == "timestamp-millis":
            return _parse_instant(_require_json_string(document, "timestamp-millis"))
        raise VectorError("unknown primitive %r" % name)
    if kind == "enum":
        symbol = require_well_formed(_require_json_string(document, "enum"), "enum symbol")
        if symbol not in node["symbols"]:
            raise Reject("SCHEMA.md §4.1", "%r is not a symbol of enum %r" % (symbol, node["name"]))
        return symbol
    if kind == "array":
        if not isinstance(document, list):
            raise Reject("SCHEMA.md §4.1", "expected a JSON array")
        if len(document) > MAX_COLLECTION_ELEMENTS:
            raise Reject("SCHEMA.md §3.4 rule 1", "array above the collection cap")
        child = _depth(depth)
        return [_decode_json_value(node["items"], item, child) for item in document]
    if kind == "map":
        if not isinstance(document, dict):
            raise Reject("SCHEMA.md §4.1", "expected a JSON object")
        if len(document) > MAX_COLLECTION_ELEMENTS:
            raise Reject("SCHEMA.md §3.4 rule 1", "map above the collection cap")
        child = _depth(depth)
        entries = {}
        for key, entry in document.items():
            require_well_formed(key, "map key")
            entries[key] = _decode_json_value(node["values"], entry, child)
        return entries
    if kind == "record":
        return _decode_json_record(node, document, depth)
    if kind == "envelope":
        builtin = BUILTIN_SCHEMAS[
            "persona.MultiSignedEnvelope@v1" if node["multi"] else "persona.SignedEnvelope@v1"
        ]
        return _decode_json_record(builtin, document, depth)
    raise VectorError("unknown node kind %r" % kind)


def _decode_json_record(record, document, depth):
    if not isinstance(document, dict):
        raise Reject("SCHEMA.md §4.1", "expected a JSON object for record %r" % record["name"])
    declared = {f["name"] for f in record["fields"]}
    for key in document:
        if key not in declared:
            raise Reject("SCHEMA.md §4.3", "record %r has unknown field %r" % (record["name"], key))
    value = {}
    for field in record["fields"]:
        if field["name"] not in document:
            raise Reject("SCHEMA.md §1.3 rule 5", "record %r is missing field %r" % (record["name"], field["name"]))
        if document[field["name"]] is None:
            raise Reject("SCHEMA.md §4.3", "value must not be null")
        value[field["name"]] = _decode_json_value(field["type"], document[field["name"]], depth)
    return value


def _require_json_string(document, what):
    if not isinstance(document, str):
        raise Reject("SCHEMA.md §4.1", "expected a JSON string for %s" % what)
    return document


def _decode_base64(text):
    """Standard base64 with padding only (§4.1, §3.4 rule 10)."""
    if len(text) % 4 != 0:
        raise Reject("SCHEMA.md §3.4 rule 10", "%r is not padded standard base64" % text)
    try:
        return base64.b64decode(text, validate=True)
    except (binascii.Error, ValueError):
        raise Reject("SCHEMA.md §3.4 rule 10", "%r is not standard base64" % text)


_INSTANT_RE = re.compile(
    r"^([+-]?[0-9]+)-([0-9]{2})-([0-9]{2})[Tt]([0-9]{2}):([0-9]{2}):([0-9]{2})"
    r"(?:\.([0-9]+))?([Zz]|[+-][0-9]{2}:[0-9]{2})$"
)

_DAYS_IN_MONTH = (31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)


def _days_from_civil(year, month, day):
    """Howard Hinnant's days_from_civil (proleptic Gregorian), the inverse of
    _civil_from_days, so expanded years parse without datetime (§4.1 names
    -292275055-05-16T16:47:05.000Z as a rendering of the timestamp floor).

    Python's ``//`` already floors, so the era uses plain floor division (the C
    formulation shifts by 399 first only because C division truncates)."""
    y = year - (1 if month <= 2 else 0)
    era = y // 400
    yoe = y - era * 400
    doy = (153 * (month + (-3 if month > 2 else 9)) + 2) // 5 + day - 1
    doe = yoe * 365 + yoe // 4 - yoe // 100 + doy
    return era * 146097 + doe - 719468


def _is_leap_year(year):
    return year % 4 == 0 and (year % 100 != 0 or year % 400 == 0)


def _parse_instant(text):
    """Parses an ISO-8601 instant and returns epoch milliseconds (§4.1, §3.4 rule 7)."""
    match = _INSTANT_RE.match(text)
    if not match:
        raise Reject("SCHEMA.md §4.1", "%r is not an ISO-8601 instant" % text)
    year = int(match.group(1))
    month, day, hour, minute, second = (int(match.group(i)) for i in range(2, 7))
    fraction = match.group(7) or ""
    zone = match.group(8)
    if len(fraction) > 9:
        raise Reject("SCHEMA.md §4.1", "%r has sub-nanosecond precision" % text)
    nanos = int((fraction + "000000000")[:9])
    if nanos % 1000000 != 0:
        raise Reject("SCHEMA.md §4.1", "%r is not millisecond precision" % text)
    if not 1 <= month <= 12:
        raise Reject("SCHEMA.md §4.1", "%r is not a valid date" % text)
    max_day = _DAYS_IN_MONTH[month - 1] + (1 if month == 2 and _is_leap_year(year) else 0)
    if not 1 <= day <= max_day:
        raise Reject("SCHEMA.md §4.1", "%r is not a valid date" % text)
    # Time-of-day and offset validation mirrors java.time.Instant.parse
    # (DateTimeFormatter.ISO_INSTANT), which the reference codec uses:
    #   - hour 0..23, minute 0..59, second 0..59;
    #   - T24:00:00 (with an absent or all-zero fraction) is the ISO-8601
    #     end-of-day form and rolls into the next day;
    #   - a leap second (:60) is accepted only at minute 59 and is read as :59,
    #     regardless of whether a leap was actually scheduled on that date;
    #   - numeric offsets follow java.time.ZoneOffset: at most ±18:00.
    if hour > 24:
        raise Reject("SCHEMA.md §4.1", "%r is not a valid time" % text)
    if hour == 24:
        if minute != 0 or second != 0 or int(fraction or "0") != 0:
            raise Reject("SCHEMA.md §4.1", "%r is not a valid time" % text)
    if not 0 <= minute <= 59:
        raise Reject("SCHEMA.md §4.1", "%r is not a valid time" % text)
    if second > 60 or (second == 60 and minute != 59):
        raise Reject("SCHEMA.md §4.1", "%r is not a valid time" % text)
    try:
        if 1 <= year <= 9999:
            days = (datetime.date(year, month, day) - datetime.date(1970, 1, 1)).days
        else:
            # Expanded years (|year| outside 0..9999) need the proleptic calendar.
            days = _days_from_civil(year, month, day)
    except ValueError:
        raise Reject("SCHEMA.md §4.1", "%r is not a valid date" % text)
    if hour == 24:
        days += 1
        hour = 0
    if second == 60:
        second = 59
    millis = ((days * 86400) + hour * 3600 + minute * 60 + second) * 1000 + nanos // 1000000
    if zone in ("Z", "z"):
        offset = 0
    else:
        sign = 1 if zone[0] == "+" else -1
        offset_hour, offset_minute = int(zone[1:3]), int(zone[4:6])
        if offset_minute > 59 or (offset_hour, offset_minute) > (18, 0):
            raise Reject("SCHEMA.md §4.1", "%r has an out-of-range UTC offset" % text)
        offset = sign * (offset_hour * 60 + offset_minute) * 60000
    millis -= offset
    if not MIN_EPOCH_MILLIS <= millis <= MAX_EPOCH_MILLIS:
        raise Reject("SCHEMA.md §3.4 rule 7", "instant outside the timestamp-millis range")
    return millis


# ---------------------------------------------------------------------------
# Vector value model: the §4.1 JSON rendering plus documented extensions
# ---------------------------------------------------------------------------

DOUBLE_TOKENS = {"NaN": "nan", "Infinity": "inf", "-Infinity": "-inf"}


def read_vector_value(node, document, path="v"):
    """Reads a vector ``value``/``expectedValue`` JSON document into the value model.

    Extensions (documented in vectors/README.md): a ``timestamp-millis``
    position may hold ``"@epoch-millis:<int>"``, a ``double`` position may hold
    ``"NaN"``/``"Infinity"``/``"-Infinity"``, and JSON ``null`` denotes the null
    value (which every encoder must reject).
    """
    if document is None:
        return None
    kind = node["kind"]
    if kind == "primitive":
        name = node["name"]
        if name == "string":
            return _require(document, str, path)
        if name == "bytes":
            if not isinstance(document, str):
                raise VectorError("%s must be a base64 string" % path)
            return _decode_base64(document)
        if name == "boolean":
            if not isinstance(document, bool):
                return document
            return document
        if name in ("int", "long"):
            # A wrong-shaped value (string, fraction, boolean) is passed through so
            # that the encoder's own type check produces the rejection.
            if isinstance(document, bool) or not isinstance(document, int):
                return document
            return document
        if name == "double":
            if isinstance(document, str):
                token = DOUBLE_TOKENS.get(document)
                if token is None:
                    return document
                return float(token)
            if isinstance(document, bool) or not isinstance(document, (int, float)):
                return document
            return float(document)
        if name == "timestamp-millis":
            if isinstance(document, str) and document.startswith("@epoch-millis:"):
                return int(document[len("@epoch-millis:") :])
            if isinstance(document, str):
                return _parse_instant(document)
            raise VectorError("%s must be an ISO-8601 string or @epoch-millis" % path)
        raise VectorError("unknown primitive %r" % name)
    if kind == "enum":
        return _require(document, str, path)
    if kind == "array":
        if not isinstance(document, list):
            raise VectorError("%s must be a JSON array" % path)
        return [read_vector_value(node["items"], item, path + "[]") for item in document]
    if kind == "map":
        if not isinstance(document, dict):
            raise VectorError("%s must be a JSON object" % path)
        return dict((key, read_vector_value(node["values"], value, path + "[]")) for key, value in document.items())
    if kind == "record":
        return _read_vector_record(node, document, path)
    if kind == "envelope":
        builtin = BUILTIN_SCHEMAS[
            "persona.MultiSignedEnvelope@v1" if node["multi"] else "persona.SignedEnvelope@v1"
        ]
        return _read_vector_record(builtin, document, path)
    raise VectorError("unknown node kind %r" % kind)


def _read_vector_record(record, document, path):
    """Reads a record value leniently: absent fields stay absent and unknown keys
    are carried through with a generic conversion, so that reject vectors can
    reach the encoder's own missing/unknown-field check instead of failing in the
    vector reader."""
    if not isinstance(document, dict):
        raise VectorError("%s must be a JSON object for record %r" % (path, record["name"]))
    declared = dict((f["name"], f["type"]) for f in record["fields"])
    value = {}
    for key, entry in document.items():
        if key in declared:
            value[key] = read_vector_value(declared[key], entry, path + "." + key)
        else:
            value[key] = _generic_value(entry)
    return value


def _generic_value(document):
    """Converts a value whose schema type is unknown, for unknown-field vectors."""
    if isinstance(document, list):
        return [_generic_value(item) for item in document]
    if isinstance(document, dict):
        return dict((key, _generic_value(item)) for key, item in document.items())
    return document


def render_value(node, value):
    """Renders a value model back to the §4.1 JSON rendering (for comparison)."""
    kind = node["kind"]
    if kind == "primitive":
        name = node["name"]
        if name == "bytes":
            return base64.b64encode(value).decode("ascii")
        if name == "timestamp-millis":
            return iso_from_millis(value)
        return value
    if kind == "array":
        return [render_value(node["items"], item) for item in value]
    if kind == "map":
        return dict((key, render_value(node["values"], item)) for key, item in value.items())
    if kind == "record":
        return dict((f["name"], render_value(f["type"], value[f["name"]])) for f in node["fields"])
    if kind == "envelope":
        builtin = BUILTIN_SCHEMAS[
            "persona.MultiSignedEnvelope@v1" if node["multi"] else "persona.SignedEnvelope@v1"
        ]
        return dict((f["name"], render_value(f["type"], value[f["name"]])) for f in builtin["fields"])
    return value


def deep_equal(left, right):
    if isinstance(left, bool) or isinstance(right, bool):
        return left is right
    if isinstance(left, float) or isinstance(right, float):
        if not (isinstance(left, float) and isinstance(right, float)):
            return False
        return struct.pack("<d", left) == struct.pack("<d", right)
    if isinstance(left, (bytes, bytearray)) or isinstance(right, (bytes, bytearray)):
        return bytes(left) == bytes(right)
    if isinstance(left, dict) and isinstance(right, dict):
        return set(left) == set(right) and all(deep_equal(left[k], right[k]) for k in left)
    if isinstance(left, list) and isinstance(right, list):
        return len(left) == len(right) and all(deep_equal(a, b) for a, b in zip(left, right))
    return left == right


# ---------------------------------------------------------------------------
# Identity layer: persona id (§3.2), realm (§3.5), crypto policy (§8)
# ---------------------------------------------------------------------------

PEM_BEGIN_RE = re.compile(r"^-----BEGIN ([A-Z0-9 ]+)-----$")


def first_pem_body(pem_text):
    """Returns the base64 body of the FIRST PEM object in ``pem_text``.

    PERSONA-PROTOCOL.md §10.1: chains travel as multi-object PEM files, so
    "parse a PEM" must be defined as "parse the first object" (both
    BouncyCastle's PemReader and this reference stop at the first object).
    """
    body_lines = []
    in_object = False
    for line in pem_text.splitlines():
        line = line.strip()
        if not in_object:
            if PEM_BEGIN_RE.match(line):
                in_object = True
            continue
        if line.startswith("-----END"):
            break
        body_lines.append(line)
    if not body_lines:
        return None
    return "".join(body_lines)


PERSONA_ID_RE = re.compile(r"^(?P<id>[^#]+)(?:#(?P<thumbprint>[^#]*))?$")
THUMBPRINT_RE = re.compile(r"^[0-9a-f]{64}$")


def parse_persona_id(text):
    """Parses ``<id>`` / ``<id>#<thumbprint>`` (protocol §3.2; reference grammar)."""
    if text is None:
        raise Reject("PERSONA-PROTOCOL.md §3.2", "null persona id")
    if text.startswith("#"):
        raise Reject("PERSONA-PROTOCOL.md §3.2", "persona id must not start with '#'")
    match = PERSONA_ID_RE.match(text)
    if not match:
        raise Reject("PERSONA-PROTOCOL.md §3.2", "malformed persona id %r" % text)
    identifier = match.group("id")
    thumbprint = match.group("thumbprint")
    if identifier == "":
        raise Reject("PERSONA-PROTOCOL.md §3.2", "empty persona id")
    if thumbprint is None:
        return (identifier, None)
    if thumbprint == "any" and identifier == "any":
        return (identifier, "any")
    if thumbprint == "any":
        raise Reject("PERSONA-PROTOCOL.md §3.2", "the wildcard thumbprint is only valid for id 'any'")
    if not THUMBPRINT_RE.match(thumbprint):
        raise Reject("PERSONA-PROTOCOL.md §3.2", "thumbprint is not 64 lowercase hex characters")
    return (identifier, thumbprint)


def is_wildcard(identifier, thumbprint):
    return identifier == "any" and (thumbprint is None or thumbprint == "any")


def can_sign_on_behalf_of(signer, signed_for):
    """Delegation rule of protocol §3.2."""
    if signed_for is None:
        return False
    if is_wildcard(*signed_for):
        return True
    if signed_for[0] == signer[0] and signed_for[1] is None:
        return True
    return signer == signed_for


REALM_SEGMENT_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")


def is_valid_realm(realm):
    """Realm grammar of the reference implementation (protocol §3.5)."""
    if realm is None or realm == "":
        return False
    return all(REALM_SEGMENT_RE.match(part) for part in realm.split("."))


POLICY_V0 = {
    "version": "v0",
    "validSignatureAlgorithmSets": [["SHA256withRSA"]],
    "preferredSignatureAlgorithmSet": ["SHA256withRSA"],
    "validAsymmetricEncryptionAlgorithms": ["RSA/NONE/OAEPwithSHA-256andMGF1Padding"],
    "preferredAsymmetricEncryptionAlgorithm": "RSA/NONE/OAEPwithSHA-256andMGF1Padding",
    "validSymmetricEncryptionAlgorithms": ["AES/CTR/NoPadding"],
    "preferredSymmetricEncryptionAlgorithm": "AES/CTR/NoPadding",
    "certificateSignatureAlgorithm": "SHA256withRSA",
    "keyGenerationAlgorithm": "RSA",
    "keySize": 2048,
    "preferredHashAlgorithm": "SHA-256",
}


def supports_signature_algorithm_set(algorithms):
    candidate = set(algorithms)
    return any(candidate == set(sanctioned) for sanctioned in POLICY_V0["validSignatureAlgorithmSets"])


# ---------------------------------------------------------------------------
# Vector files
# ---------------------------------------------------------------------------

KNOWN_KINDS = {
    "schema.digest",
    "payload.binary.encode",
    "payload.binary.decode",
    "payload.json.encode",
    "payload.json.decode",
    "identity.persona-id",
    "identity.persona-id-delegation",
    "identity.realm",
    "identity.pem",
    "crypto.policy-v0",
    "crypto.signature-algorithm-set",
}


class Vector(object):
    def __init__(self, path, index, kind, document):
        self.path = path
        self.index = index
        self.kind = kind
        self.document = document
        self.id = document.get("id")

    def __repr__(self):
        return "%s#%s" % (self.path, self.id)


class Report(object):
    def __init__(self, verbose=False):
        self.checked = 0
        self.skipped = 0
        self.kinds = {}
        self.failures = []
        self.skips = []
        self.verbose = verbose

    def check(self, vector, condition, message):
        self.checked += 1
        if not condition:
            self.failures.append("%s (%s): %s" % (vector, vector.kind, message))

    def skip(self, vector, reason):
        self.skipped += 1
        self.skips.append("%s (%s): %s" % (vector, vector.kind, reason))


def load_vector_files(directory):
    """Returns (files, vectors) for every ``*.json`` under ``directory`` except the manifest."""
    files = []
    for root, _dirs, names in sorted(os.walk(directory)):
        for name in sorted(names):
            if not name.endswith(".json"):
                continue
            if root == directory and name == "manifest.json":
                continue
            files.append(os.path.join(root, name))
    vectors = []
    for path in files:
        with open(path, "r", encoding="utf-8") as handle:
            text = handle.read()
        try:
            document = json_loads(text, "vector file")
        except Reject as reject:
            raise VectorError("%s: %s" % (path, reject.detail))
        if document.get("vectorFormat") != VECTOR_FORMAT:
            raise VectorError("%s: missing vectorFormat %r" % (path, VECTOR_FORMAT))
        if document.get("vectorFormatVersion") != VECTOR_FORMAT_VERSION:
            raise VectorError("%s: unsupported vectorFormatVersion" % path)
        kind = document.get("kind")
        if kind not in KNOWN_KINDS:
            raise VectorError("%s: unknown kind %r" % (path, kind))
        for index, entry in enumerate(document.get("vectors", [])):
            entry = dict(entry)
            entry["_file_schemas"] = document.get("schemas", {})
            entry["_file"] = os.path.relpath(path, directory)
            entry["_index"] = index
            vectors.append(Vector(os.path.relpath(path, directory), index, kind, entry))
    return files, vectors


def resolve_schema(vector, document):
    """Resolves a vector's schema: file-local ``schemas`` entry, built-in, inline text,
    or a ``generatedSchema`` template."""
    document = vector.document
    forms = ["schema" in document, "schemaRef" in document, "generatedSchema" in document]
    if sum(forms) != 1:
        raise VectorError(
            "%s: exactly one of 'schema', 'schemaRef' and 'generatedSchema' is required" % vector
        )
    if "schema" in document:
        return parse_schema(document["schema"])
    if "generatedSchema" in document:
        return _generated_schema(vector, document["generatedSchema"])
    reference = document["schemaRef"]
    if reference in document.get("_file_schemas", {}):
        return parse_schema(document["_file_schemas"][reference])
    if reference in BUILTIN_SCHEMAS:
        return BUILTIN_SCHEMAS[reference]
    raise VectorError("%s: unknown schemaRef %r" % (vector, reference))


def _generated_schema(vector, block):
    """Builds a schema without parsing JSON, for type trees deeper than a JSON
    document should be (the §3.4 rule 2 nesting vectors). The only template is
    ``nested-array``: a one-field record ``Nest`` whose field ``v`` is ``depth``
    nested arrays over ``elementType``."""
    if not isinstance(block, dict):
        raise VectorError("%s: 'generatedSchema' must be an object" % vector)
    template = block.get("template")
    element_type = block.get("elementType")
    depth = block.get("depth")
    if (
        template != "nested-array"
        or element_type != "int"
        or isinstance(depth, bool)
        or not isinstance(depth, int)
        or depth < 1
    ):
        raise VectorError(
            "%s: unknown generatedSchema %r (supported: template 'nested-array', "
            "elementType 'int', depth a positive integer)" % (vector, block)
        )
    node = _node("primitive", name="int")
    for _ in range(depth):
        node = _node("array", items=node)
    return _node(
        "record",
        name="Nest",
        namespace=None,
        messageType=None,
        fields=[{"name": "v", "type": node}],
    )


def expect_string(vector, key):
    value = vector.document.get(key)
    if not isinstance(value, str):
        raise VectorError("%s: %r must be a string" % (vector, key))
    return value


def check_vector(report, vector):
    document = vector.document
    if not isinstance(document.get("id"), str) or not document["id"]:
        raise VectorError("%s: every vector needs a non-empty 'id'" % vector.path)
    expects = document.get("expect", "accept")
    if expects not in ("accept", "reject"):
        raise VectorError("%s: 'expect' must be 'accept' or 'reject'" % vector)
    if expects == "reject" and not isinstance(document.get("rejectionRule"), str):
        raise VectorError("%s: a reject vector must state its 'rejectionRule'" % vector)
    report.kinds[vector.kind] = report.kinds.get(vector.kind, 0) + 1
    kind = vector.kind
    if kind == "schema.digest":
        _check_schema_digest(report, vector, expects)
    elif kind == "payload.binary.encode":
        _check_binary_encode(report, vector, expects)
    elif kind == "payload.binary.decode":
        _check_binary_decode(report, vector, expects)
    elif kind == "payload.json.encode":
        _check_json_encode(report, vector, expects)
    elif kind == "payload.json.decode":
        _check_json_decode(report, vector, expects)
    elif kind == "identity.persona-id":
        _check_persona_id(report, vector, expects)
    elif kind == "identity.persona-id-delegation":
        _check_delegation(report, vector)
    elif kind == "identity.realm":
        _check_realm(report, vector)
    elif kind == "identity.pem":
        _check_pem(report, vector, expects)
    elif kind == "crypto.policy-v0":
        _check_policy(report, vector)
    elif kind == "crypto.signature-algorithm-set":
        _check_algorithm_set(report, vector)
    else:
        raise VectorError("%s: unknown kind %r" % (vector, kind))


def _recompute(report, vector, expects, compute, expected_message):
    try:
        actual = compute()
    except Reject as reject:
        report.check(vector, expects == "reject", "reference rejected with %s but the vector expects %s" % (reject, expects))
        return None
    report.check(vector, expects == "accept", expected_message + " (reference accepted)")
    return actual


def _check_schema_digest(report, vector, expects):
    node = _recompute(
        report,
        vector,
        expects,
        lambda: resolve_schema(vector, vector.document),
        "reference rejected the schema",
    )
    if node is None:
        return
    canonical = canonical_form(node)
    digest = schema_digest(node)
    report.check(
        vector,
        canonical == expect_string(vector, "expectedCanonicalForm"),
        "canonical form mismatch:\n    expected %s\n    reference %s" % (expect_string(vector, "expectedCanonicalForm"), canonical),
    )
    report.check(
        vector,
        digest == expect_string(vector, "expectedSchemaId"),
        "schemaId mismatch: expected %s, reference %s" % (expect_string(vector, "expectedSchemaId"), digest),
    )


def _check_binary_encode(report, vector, expects):
    node = resolve_schema(vector, vector.document)
    value = read_vector_value(node, vector.document.get("value"))
    encoded = _recompute(
        report, vector, expects, lambda: encode_binary(node, value), "reference encoded the value"
    )
    if encoded is None:
        return
    expected = expect_string(vector, "expectedHex")
    report.check(
        vector,
        encoded.hex() == expected,
        "binary encoding mismatch:\n    expected %s\n    reference %s" % (expected, encoded.hex()),
    )


def _check_binary_decode(report, vector, expects):
    node = resolve_schema(vector, vector.document)
    data = bytes.fromhex(expect_string(vector, "inputHex"))
    decoded = _recompute(
        report, vector, expects, lambda: decode_binary(node, data), "reference decoded the payload"
    )
    if decoded is None:
        return
    expected = read_vector_value(node, vector.document.get("expectedValue"))
    report.check(
        vector,
        deep_equal(expected, decoded),
        "decoded value mismatch:\n    expected %s\n    reference %s"
        % (json.dumps(vector.document.get("expectedValue"), sort_keys=True, default=repr),
           json.dumps(render_value(node, decoded), sort_keys=True, default=repr)),
    )


def _check_json_encode(report, vector, expects):
    node = resolve_schema(vector, vector.document)
    value = read_vector_value(node, vector.document.get("value"))
    encoded = _recompute(
        report, vector, expects, lambda: encode_json(node, value), "reference encoded the value"
    )
    if encoded is None:
        return
    expected = expect_string(vector, "expectedJson")
    report.check(
        vector,
        encoded == expected,
        "canonical JSON mismatch:\n    expected %s\n    reference %s" % (expected, encoded),
    )


def _check_json_decode(report, vector, expects):
    node = resolve_schema(vector, vector.document)
    text = expect_string(vector, "inputJson")
    decoded = _recompute(
        report, vector, expects, lambda: decode_json(node, text), "reference decoded the payload"
    )
    if decoded is None:
        return
    expected = read_vector_value(node, vector.document.get("expectedValue"))
    report.check(
        vector,
        deep_equal(expected, decoded),
        "decoded value mismatch:\n    expected %s\n    reference %s"
        % (json.dumps(vector.document.get("expectedValue"), sort_keys=True, default=repr),
           json.dumps(render_value(node, decoded), sort_keys=True, default=repr)),
    )


def _check_persona_id(report, vector, expects):
    text = vector.document.get("input")
    parsed = _recompute(
        report, vector, expects, lambda: parse_persona_id(text), "reference accepted the id"
    )
    if parsed is None:
        return
    identifier, thumbprint = parsed
    report.check(
        vector,
        identifier == expect_string(vector, "expectedId"),
        "id mismatch: expected %s, reference %s" % (expect_string(vector, "expectedId"), identifier),
    )
    expected_thumbprint = vector.document.get("expectedThumbprint")
    report.check(
        vector,
        thumbprint == expected_thumbprint,
        "thumbprint mismatch: expected %r, reference %r" % (expected_thumbprint, thumbprint),
    )
    canonical = identifier if thumbprint is None else "%s#%s" % (identifier, thumbprint)
    report.check(
        vector,
        canonical == expect_string(vector, "expectedCanonical"),
        "canonical form mismatch: expected %s, reference %s"
        % (expect_string(vector, "expectedCanonical"), canonical),
    )
    report.check(
        vector,
        is_wildcard(identifier, thumbprint) == vector.document.get("expectedWildcard"),
        "wildcard mismatch",
    )


def _check_delegation(report, vector):
    signer = parse_persona_id(vector.document.get("signer"))
    raw_signed_for = vector.document.get("signedFor")
    if raw_signed_for is None:
        # Java: X.canSignOnBehalfOf(null) is false without ever parsing a target.
        report.check(vector, vector.document.get("expected") is False,
                     "delegation verdict mismatch: null target can never be signed for")
        return
    signed_for = parse_persona_id(raw_signed_for)
    report.check(
        vector,
        can_sign_on_behalf_of(signer, signed_for) == vector.document.get("expected"),
        "delegation verdict mismatch",
    )


def _check_realm(report, vector):
    report.check(
        vector,
        is_valid_realm(vector.document.get("input")) == vector.document.get("expected"),
        "realm verdict mismatch",
    )


def _der_tlvs(buf, start, end):
    """Parses the DER content ``buf[start:end]`` into ``(tag, tlv_start,
    content_start, tlv_end)`` tuples with offsets into ``buf``."""
    tlvs = []
    offset = start
    while offset < end:
        tlv_start = offset
        tag = buf[offset]
        offset += 1
        if offset >= end:
            raise VectorError("truncated DER length")
        length = buf[offset]
        offset += 1
        if length & 0x80:
            nbytes = length & 0x7F
            length = int.from_bytes(buf[offset : offset + nbytes], "big")
            offset += nbytes
        tlvs.append((tag, tlv_start, offset, offset + length))
        offset += length
    return tlvs


def _extract_spki(der, what):
    """Extracts the SubjectPublicKeyInfo TLV of a certificate (RFC 5280) or PKCS#10
    certification request (RFC 2986) body, without a full X.509 model: it is the
    child SEQUENCE of the TBS/request-info shaped as ``SEQUENCE { SEQUENCE {
    OID, ... }, BIT STRING }``."""
    outer = _der_tlvs(der, 0, len(der))
    if not outer or outer[0][0] != 0x30:
        raise VectorError("%s: top-level object is not a SEQUENCE" % what)
    body = _der_tlvs(der, outer[0][2], outer[0][3])
    if not body or body[0][0] != 0x30:
        raise VectorError("%s: the TBS/request-info is not a SEQUENCE" % what)
    for tag, start, content_start, end in _der_tlvs(der, body[0][2], body[0][3]):
        if tag != 0x30:
            continue
        fields = _der_tlvs(der, content_start, end)
        if len(fields) == 2 and fields[0][0] == 0x30 and fields[1][0] == 0x03:
            algorithm = _der_tlvs(der, fields[0][2], fields[0][3])
            if algorithm and algorithm[0][0] == 0x06:
                return der[start:end]
    raise VectorError("%s: no SubjectPublicKeyInfo found" % what)


def _pem_der(report, vector, pem):
    """Decodes the first PEM object's body to DER, recording failures as check failures."""
    body = first_pem_body(pem)
    if body is None:
        report.check(vector, False, "the committed PEM has no parsable first object")
        return None
    try:
        return base64.b64decode(body, validate=True)
    except (binascii.Error, ValueError):
        report.check(vector, False, "the committed PEM has a malformed base64 body")
        return None


def _check_pem(report, vector, expects):
    pem = expect_string(vector, "pem")
    object_type = vector.document.get("objectType")
    expected = vector.document.get("expected")
    if expects == "reject":
        # Rejection depends on the PEM/X.509 library; only the thumbprint
        # expectations of accept vectors are mechanically checkable here.
        report.skip(vector, "PEM rejection is not re-implemented in pure Python")
        return
    if not isinstance(expected, dict):
        report.skip(vector, "no pure-Python X.509/PKCS#8/PKCS#10 model")
        return
    if object_type == "certificate" and "sha256Thumbprint" in expected:
        der = _pem_der(report, vector, pem)
        if der is None:
            return
        digest = hashlib.sha256(der).hexdigest()
        report.check(
            vector,
            digest == expected["sha256Thumbprint"],
            "certificate thumbprint mismatch: expected %s, hashlib(SHA-256(DER)) %s"
            % (expected["sha256Thumbprint"], digest),
        )
        if "publicKeySha256Thumbprint" in expected:
            spki_digest = hashlib.sha256(_extract_spki(der, vector.id)).hexdigest()
            report.check(
                vector,
                spki_digest == expected["publicKeySha256Thumbprint"],
                "certificate public key thumbprint mismatch: expected %s, "
                "hashlib(SHA-256(SPKI DER)) %s"
                % (expected["publicKeySha256Thumbprint"], spki_digest),
            )
        report.skip(vector, "remaining X.509 fields (owner/serial/validity/PEM re-encoding) are not re-implemented")
        return
    if object_type == "publicKey" and "publicKeySha256Thumbprint" in expected:
        # A PUBLIC KEY body IS the SubjectPublicKeyInfo DER: the thumbprint is
        # SHA-256 of the body, no X.509 model needed.
        der = _pem_der(report, vector, pem)
        if der is None:
            return
        digest = hashlib.sha256(der).hexdigest()
        report.check(
            vector,
            digest == expected["publicKeySha256Thumbprint"],
            "public key thumbprint mismatch: expected %s, hashlib(SHA-256(SPKI DER)) %s"
            % (expected["publicKeySha256Thumbprint"], digest),
        )
        report.skip(vector, "PEM re-encoding (publicKeyPem) is not re-implemented")
        return
    if object_type == "csr" and "publicKeySha256Thumbprint" in expected:
        der = _pem_der(report, vector, pem)
        if der is None:
            return
        digest = hashlib.sha256(_extract_spki(der, vector.id)).hexdigest()
        report.check(
            vector,
            digest == expected["publicKeySha256Thumbprint"],
            "CSR public key thumbprint mismatch: expected %s, hashlib(SHA-256(SPKI DER)) %s"
            % (expected["publicKeySha256Thumbprint"], digest),
        )
        report.skip(vector, "remaining PKCS#10 fields (subject CN, PEM re-encoding) are not re-implemented")
        return
    report.skip(vector, "no pure-Python X.509/PKCS#8/PKCS#10 model")


def _check_policy(report, vector):
    field = expect_string(vector, "field")
    if field not in POLICY_V0:
        raise VectorError("%s: unknown policy field %r" % (vector, field))
    expected = vector.document.get("expected")
    reference = POLICY_V0[field]
    if isinstance(expected, list) and reference and isinstance(reference[0], list):
        normalised = [sorted(item) for item in expected]
        report.check(
            vector,
            normalised == reference,
            "policy %s mismatch: expected %r, §8 table %r" % (field, expected, reference),
        )
    elif isinstance(expected, list):
        report.check(
            vector,
            sorted(expected) == sorted(reference),
            "policy %s mismatch: expected %r, §8 table %r" % (field, expected, reference),
        )
    else:
        report.check(
            vector,
            expected == reference,
            "policy %s mismatch: expected %r, §8 table %r" % (field, expected, reference),
        )


def _check_algorithm_set(report, vector):
    algorithms = vector.document.get("signatureAlgorithms")
    if not isinstance(algorithms, list):
        raise VectorError("%s: 'signatureAlgorithms' must be a list" % vector)
    report.check(
        vector,
        supports_signature_algorithm_set(algorithms) == vector.document.get("expected"),
        "signature algorithm set verdict mismatch for %r" % (algorithms,),
    )


def check_directory(directory, verbose=False):
    report = Report(verbose)
    _files, vectors = load_vector_files(directory)
    for vector in vectors:
        try:
            check_vector(report, vector)
        except VectorError as error:
            raise VectorError("%s: %s" % (vector, error))
    return report, vectors


def manifest_check(directory, files, vectors):
    """Cross-checks manifest.json against the discovered files (returns failures)."""
    path = os.path.join(directory, "manifest.json")
    if not os.path.exists(path):
        return ["manifest.json is missing"]
    with open(path, "r", encoding="utf-8") as handle:
        manifest = json.load(handle)
    failures = []
    if manifest.get("vectorFormat") != VECTOR_FORMAT:
        failures.append("manifest.json: wrong vectorFormat")
    if manifest.get("vectorFormatVersion") != VECTOR_FORMAT_VERSION:
        failures.append("manifest.json: wrong vectorFormatVersion")
    listed = {}
    for entry in manifest.get("files", []):
        listed[entry["path"]] = (entry["kind"], entry["vectorCount"])
    discovered = {}
    for file_path in files:
        relative = os.path.relpath(file_path, directory)
        with open(file_path, "r", encoding="utf-8") as handle:
            document = json.load(handle)
        discovered[relative] = (document["kind"], len(document.get("vectors", [])))
    for relative in sorted(set(discovered) - set(listed)):
        failures.append("manifest.json does not list %s" % relative)
    for relative in sorted(set(listed) - set(discovered)):
        failures.append("manifest.json lists %s which does not exist" % relative)
    for relative in sorted(set(listed) & set(discovered)):
        if listed[relative] != discovered[relative]:
            failures.append(
                "manifest.json entry %s is %r but the file is %r" % (relative, listed[relative], discovered[relative])
            )
    total = sum(count for _kind, count in discovered.values())
    if manifest.get("vectorCount") != total:
        failures.append("manifest.json vectorCount is %r but %d vectors were discovered" % (manifest.get("vectorCount"), total))
    discovered_kinds = {}
    for _relative, (kind, count) in discovered.items():
        discovered_kinds[kind] = discovered_kinds.get(kind, 0) + count
    listed_kinds = {}
    for entry in manifest.get("kinds", []):
        listed_kinds[entry["kind"]] = entry["vectorCount"]
    if listed_kinds != discovered_kinds:
        failures.append(
            "manifest.json kinds are %r but discovery counts %r" % (listed_kinds, discovered_kinds)
        )
    return failures


# ---------------------------------------------------------------------------
# Self-test: pins the independent implementation against hand-derived values
# ---------------------------------------------------------------------------

SELFTEST_CASES = [
    ("int 0", '"int"', 0, "00"),
    ("int 1", '"int"', 1, "02"),
    ("int -1", '"int"', -1, "01"),
    ("int 64", '"int"', 64, "8001"),
    ("int -64", '"int"', -64, "7f"),
    ("int MAX", '"int"', 2147483647, "feffffff0f"),
    ("int MIN", '"int"', -2147483648, "ffffffff0f"),
    ("long 150", '"long"', 150, "ac02"),
    ("long -1", '"long"', -1, "01"),
    ("long MAX", '"long"', 9223372036854775807, "feffffffffffffffff01"),
    ("long MIN", '"long"', -9223372036854775808, "ffffffffffffffffff01"),
    ("string foo", '"string"', "foo", "06666f6f"),
    ("string empty", '"string"', "", "00"),
    ("string e-acute", '"string"', "\u00e9", "04c3a9"),
    ("boolean true", '"boolean"', True, "01"),
    ("boolean false", '"boolean"', False, "00"),
    ("double 1.5", '"double"', 1.5, "000000000000f83f"),
    ("double -0.0", '"double"', -0.0, "0000000000000080"),
    ("double 1.0", '"double"', 1.0, "000000000000f03f"),
    ("bytes 010203", '"bytes"', b"\x01\x02\x03", "06010203"),
    ("bytes empty", '"bytes"', b"", "00"),
    ("timestamp 0", '"timestamp-millis"', 0, "00"),
    ("timestamp 150", '"timestamp-millis"', 150, "ac02"),
    ("timestamp 2026-10-10T12:00:00Z", '"timestamp-millis"', 1791633600000, "80b8f9daa468"),
]

SELFTEST_SCHEMA = '{"type":"record","name":"Simple","fields":[{"name":"a","type":"string"}]}'
SELFTEST_CANONICAL = '{"fields":[{"name":"a","type":"string"}],"name":"Simple","type":"record"}'


def selftest():
    failures = []
    for name, type_json, value, expected_hex in SELFTEST_CASES:
        node = parse_schema('{"type":"record","name":"Single","fields":[{"name":"v","type":%s}]}' % type_json)
        actual = encode_binary(node, {"v": value}).hex()
        if actual != expected_hex:
            failures.append("%s: expected %s, got %s" % (name, expected_hex, actual))
    node = parse_schema(SELFTEST_SCHEMA)
    actual = canonical_form(node)
    if actual != SELFTEST_CANONICAL:
        failures.append("canonical form: expected %s, got %s" % (SELFTEST_CANONICAL, actual))
    actual_digest = schema_digest(node)
    if actual_digest != "66cd4cc7f890f80cbda43fa5d69efeccadad0833":
        failures.append("digest: expected 66cd4cc7f890f80cbda43fa5d69efeccadad0833, got %s" % actual_digest)
    for message in failures:
        sys.stderr.write("selftest FAIL %s\n" % message)
    if not failures:
        sys.stdout.write("selftest OK (%d binary cases + canonical form + digest)\n" % len(SELFTEST_CASES))
    return 1 if failures else 0


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def main(argv):
    if len(argv) < 2:
        sys.stderr.write(__doc__)
        return 2
    command = argv[1]
    if command == "check":
        directory = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "vectors")
        verbose = "--verbose" in argv
        if "--vectors" in argv:
            directory = argv[argv.index("--vectors") + 1]
        directory = os.path.normpath(directory)
        report, vectors = check_directory(directory, verbose)
        files, _ = load_vector_files(directory)
        failures = list(report.failures) + manifest_check(directory, files, vectors)
        sys.stdout.write(
            "vector files: %d, vectors: %d, checks: %d, skipped vectors: %d\n"
            % (len(files), len(vectors), report.checked, report.skipped)
        )
        for kind in sorted(report.kinds):
            sys.stdout.write("  %-32s %d\n" % (kind, report.kinds[kind]))
        if verbose:
            for skip in report.skips:
                sys.stdout.write("  skipped %s\n" % skip)
        for failure in failures:
            sys.stdout.write("FAIL %s\n" % failure)
        if failures:
            sys.stdout.write("%d mismatch(es)\n" % len(failures))
            return 1
        sys.stdout.write("OK: every mechanically checkable golden value agrees with the reference\n")
        return 0
    if command == "selftest":
        return selftest()
    if command == "manifest":
        directory = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "vectors")
        if "--vectors" in argv:
            directory = argv[argv.index("--vectors") + 1]
        directory = os.path.normpath(directory)
        files, _vectors = load_vector_files(directory)
        files.sort(key=lambda p: os.path.relpath(p, directory))
        entries = []
        kinds = {}
        total = 0
        for file_path in files:
            relative = os.path.relpath(file_path, directory)
            with open(file_path, "r", encoding="utf-8") as handle:
                document = json.load(handle)
            count = len(document.get("vectors", []))
            entries.append({"path": relative, "kind": document["kind"], "vectorCount": count})
            kinds[document["kind"]] = kinds.get(document["kind"], 0) + count
            total += count
        manifest = {
            "vectorFormat": VECTOR_FORMAT,
            "vectorFormatVersion": VECTOR_FORMAT_VERSION,
            "kind": "manifest",
            "description": "Index of every vector file (see vectors/README.md). Regenerate with 'python3 tools/reference_vectors.py manifest' after adding, removing or renaming a vector file; the reference check and the Java runner both fail when this index is stale.",
            "fileCount": len(entries),
            "vectorCount": total,
            "kinds": [{"kind": kind, "vectorCount": kinds[kind]} for kind in sorted(kinds)],
            "files": entries,
        }
        with open(os.path.join(directory, "manifest.json"), "w", encoding="utf-8") as handle:
            handle.write(json.dumps(manifest, indent=2) + "\n")
        sys.stdout.write("wrote manifest.json: %d files, %d vectors\n" % (len(entries), total))
        return 0
    if (command == "hash" and len(argv) >= 3) or (command in ("encode", "json") and len(argv) >= 4):
        node = parse_schema(argv[2])
        if command == "hash":
            sys.stdout.write("%s\n%s\n" % (canonical_form(node), schema_digest(node)))
            return 0
        value = read_vector_value(node, json.loads(argv[3]))
        if command == "encode":
            sys.stdout.write(encode_binary(node, value).hex() + "\n")
        else:
            sys.stdout.write(encode_json(node, value) + "\n")
        return 0
    sys.stderr.write(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
