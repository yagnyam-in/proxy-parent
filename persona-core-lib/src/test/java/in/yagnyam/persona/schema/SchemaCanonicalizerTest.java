package in.yagnyam.persona.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SchemaCanonicalizer}: the exact canonical form of SCHEMA.md §2.2, digest
 * stability across formatting, and digest sensitivity to every semantic change (§6.1).
 */
class SchemaCanonicalizerTest {

  @Test
  void canonicalizesTheSectionTwoExampleExactly() {
    assertThat(SchemaCanonicalizer.canonicalForm(SchemaFixtures.SIMPLE))
        .isEqualTo(SchemaFixtures.SIMPLE_CANONICAL_FORM);
  }

  @Test
  void digestOfTheSectionTwoExampleMatchesThePinnedSha1() {
    assertThat(SchemaCanonicalizer.digestOf(SchemaFixtures.SIMPLE).value())
        .isEqualTo(SchemaFixtures.SIMPLE_SCHEMA_ID);
    assertThat(SchemaId.of(SchemaFixtures.SIMPLE).value())
        .isEqualTo(SchemaFixtures.SIMPLE_SCHEMA_ID);
  }

  @Test
  void digestOfTheSectionOneExampleMatchesTheReferenceImplementation() {
    assertThat(SchemaCanonicalizer.digestOf(SchemaFixtures.GREETING).value())
        .isEqualTo(SchemaFixtures.GREETING_SCHEMA_ID);
  }

  @Test
  void digestOfTheBuiltInSchemasMatchesTheReferenceImplementation() {
    assertThat(SchemaCanonicalizer.digestOf(PersonaSchemas.signatureSchema()).value())
        .isEqualTo(SchemaFixtures.SIGNATURE_SCHEMA_ID);
    assertThat(SchemaCanonicalizer.digestOf(PersonaSchemas.multiSignatureSchema()).value())
        .isEqualTo(SchemaFixtures.MULTI_SIGNATURE_SCHEMA_ID);
    assertThat(SchemaCanonicalizer.digestOf(PersonaSchemas.signedEnvelopeSchema()).value())
        .isEqualTo(SchemaFixtures.SIGNED_ENVELOPE_SCHEMA_ID);
    assertThat(SchemaCanonicalizer.digestOf(PersonaSchemas.multiSignedEnvelopeSchema()).value())
        .isEqualTo(SchemaFixtures.MULTI_SIGNED_ENVELOPE_SCHEMA_ID);
  }

  @Test
  void recordNodesIncludeTheRecordKeyword() {
    PersonaSchema nested =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"Outer\",\"fields\":[{\"name\":\"inner\",\"type\":"
                + "{\"type\":\"record\",\"name\":\"Inner\",\"fields\":[{\"name\":\"x\","
                + "\"type\":\"long\"}]}}]}");

