package in.yagnyam.persona.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SchemaParser}: the §1 example parses, and every §1.3 violation is rejected with a
 * message naming the offending element.
 */
class SchemaParserTest {

  @Test
  void parsesTheSectionOneExample() {
    PersonaSchema schema = SchemaFixtures.GREETING;

    assertThat(schema.rootRecord().name()).isEqualTo("Greeting");
    assertThat(schema.rootRecord().namespace()).contains("example");
    assertThat(schema.messageType()).contains("example.Greeting@v1");
    assertThat(schema.rootRecord().fields()).hasSize(14);
    assertThat(schema.field("issuedAt").orElseThrow().type())
        .isEqualTo(new PrimitiveType("timestamp-millis"));
    assertThat(schema.field("tags").orElseThrow().type())
        .isEqualTo(new ArrayType(new PrimitiveType("string")));
    assertThat(schema.field("attributes").orElseThrow().type())
        .isEqualTo(new MapType(new PrimitiveType("string")));
    assertThat(schema.field("mood").orElseThrow().type())
        .isEqualTo(
            new EnumType(
                "Mood", Optional.empty(), List.of("HAPPY", "NEUTRAL", "SAD")));
    assertThat(schema.field("inner").orElseThrow().type())
        .isEqualTo(
            new RecordType(
                "Inner",
                Optional.empty(),
                Optional.empty(),
                List.of(new Field("x", new PrimitiveType("long")))));
    assertThat(schema.field("forwarded").orElseThrow().type()).isEqualTo(new EnvelopeType(false));
    assertThat(schema.field("missing")).isEmpty();
  }

  @Test
  void parsesMultiEnvelopeNodes() {
    PersonaSchema schema = SchemaFixtures.singleField("\"multi-envelope\"");

    assertThat(schema.field("v").orElseThrow().type()).isEqualTo(new EnvelopeType(true));
  }

  @Test
  void parsesNestedNamespaceAndEmptyFieldList() {
    PersonaSchema schema =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"Outer\",\"namespace\":\"a.b_c\",\"fields\":["
                + "{\"name\":\"e\",\"type\":{\"type\":\"enum\",\"name\":\"E\",\"namespace\":\"x.y\","
                + "\"symbols\":[\"_A1\"]}},"
                + "{\"name\":\"r\",\"type\":{\"type\":\"record\",\"name\":\"R\",\"fields\":[]}}]}");

