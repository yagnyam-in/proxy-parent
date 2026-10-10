package in.yagnyam.persona.schema;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses a schema JSON document into a {@link PersonaSchema} ({@code docs/SCHEMA.md} §1) and
 * enforces every validation rule of §1.3:
 *
 * <ol>
 *   <li>names (records, enums, fields, symbols) match {@code ^[A-Za-z_][A-Za-z0-9_]*$};
 *   <li>namespaces match {@code ^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$};
 *   <li>messageType matches the §1.3 rule 3 regex;
 *   <li>record field names are unique; enum symbols are unique and non-empty;
 *   <li>there are no unions or optional fields: a field without a type, or an unknown type, is
 *       rejected;
 *   <li>nested records/enums are declared inline: a bare type name that is not a primitive is
 *       rejected (no named references);
 *   <li>{@code messageType} appears at most once and only on the root record.
 * </ol>
 *
 * <p>Additionally every node object must be written with exactly the keys of §1.1 — unknown keys
 * are rejected rather than silently dropped, because the schema digest (§2) only covers the parsed
 * model and an ignored key would otherwise let two different documents share a digest.
 */
public final class SchemaParser {

  private static final Pattern NAME = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
  private static final Pattern NAMESPACE =
      Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*$");
  private static final Pattern MESSAGE_TYPE =
      Pattern.compile(
          "^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*@[A-Za-z0-9][A-Za-z0-9._-]*$");

  private static final Set<String> RECORD_KEYS =
      Set.of("type", "name", "namespace", "messageType", "fields");
  private static final Set<String> ENUM_KEYS = Set.of("type", "name", "namespace", "symbols");
  private static final Set<String> ARRAY_KEYS = Set.of("type", "items");
  private static final Set<String> MAP_KEYS = Set.of("type", "values");
  private static final Set<String> FIELD_KEYS = Set.of("name", "type");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private SchemaParser() {
  }

  /**
   * Parses and validates a schema document.
   *
   * @param schemaJson the schema JSON text
   * @return the parsed schema
   * @throws NullPointerException if {@code schemaJson} is {@code null}
   * @throws SchemaValidationException if the document is not valid JSON or violates §1.3
   */
  public static PersonaSchema parse(String schemaJson) {
    Objects.requireNonNull(schemaJson, "schemaJson");
    JsonNode document = readJson(schemaJson, "schema document");
    if (!document.isObject()) {
      throw new SchemaValidationException(
          "schema root must be a JSON object describing a record, found "
              + document.getNodeType()
              + " (SCHEMA.md §1.1)");
    }
    TypeNode root = parseNode(document, true, "root");
    if (!(root instanceof RecordType record)) {
      throw new SchemaValidationException(
          "schema root must be a record, found " + TypeNode.kindOf(root) + " (SCHEMA.md §1.1)");
    }
    return new PersonaSchema(record);
  }

  /** Reads exactly one JSON value from the given text. */
  private static JsonNode readJson(String json, String what) {
    try (JsonParser parser = MAPPER.getFactory().createParser(json)) {
      JsonNode document = MAPPER.readTree(parser);
      if (parser.nextToken() != null) {
        throw new SchemaValidationException(
            what + " must contain exactly one JSON value, found trailing content");
      }
      if (document == null || document.isMissingNode()) {
        throw new SchemaValidationException(what + " is empty");
      }
      return document;
    } catch (IOException e) {
      throw new SchemaValidationException(what + " is not valid JSON: " + e.getMessage(), e);
    }
  }

  /** Parses a type node (§1.1, §1.2). */
  private static TypeNode parseNode(JsonNode node, boolean root, String location) {
    if (node.isTextual()) {
      return parseKeyword(node.textValue(), location);
    }
    if (!node.isObject()) {
      throw new SchemaValidationException(
          location
              + ": a type node must be a JSON string (primitive, envelope or multi-envelope) or a"
              + " JSON object (record, enum, array or map), found "
              + node.getNodeType());
    }
    String type = requireText(node, "type", location);
    return switch (type) {
      case "record" -> parseRecord(node, root, location);
      case "enum" -> parseEnum(node, location);
      case "array" -> parseArray(node, location);
      case "map" -> parseMap(node, location);
      default -> throw unknownNodeType(type, location);
    };
  }

