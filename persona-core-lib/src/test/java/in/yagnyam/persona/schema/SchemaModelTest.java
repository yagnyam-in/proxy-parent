package in.yagnyam.persona.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Tests for the schema model records and the canonical JSON writer helpers: null rejection,
 * defensive copies, primitive validation and the §2.1 writer rules.
 */
class SchemaModelTest {

  @Test
  void primitiveTypeAcceptsOnlyTheSectionTwoPrimitives() {
    for (String name :
        List.of("string", "boolean", "int", "long", "double", "bytes", "timestamp-millis")) {
      assertThat(new PrimitiveType(name).name()).isEqualTo(name);
      assertThat(PrimitiveType.isPrimitive(name)).isTrue();
    }
    assertThat(PrimitiveType.isPrimitive("int32")).isFalse();
    assertThatThrownBy(() -> new PrimitiveType("int32"))
        .isInstanceOf(SchemaValidationException.class)
        .hasMessageContaining("unknown primitive type 'int32'");
    assertThatThrownBy(() -> new PrimitiveType(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void fieldRejectsNulls() {
    assertThatThrownBy(() -> new Field(null, new PrimitiveType("int")))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Field("a", null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void recordTypeRejectsNullsAndCopiesFields() {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("a", new PrimitiveType("int")));
    RecordType record = new RecordType("R", Optional.empty(), Optional.empty(), fields);
    fields.add(new Field("b", new PrimitiveType("int")));

    assertThat(record.fields()).hasSize(1);
    assertThatThrownBy(() -> record.fields().add(new Field("c", new PrimitiveType("int"))))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> new RecordType(null, Optional.empty(), Optional.empty(), List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new RecordType("R", null, Optional.empty(), List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new RecordType("R", Optional.empty(), null, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new RecordType("R", Optional.empty(), Optional.empty(), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void enumTypeCopiesSymbolsAndRejectsNulls() {
    List<String> symbols = new ArrayList<>(List.of("A"));
    EnumType enumType = new EnumType("E", Optional.empty(), symbols);
    symbols.add("B");

    assertThat(enumType.symbols()).containsExactly("A");
    assertThatThrownBy(() -> new EnumType("E", Optional.empty(), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EnumType("E", Optional.empty(), List.of("A", null)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void arrayAndMapRejectNulls() {
    assertThatThrownBy(() -> new ArrayType(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new MapType(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void envelopeTypeResolvesTheBuiltInRecords() {
    assertThat(new EnvelopeType(false).record())
        .isEqualTo(PersonaSchemas.signedEnvelopeSchema().rootRecord());
    assertThat(new EnvelopeType(true).record())
        .isEqualTo(PersonaSchemas.multiSignedEnvelopeSchema().rootRecord());
    assertThat(new EnvelopeType(false).keyword()).isEqualTo(EnvelopeType.ENVELOPE);
    assertThat(new EnvelopeType(true).keyword()).isEqualTo(EnvelopeType.MULTI_ENVELOPE);
  }

  @Test
  void personaSchemaRequiresARecordRoot() {
    assertThatThrownBy(() -> new PersonaSchema(new PrimitiveType("int")))
        .isInstanceOf(SchemaValidationException.class)
        .hasMessageContaining("schema root must be a record");
    assertThatThrownBy(() -> new PersonaSchema(null)).isInstanceOf(NullPointerException.class);
    assertThat(new PersonaSchema(SchemaFixtures.SIMPLE.rootRecord()).messageType()).isEmpty();
    assertThatThrownBy(() -> SchemaFixtures.SIMPLE.field(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void schemaIdValidatesItsHexForm() {
    assertThat(SchemaId.of(SchemaFixtures.SIMPLE_SCHEMA_ID).value())
        .isEqualTo(SchemaFixtures.SIMPLE_SCHEMA_ID);
    assertThat(SchemaId.NONE.value()).isEmpty();
    assertThat(SchemaId.NONE.isEmpty()).isTrue();
    assertThat(SchemaId.of(SchemaFixtures.SIMPLE_SCHEMA_ID).isEmpty()).isFalse();
    assertThatThrownBy(() -> SchemaId.of(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("40 lowercase hex characters");
    assertThatThrownBy(() -> SchemaId.of("ABC"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("40 lowercase hex characters");
    assertThatThrownBy(() -> SchemaId.of("66CD4CC7F890F80CBDA43FA5D69EFECC" + "ADAD0833"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SchemaId.of((String) null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new SchemaId("nope")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void typeNodeKindNamesTheNodeForMessages() {
    assertThat(TypeNode.kindOf(SchemaFixtures.SIMPLE.root())).isEqualTo("record 'Simple'");
    assertThat(TypeNode.kindOf(new EnumType("E", Optional.empty(), List.of("A"))))
        .isEqualTo("enum 'E'");
    assertThat(TypeNode.kindOf(new ArrayType(new PrimitiveType("int")))).isEqualTo("array");
    assertThat(TypeNode.kindOf(new MapType(new PrimitiveType("int")))).isEqualTo("map");
    assertThat(TypeNode.kindOf(new EnvelopeType(false))).isEqualTo("envelope");
    assertThat(TypeNode.kindOf(new EnvelopeType(true))).isEqualTo("multi-envelope");
    assertThat(TypeNode.kindOf(new PrimitiveType("bytes"))).isEqualTo("primitive 'bytes'");
  }

  @Test
  void canonicalJsonEscapesExactlyPerSectionTwoRuleFour() {
    StringBuilder out = new StringBuilder();
    CanonicalJson.writeString(out, "a\"b\\c\b\t\n\f\r\u0000\u001f\u007f é😀");

    assertThat(out.toString())
        .isEqualTo("\"a\\\"b\\\\c\\b\\t\\n\\f\\r\\u0000\\u001f\u007f é😀\"");
  }

  @Test
  void canonicalJsonRejectsIllFormedStrings() {
    assertThat(CanonicalJson.isWellFormed("ok 😀")).isTrue();
    assertThat(CanonicalJson.isWellFormed("\ud83d")).isFalse();
    assertThat(CanonicalJson.isWellFormed("\ude00")).isFalse();
    assertThatThrownBy(() -> CanonicalJson.writeString(new StringBuilder(), "\ud83d"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unpaired surrogate");
    assertThatThrownBy(() -> CanonicalJson.utf8Bytes("\ud83d"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(CanonicalJson.utf8Bytes("é")).containsExactly((byte) 0xc3, (byte) 0xa9);
  }

  @Test
  void canonicalJsonSortsKeysByUtf8Bytes() {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("é", "1");
    values.put("z", "2");
    values.put("a", "3");
    StringBuilder out = new StringBuilder();
    CanonicalJson.writeSortedObject(out, values);

    assertThat(out.toString()).isEqualTo("{\"a\":3,\"z\":2,\"é\":1}");

    List<String> keys = new ArrayList<>(List.of("é", "z", "a"));
    keys.sort(CanonicalJson.UTF8_ORDER);
    assertThat(keys).containsExactly("a", "z", "é");
  }

  @Test
  void canonicalJsonRendersDoublesPerSectionFourFour() {
    assertThat(CanonicalJson.doubleLiteral(1.5d)).isEqualTo("1.5");
    assertThat(CanonicalJson.doubleLiteral(-0.0d)).isEqualTo("-0.0");
    assertThat(CanonicalJson.doubleLiteral(1.0d)).isEqualTo("1.0");
    assertThat(CanonicalJson.doubleLiteral(1e308d)).isEqualTo(Double.toString(1e308d));
    assertThatThrownBy(() -> CanonicalJson.doubleLiteral(Double.NaN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("non-finite");
    assertThatThrownBy(() -> CanonicalJson.doubleLiteral(Double.POSITIVE_INFINITY))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void canonicalJsonSortsIndependentlyOfInsertionOrder() {
    Map<String, String> first = new LinkedHashMap<>();
    first.put("b", "\"x\"");
    first.put("a", "\"y\"");
    Map<String, String> second = new TreeMap<>();
    second.put("a", "\"y\"");
    second.put("b", "\"x\"");

    StringBuilder one = new StringBuilder();
    StringBuilder other = new StringBuilder();
    CanonicalJson.writeSortedObject(one, first);
    CanonicalJson.writeSortedObject(other, second);

    assertThat(one.toString()).isEqualTo("{\"a\":\"y\",\"b\":\"x\"}").isEqualTo(other.toString());
  }

  @Test
  void personaSchemasExposeTheBuiltInSchemas() {
    assertThat(PersonaSchemas.signatureSchema().messageType()).contains("persona.Signature@v1");
    assertThat(PersonaSchemas.multiSignatureSchema().messageType())
        .contains("persona.MultiSignature@v1");
    assertThat(PersonaSchemas.signedEnvelopeSchema().messageType())
        .contains("persona.SignedEnvelope@v1");
    assertThat(PersonaSchemas.multiSignedEnvelopeSchema().messageType())
        .contains("persona.MultiSignedEnvelope@v1");
    assertThat(PersonaSchemas.signedEnvelopeSchema().rootRecord().fields())
        .extracting(Field::name)
        .containsExactly(
            "messageType", "payloadFormat", "schemaId", "payload", "signedBy", "signatures");
    assertThat(PersonaSchemas.multiSignedEnvelopeSchema().rootRecord().fields())
        .extracting(Field::name)
        .containsExactly("messageType", "payloadFormat", "schemaId", "payload", "signatures");
    assertThat(PersonaSchemas.signedEnvelopeSchema().field("signatures").orElseThrow().type())
        .isInstanceOf(ArrayType.class);
    assertThat(PersonaSchemas.signatureSchema().field("value").orElseThrow().type())
        .isEqualTo(new PrimitiveType("bytes"));
  }

  @Test
  void schemaValuesAreUsableAsMapKeys() {
    Map<SchemaId, String> byId = new LinkedHashMap<>();
    byId.put(SchemaCanonicalizer.digestOf(SchemaFixtures.SIMPLE), "simple");
    byId.put(SchemaId.of(SchemaFixtures.SIMPLE_SCHEMA_ID), "simple again");

    assertThat(byId).hasSize(1);
    assertThat(byId).containsValue("simple again");
  }
}
