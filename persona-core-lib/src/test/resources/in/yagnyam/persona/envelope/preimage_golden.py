#!/usr/bin/env python3
"""Independent golden-vector generator for the Persona protocol §6.4 signing preimage.

This script is deliberately written from the normative text of
PERSONA-PROTOCOL.md §6.4 alone, with its own unsigned-LEB128 encoder, so the
hex vectors it prints can be embedded as literals in
``SigningPreimageTest`` and cross-check the Java implementation instead of
re-deriving the expected bytes with the code under test.

Preimage (§6.4, as amended — all five fields uniformly length-prefixed):

    preimage = uvarint(len(domain))       || domain (UTF-8)
             || uvarint(len(messageType)) || messageType (UTF-8)
             || uvarint(len(payloadFormat)) || payloadFormat (UTF-8)
             || uvarint(len(schemaId))    || schemaId (UTF-8; empty if absent)
             || uvarint(len(payload))     || payload (raw bytes)

Note: the formula quoted here previously ended with ``payload (raw bytes)``
without the length prefix — that quote was stale relative to this script's
own encoder (and to the Java implementation), which has always prefixed the
payload length; §6.4 was amended to say so. The output vectors are unchanged.

Run:  python3 preimage_golden.py
"""

SIGNED_DOMAIN = "persona.signed-envelope@v1"
MULTI_DOMAIN = "persona.multi-signed-envelope@v1"


def uvarint(n: int) -> bytes:
    """Unsigned LEB128 encoding of a non-negative length."""
    if n < 0:
        raise ValueError("uvarint expects a non-negative value")
    out = bytearray()
    while True:
        group = n & 0x7F
        n >>= 7
        if n != 0:
            group |= 0x80
        out.append(group)
        if n == 0:
            return bytes(out)


def preimage(domain: str, message_type: str, payload_format: str,
             schema_id: str, payload: bytes) -> bytes:
    out = bytearray()
    for text in (domain, message_type, payload_format, schema_id):
        encoded = text.encode("utf-8")
        out += uvarint(len(encoded)) + encoded
    out += uvarint(len(payload)) + payload
    return bytes(out)


def hex_literal(data: bytes) -> str:
    return "".join(f"{b:02x}" for b in data)


def main() -> None:
    # 1. Small single-signed envelope, absent schemaId (contributes an empty
    #    string), 3-byte payload -> 1-byte length prefixes everywhere.
    single_small = preimage(SIGNED_DOMAIN, "example.Greeting@v1", "json", "",
                            bytes([1, 2, 3]))
    # 2. Identical fields, multi-signed domain: proves domain separation.
    multi_small = preimage(MULTI_DOMAIN, "example.Greeting@v1", "json", "",
                           bytes([1, 2, 3]))
    # 3. Present schemaId (40 lowercase hex) and the binary payload format.
    single_schema = preimage(SIGNED_DOMAIN, "example.Greeting@v1", "persona-binary",
                             "0123456789012345678901234567890123456789",
                             bytes([1, 2, 3]))
    # 4. 200-byte payload: crosses the 1-byte uvarint boundary (200 >= 128,
    #    so the length is the 2-byte sequence c8 01).
    single_200 = preimage(SIGNED_DOMAIN, "example.Greeting@v1", "json", "",
                          bytes(i % 256 for i in range(200)))
    # 5. Empty payload under the multi-signed domain (uvarint(0) = 00).
    multi_empty = preimage(MULTI_DOMAIN, "example.Greeting@v1", "persona-binary",
                           "0123456789012345678901234567890123456789",
                           b"")

    for name, value in [("single_small", single_small),
                        ("multi_small", multi_small),
                        ("single_schema", single_schema),
                        ("single_200", single_200),
                        ("multi_empty", multi_empty)]:
        print(f"{name} ({len(value)} bytes):")
        print(f"  {hex_literal(value)}")


if __name__ == "__main__":
    main()