  /** Parses the bare-string node kinds: primitives (§1.2) and envelopes (§1.1). */
  private static TypeNode parseKeyword(String keyword, String location) {
    if (PrimitiveType.isPrimitive(keyword)) {
      return new PrimitiveType(keyword);
    }
    if (EnvelopeType.ENVELOPE.equals(keyword)) {
      return new EnvelopeType(false);
    }
    if (EnvelopeType.MULTI_ENVELOPE.equals(keyword)) {
      return new EnvelopeType(true);
    }
    throw new SchemaValidationException(
        location
            + ": unknown type name '"
            + keyword
            + "'; named type references are not supported, nested records and enums must be"
            + " declared inline (SCHEMA.md §1.3 rule 6)");
  }

  /** Builds the error for an object node whose {@code type} is not a node kind. */
  private static SchemaValidationException unknownNodeType(String type, String location) {
    String hint =
        PrimitiveType.isPrimitive(type)
                || EnvelopeType.ENVELOPE.equals(type)
                || EnvelopeType.MULTI_ENVELOPE.equals(type)
            ? "; '" + type + "' is a bare-string node kind and must be written as the JSON"
                + " string \"" + type + "\", not as an object (SCHEMA.md §1.1)"
            : "";
    return new SchemaValidationException(location + ": unknown node type '" + type + "'" + hint);
  }

  /** Parses a record node (§1.1) and applies §1.3 rules 1, 2, 3, 4 and 7. */
  private static RecordType parseRecord(JsonNode object, boolean root, String location) {
    rejectUnknownKeys(object, RECORD_KEYS, location, "record");
    String name = requireText(object, "name", location);
    requireName(name, "record name");
    Optional<String> namespace = optionalText(object, "namespace", location);
    namespace.ifPresent(value -> requireNamespace(value, "namespace of record '" + name + "'"));
    Optional<String> messageType = optionalText(object, "messageType", location);
    messageType.ifPresent(
        value -> requireMessageType(value, "messageType of record '" + name + "'"));
    if (messageType.isPresent() && !root) {
      throw new SchemaValidationException(
          location
              + ": record '"
              + name
              + "' declares a messageType, but at most one messageType declaration is allowed and"
              + " only on the root record (SCHEMA.md §1.3 rule 7)");
    }
    JsonNode fieldsNode = object.get("fields");
    if (fieldsNode == null) {
      throw new SchemaValidationException(
          location + ": record '" + name + "' is missing the required 'fields' array");
    }
    if (!fieldsNode.isArray()) {
      throw new SchemaValidationException(
          location
              + ": 'fields' of record '"
              + name
              + "' must be a JSON array, found "
              + fieldsNode.getNodeType());
    }
    List<Field> fields = new ArrayList<>();
    Set<String> fieldNames = new LinkedHashSet<>();
    for (JsonNode fieldNode : fieldsNode) {
      if (!fieldNode.isObject()) {
        throw new SchemaValidationException(
            location
                + ": every entry of 'fields' of record '"
                + name
                + "' must be a JSON object, found "
                + fieldNode.getNodeType());
      }
      String fieldLocation = "field of record '" + name + "' at " + location;
      rejectUnknownKeys(fieldNode, FIELD_KEYS, fieldLocation, "field");
      String fieldName = requireText(fieldNode, "name", fieldLocation);
      requireName(fieldName, "name of field in record '" + name + "' at " + location);
      if (!fieldNames.add(fieldName)) {
        throw new SchemaValidationException(
            location
                + ": record '"
                + name
                + "' declares field '"
                + fieldName
                + "' more than once (SCHEMA.md §1.3 rule 4)");
      }
      JsonNode typeNode = fieldNode.get("type");
      if (typeNode == null) {
        throw new SchemaValidationException(
            location
                + ": field '"
                + fieldName
                + "' of record '"
                + name
                + "' is missing the required 'type'");
      }
      fields.add(
          new Field(
              fieldName,
              parseNode(
                  typeNode, false, "type of field '" + fieldName + "' of record '" + name + "'")));
    }
    return new RecordType(name, namespace, messageType, fields);
  }

