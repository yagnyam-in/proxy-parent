package in.yagnyam.persona.schema;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical form and SHA-1 digest of a schema ({@code docs/SCHEMA.md} §2).
 *
 * <p>Canonicalization starts from the parsed model, never from the raw JSON text, so source
 * whitespace, key order and formatting cannot change the digest (§6.1). Record nodes include
 * {@code "type":"record"} (§2.2).
 *
 * <p>Field order is preserved (it is semantic); every other key is emitted in UTF-8 byte order by
 * {@link CanonicalJson#writeSortedObject}.
 */
public final class SchemaCanonicalizer {

  private SchemaCanonicalizer() {
  }

  /**
   * Renders the canonical JSON form of a schema (§2.2).
   *
   * @param schema the schema
   * @return the canonical JSON text, without whitespace
   * @throws NullPointerException if {@code schema} is {@code null}
   */
  public static String canonicalForm(PersonaSchema schema) {
    Objects.requireNonNull(schema, "schema");
    StringBuilder out = new StringBuilder();
    writeNode(out, schema.root());
    return out.toString();
  }

  /**
   * Computes {@code schemaId = lowercase_hex(SHA-1(UTF-8(canonicalForm(schema))))} (§2).
   *
   * @param schema the schema
   * @return the schema id
   * @throws NullPointerException if {@code schema} is {@code null}
   */
  public static SchemaId digestOf(PersonaSchema schema) {
    Objects.requireNonNull(schema, "schema");
    byte[] canonical = canonicalForm(schema).getBytes(StandardCharsets.UTF_8);
    return SchemaId.of(sha1Hex(canonical));
  }

  /** Writes a node per §2.2. */
  private static void writeNode(StringBuilder out, TypeNode node) {
    switch (node) {
      case PrimitiveType primitive -> CanonicalJson.writeString(out, primitive.name());
      case EnvelopeType envelope -> CanonicalJson.writeString(out, envelope.keyword());
      case EnumType enumType -> {
        Map<String, String> members = new LinkedHashMap<>();
        members.put("name", stringLiteral(enumType.name()));
        enumType.namespace().ifPresent(value -> members.put("namespace", stringLiteral(value)));
        StringBuilder symbols = new StringBuilder("[");
        for (int i = 0; i < enumType.symbols().size(); i++) {
          if (i > 0) {
            symbols.append(',');
          }
          CanonicalJson.writeString(symbols, enumType.symbols().get(i));
        }
        members.put("symbols", symbols.append(']').toString());
        members.put("type", stringLiteral("enum"));
        CanonicalJson.writeSortedObject(out, members);
      }
      case ArrayType arrayType -> {
        Map<String, String> members = new LinkedHashMap<>();
        members.put("items", nodeJson(arrayType.items()));
        members.put("type", stringLiteral("array"));
        CanonicalJson.writeSortedObject(out, members);
      }
      case MapType mapType -> {
        Map<String, String> members = new LinkedHashMap<>();
        members.put("type", stringLiteral("map"));
        members.put("values", nodeJson(mapType.values()));
        CanonicalJson.writeSortedObject(out, members);
      }
      case RecordType recordType -> {
        StringBuilder fields = new StringBuilder("[");
        for (int i = 0; i < recordType.fields().size(); i++) {
          if (i > 0) {
            fields.append(',');
          }
          fields.append(fieldJson(recordType.fields().get(i)));
        }
        Map<String, String> members = new LinkedHashMap<>();
        members.put("fields", fields.append(']').toString());
        recordType
            .messageType()
            .ifPresent(value -> members.put("messageType", stringLiteral(value)));
        members.put("name", stringLiteral(recordType.name()));
        recordType.namespace().ifPresent(value -> members.put("namespace", stringLiteral(value)));
        members.put("type", stringLiteral("record"));
        CanonicalJson.writeSortedObject(out, members);
      }
    }
  }

  /** Renders a field as {@code {"name":"...","type":<node>}} (§2.2). */
  private static String fieldJson(Field field) {
    Map<String, String> members = new LinkedHashMap<>();
    members.put("name", stringLiteral(field.name()));
    members.put("type", nodeJson(field.type()));
    StringBuilder out = new StringBuilder();
    CanonicalJson.writeSortedObject(out, members);
    return out.toString();
  }

  /** Renders a node to a standalone canonical JSON string. */
  private static String nodeJson(TypeNode node) {
    StringBuilder out = new StringBuilder();
    writeNode(out, node);
    return out.toString();
  }

  /** Renders a JSON string literal. */
  private static String stringLiteral(String value) {
    StringBuilder out = new StringBuilder();
    CanonicalJson.writeString(out, value);
    return out.toString();
  }

  /** Lowercase hex SHA-1 of the given bytes. */
  private static String sha1Hex(byte[] bytes) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(
          "SHA-1 is required by SCHEMA.md §2 but is unavailable in this JVM", e);
    }
    byte[] hash = digest.digest(bytes);
    StringBuilder hex = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      hex.append(Character.forDigit((b >> 4) & 0x0f, 16)).append(Character.forDigit(b & 0x0f, 16));
    }
    return hex.toString();
  }
}