    assertThat(schema.rootRecord().namespace()).contains("a.b_c");
    assertThat(schema.field("e").orElseThrow().type())
        .isEqualTo(new EnumType("E", Optional.of("x.y"), List.of("_A1")));
    assertThat(schema.field("r").orElseThrow().type())
        .isEqualTo(
            new RecordType(
                "R", Optional.empty(), Optional.empty(), List.of()));
  }

  @Test
  void keyOrderAndWhitespaceDoNotMatter() {
    PersonaSchema one =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"R\",\"namespace\":\"n\",\"messageType\":\"n.R@v1\","
                + "\"fields\":[{\"name\":\"a\",\"type\":\"int\"}]}");
    PersonaSchema other =
        SchemaParser.parse(
            "  {  \"fields\" : [ { \"type\" : \"int\" , \"name\" : \"a\" } ] ,\n"
                + "     \"messageType\":\"n.R@v1\", \"name\":\"R\", \"namespace\":\"n\","
                + "\"type\":\"record\" }  ");

    assertThat(other).isEqualTo(one);
  }

  @Test
  void rejectsInvalidNames() {
    assertRejected("{\"type\":\"record\",\"name\":\"1Bad\",\"fields\":[]}", "record name '1Bad'");
    assertRejected("{\"type\":\"record\",\"name\":\"Bad-Name\",\"fields\":[]}", "record name");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"9x\",\"type\":\"int\"}]}",
        "name of field");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"x\","
            + "\"type\":{\"type\":\"enum\",\"name\":\"E\",\"symbols\":[\"ok\",\"not ok\"]}}]}",
        "symbol of enum");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"x\","
            + "\"type\":{\"type\":\"enum\",\"name\":\"E!\",\"symbols\":[\"A\"]}}]}",
        "enum name 'E!'");
  }

  @Test
  void rejectsInvalidNamespace() {
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"namespace\":\"a..b\",\"fields\":[]}",
        "namespace of record 'R'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"namespace\":\"9a\",\"fields\":[]}",
        "namespace of record 'R'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"x\",\"type\":"
            + "{\"type\":\"record\",\"name\":\"N\",\"namespace\":\"a.\",\"fields\":[]}}]}",
        "namespace of record 'N'");
  }

  @Test
  void rejectsInvalidMessageType() {
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"messageType\":\"example.Greeting@\",\"fields\":[]}",
        "messageType of record 'R'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"messageType\":\"1example.Greeting@v1\",\"fields\":[]}",
        "messageType of record 'R'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"messageType\":\"Greeting v1\",\"fields\":[]}",
        "messageType of record 'R'");
  }

  @Test
  void rejectsDuplicateFieldNames() {
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"},"
            + "{\"name\":\"a\",\"type\":\"long\"}]}",
        "declares field 'a' more than once");
  }

  @Test
  void rejectsDuplicateAndEmptyEnumSymbols() {
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"x\",\"type\":"
            + "{\"type\":\"enum\",\"name\":\"E\",\"symbols\":[\"A\",\"A\"]}}]}",
        "declares symbol 'A' more than once");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"x\",\"type\":"
            + "{\"type\":\"enum\",\"name\":\"E\",\"symbols\":[]}}]}",
        "at least one symbol");
  }

  @Test
  void rejectsNonRecordRoot() {
    assertRejected(
        "{\"type\":\"enum\",\"name\":\"E\",\"symbols\":[\"A\"]}", "schema root must be a record");
    assertRejected("{\"type\":\"array\",\"items\":\"string\"}", "schema root must be a record");
    assertRejected("\"string\"", "schema root must be a JSON object");
  }

  @Test
  void rejectsUnknownPrimitiveAndNamedReferences() {
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int32\"}]}",
        "unknown type name 'int32'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"Mood\"}]}",
        "named type references are not supported");
  }

  @Test
  void rejectsMessageTypeOnNestedRecord() {
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"x\",\"type\":"
            + "{\"type\":\"record\",\"name\":\"N\",\"messageType\":\"n.N@v1\",\"fields\":[]}}]}",
        "only on the root record");
  }

  @Test
  void rejectsMissingRequiredKeys() {
    assertRejected("{\"type\":\"record\",\"name\":\"R\"}", "missing the required 'fields' array");
    assertRejected("{\"type\":\"record\",\"fields\":[]}", "missing required key 'name'");
    assertRejected("{\"name\":\"R\",\"fields\":[]}", "missing required key 'type'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\"}]}",
        "is missing the required 'type'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"type\":\"int\"}]}",
        "missing required key 'name'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
            + "{\"type\":\"array\"}}]}",
        "missing the required 'items'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
            + "{\"type\":\"map\"}}]}",
        "missing the required 'values'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
            + "{\"type\":\"enum\",\"name\":\"E\"}}]}",
        "missing the required 'symbols' array");
  }

  @Test
  void rejectsUnknownKeys() {
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[],\"doc\":\"nope\"}",
        "unknown key(s) [doc]");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\","
            + "\"default\":1}]}",
        "unknown key(s) [default]");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"x\",\"type\":"
            + "{\"type\":\"enum\",\"name\":\"E\",\"symbols\":[\"A\"],\"messageType\":\"e.E@v1\"}}]}",
        "unknown key(s) [messageType]");
  }

  @Test
  void rejectsUnknownNodeTypeAndObjectFormPrimitives() {
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
            + "{\"type\":\"union\",\"items\":\"string\"}}]}",
        "unknown node type 'union'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
            + "{\"type\":\"string\"}}]}",
        "must be written as the JSON string");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
            + "{\"type\":\"envelope\"}}]}",
        "must be written as the JSON string");
  }

  @Test
  void rejectsWrongShapes() {
    assertRejected("[]", "schema root must be a JSON object");
    assertRejected("{\"type\":\"record\",\"name\":\"R\",\"fields\":{}}", "'fields' of record 'R'");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[\"a\"]}", "must be a JSON object");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":1,\"type\":\"int\"}]}",
        "key 'name' must be a JSON string");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
            + "{\"type\":\"enum\",\"name\":\"E\",\"symbols\":\"A\"}}]}",
        "'symbols' of enum 'E' must be a JSON array");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
            + "{\"type\":\"enum\",\"name\":\"E\",\"symbols\":[1]}}]}",
        "must be a JSON string");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":5}]}",
        "must be a JSON string (primitive, envelope or multi-envelope) or a JSON object");
  }

  @Test
  void rejectsEmptyAndMalformedDocuments() {
    assertRejected("", "schema document is empty");
    assertRejected("   ", "schema document is empty");
    assertRejected("{", "schema document is not valid JSON");
    assertRejected(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[]} {}",
        "must contain exactly one JSON value");
  }

  @Test
  void rejectsNullArgument() {
    assertThatThrownBy(() -> SchemaParser.parse(null)).isInstanceOf(NullPointerException.class);
  }

  private static void assertRejected(String json, String expectedFragment) {
    assertThatThrownBy(() -> SchemaParser.parse(json))
        .isInstanceOf(SchemaValidationException.class)
        .hasMessageContaining(expectedFragment);
  }
}