  /** Parses an enum node (§1.1) and applies §1.3 rules 1, 2 and 4. */
  private static EnumType parseEnum(JsonNode object, String location) {
    rejectUnknownKeys(object, ENUM_KEYS, location, "enum");
    String name = requireText(object, "name", location);
    requireName(name, "enum name");
    Optional<String> namespace = optionalText(object, "namespace", location);
    namespace.ifPresent(value -> requireNamespace(value, "namespace of enum '" + name + "'"));
    JsonNode symbolsNode = object.get("symbols");
    if (symbolsNode == null) {
      throw new SchemaValidationException(
          location + ": enum '" + name + "' is missing the required 'symbols' array");
    }
    if (!symbolsNode.isArray()) {
      throw new SchemaValidationException(
          location
              + ": 'symbols' of enum '"
              + name
              + "' must be a JSON array, found "
              + symbolsNode.getNodeType());
    }
    List<String> symbols = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (JsonNode symbolNode : symbolsNode) {
      if (!symbolNode.isTextual()) {
        throw new SchemaValidationException(
            location
                + ": every symbol of enum '"
                + name
                + "' must be a JSON string, found "
                + symbolNode.getNodeType());
      }
      String symbol = symbolNode.textValue();
      requireName(symbol, "symbol of enum '" + name + "' at " + location);
      if (!seen.add(symbol)) {
        throw new SchemaValidationException(
            location
                + ": enum '"
                + name
                + "' declares symbol '"
                + symbol
                + "' more than once (SCHEMA.md §1.3 rule 4)");
      }
      symbols.add(symbol);
    }
    if (symbols.isEmpty()) {
      throw new SchemaValidationException(
          location
              + ": enum '"
              + name
              + "' must declare at least one symbol (SCHEMA.md §1.3 rule 4)");
    }
    return new EnumType(name, namespace, symbols);
  }

  /** Parses an array node (§1.1). */
  private static ArrayType parseArray(JsonNode object, String location) {
    rejectUnknownKeys(object, ARRAY_KEYS, location, "array");
    JsonNode items = object.get("items");
    if (items == null) {
      throw new SchemaValidationException(
          location + ": array node is missing the required 'items' type");
    }
    return new ArrayType(parseNode(items, false, "items of the array at " + location));
  }

  /** Parses a map node (§1.1). */
  private static MapType parseMap(JsonNode object, String location) {
    rejectUnknownKeys(object, MAP_KEYS, location, "map");
    JsonNode values = object.get("values");
    if (values == null) {
      throw new SchemaValidationException(
          location + ": map node is missing the required 'values' type");
    }
    return new MapType(parseNode(values, false, "values of the map at " + location));
  }

  /** Reads a required string key. */
  private static String requireText(JsonNode object, String key, String location) {
    JsonNode value = object.get(key);
    if (value == null) {
      throw new SchemaValidationException(
          location + ": missing required key '" + key + "'");
    }
    if (!value.isTextual()) {
      throw new SchemaValidationException(
          location + ": key '" + key + "' must be a JSON string, found " + value.getNodeType());
    }
    return value.textValue();
  }

  /** Reads an optional string key. */
  private static Optional<String> optionalText(JsonNode object, String key, String location) {
    if (!object.has(key)) {
      return Optional.empty();
    }
    return Optional.of(requireText(object, key, location));
  }

  /** Rejects keys that are not part of a node kind's shape (§1.1). */
  private static void rejectUnknownKeys(
      JsonNode object, Set<String> allowed, String location, String what) {
    List<String> unknown = new ArrayList<>();
    object.fieldNames().forEachRemaining(key -> {
      if (!allowed.contains(key)) {
        unknown.add(key);
      }
    });
    if (!unknown.isEmpty()) {
      throw new SchemaValidationException(
          location
              + ": "
              + what
              + " node has unknown key(s) "
              + unknown
              + "; allowed keys are "
              + allowed.stream().sorted().toList()
              + " (SCHEMA.md §1.1)");
    }
  }

  /** Validates an identifier (§1.3 rule 1). */
  private static void requireName(String value, String what) {
    if (!NAME.matcher(value).matches()) {
      throw new SchemaValidationException(
          what + " '" + value + "' must match " + NAME.pattern() + " (SCHEMA.md §1.3 rule 1)");
    }
  }

  /** Validates a namespace (§1.3 rule 2). */
  private static void requireNamespace(String value, String what) {
    if (!NAMESPACE.matcher(value).matches()) {
      throw new SchemaValidationException(
          what
              + " '"
              + value
              + "' must match "
              + NAMESPACE.pattern()
              + " (SCHEMA.md §1.3 rule 2)");
    }
  }

  /** Validates a messageType (§1.3 rule 3). */
  private static void requireMessageType(String value, String what) {
    if (!MESSAGE_TYPE.matcher(value).matches()) {
      throw new SchemaValidationException(
          what
              + " '"
              + value
              + "' must match "
              + MESSAGE_TYPE.pattern()
              + " (SCHEMA.md §1.3 rule 3)");
    }
  }
}