    assertThat(SchemaCanonicalizer.canonicalForm(nested))
        .isEqualTo(
            "{\"fields\":[{\"name\":\"inner\",\"type\":{\"fields\":[{\"name\":\"x\","
                + "\"type\":\"long\"}],\"name\":\"Inner\",\"type\":\"record\"}}],"
                + "\"name\":\"Outer\",\"type\":\"record\"}");
  }

  @Test
  void canonicalizesEnumArrayMapAndEnvelopeNodes() {
    PersonaSchema schema =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"R\",\"namespace\":\"n\",\"messageType\":\"n.R@v1\","
                + "\"fields\":["
                + "{\"name\":\"e\",\"type\":{\"type\":\"enum\",\"name\":\"E\",\"namespace\":\"n\","
                + "\"symbols\":[\"B\",\"A\"]}},"
                + "{\"name\":\"a\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},"
                + "{\"name\":\"m\",\"type\":{\"type\":\"map\",\"values\":\"bytes\"}},"
                + "{\"name\":\"x\",\"type\":\"envelope\"},"
                + "{\"name\":\"y\",\"type\":\"multi-envelope\"}]}");

    assertThat(SchemaCanonicalizer.canonicalForm(schema))
        .isEqualTo(
            "{\"fields\":["
                + "{\"name\":\"e\",\"type\":{\"name\":\"E\",\"namespace\":\"n\","
                + "\"symbols\":[\"B\",\"A\"],\"type\":\"enum\"}},"
                + "{\"name\":\"a\",\"type\":{\"items\":\"int\",\"type\":\"array\"}},"
                + "{\"name\":\"m\",\"type\":{\"type\":\"map\",\"values\":\"bytes\"}},"
                + "{\"name\":\"x\",\"type\":\"envelope\"},"
                + "{\"name\":\"y\",\"type\":\"multi-envelope\"}],"
                + "\"messageType\":\"n.R@v1\",\"name\":\"R\",\"namespace\":\"n\",\"type\":\"record\"}");
  }

  @Test
  void digestIgnoresSourceFormatting() {
    PersonaSchema compact =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"R\",\"namespace\":\"n\",\"messageType\":\"n.R@v1\","
                + "\"fields\":[{\"name\":\"a\",\"type\":\"int\"},{\"name\":\"b\",\"type\":"
                + "{\"type\":\"array\",\"items\":\"string\"}}]}");
    PersonaSchema pretty =
        SchemaParser.parse(
            "{\n"
                + "    \"messageType\" : \"n.R@v1\",\n"
                + "    \"fields\" : [\n"
                + "        { \"type\" : \"int\", \"name\" : \"a\" },\n"
                + "        { \"type\" : { \"items\" : \"string\", \"type\" : \"array\" },\n"
                + "          \"name\" : \"b\" }\n"
                + "    ],\n"
                + "    \"namespace\" : \"n\",\n"
                + "    \"name\" : \"R\",\n"
                + "    \"type\" : \"record\"\n"
                + "}\n");

    assertThat(pretty).isEqualTo(compact);
    assertThat(SchemaCanonicalizer.canonicalForm(pretty))
        .isEqualTo(SchemaCanonicalizer.canonicalForm(compact));
    assertThat(SchemaCanonicalizer.digestOf(pretty))
        .isEqualTo(SchemaCanonicalizer.digestOf(compact));
  }

  @Test
  void fieldRenameChangesTheDigest() {
    assertDigestDiffers(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"}]}",
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"b\",\"type\":\"int\"}]}");
  }

  @Test
  void typeChangeChangesTheDigest() {
    assertDigestDiffers(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"}]}",
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"long\"}]}");
  }

  @Test
  void fieldOrderChangeChangesTheDigest() {
    assertDigestDiffers(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"},"
            + "{\"name\":\"b\",\"type\":\"int\"}]}",
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"b\",\"type\":\"int\"},"
            + "{\"name\":\"a\",\"type\":\"int\"}]}");
  }

  @Test
  void enumSymbolOrderChangeChangesTheDigest() {
    assertDigestDiffers(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"e\",\"type\":{\"type\":\"enum\","
            + "\"name\":\"E\",\"symbols\":[\"A\",\"B\"]}}]}",
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"e\",\"type\":{\"type\":\"enum\","
            + "\"name\":\"E\",\"symbols\":[\"B\",\"A\"]}}]}");
  }

  @Test
  void addedFieldChangesTheDigest() {
    assertDigestDiffers(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"}]}",
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"},"
            + "{\"name\":\"b\",\"type\":\"int\"}]}");
  }

  @Test
  void addedNamespaceChangesTheDigest() {
    assertDigestDiffers(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"}]}",
        "{\"type\":\"record\",\"name\":\"R\",\"namespace\":\"n\",\"fields\":[{\"name\":\"a\","
            + "\"type\":\"int\"}]}");
  }

  @Test
  void addedMessageTypeChangesTheDigest() {
    assertDigestDiffers(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"}]}",
        "{\"type\":\"record\",\"name\":\"R\",\"messageType\":\"n.R@v1\",\"fields\":[{\"name\":\"a\","
            + "\"type\":\"int\"}]}");
  }

  @Test
  void renamedRecordChangesTheDigest() {
    assertDigestDiffers(
        "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"}]}",
        "{\"type\":\"record\",\"name\":\"S\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"}]}");
  }

  @Test
  void rejectsNullSchema() {
    assertThatThrownBy(() -> SchemaCanonicalizer.canonicalForm(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> SchemaCanonicalizer.digestOf(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void schemaIdsAreAlwaysFortyLowercaseHexCharacters() {
    assertThat(SchemaCanonicalizer.digestOf(SchemaFixtures.KITCHEN).value())
        .matches("[0-9a-f]{40}");
    assertThat(SchemaId.NONE.value()).isEmpty();
    assertThat(SchemaId.NONE.isEmpty()).isTrue();
  }

  @Test
  void recordEqualityIsStructural() {
    PersonaSchema one =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
                + "{\"type\":\"enum\",\"name\":\"E\",\"symbols\":[\"A\"]}}]}");
    PersonaSchema other =
        SchemaParser.parse(
            "{\"type\":\"record\",\"name\":\"R\",\"fields\":[{\"name\":\"a\",\"type\":"
                + "{\"type\":\"enum\",\"name\":\"E\",\"symbols\":[\"A\"]}}]}");

    assertThat(other).isEqualTo(one);
    assertThat(other.hashCode()).isEqualTo(one.hashCode());
    assertThat(other.rootRecord().fields().get(0).type())
        .isEqualTo(new EnumType("E", Optional.empty(), List.of("A")));
  }

  private static void assertDigestDiffers(String first, String second) {
    SchemaId firstId = SchemaCanonicalizer.digestOf(SchemaParser.parse(first));
    SchemaId secondId = SchemaCanonicalizer.digestOf(SchemaParser.parse(second));
    assertThat(secondId).isNotEqualTo(firstId);
  }
}
