package in.yagnyam.persona.schema;

/**
 * The built-in envelope schemas of {@code docs/SCHEMA.md} §5 and their bootstrap registration.
 *
 * <p>{@code Signature} and {@code MultiSignature} are declared <em>inline</em> inside the envelope
 * schemas because named type references are not supported by the model (§1.3 rule 6); the
 * standalone {@code persona.Signature@v1} and {@code persona.MultiSignature@v1} registrations exist
 * only for digest stability testing.
 */
public final class PersonaSchemas {

  /**
   * The {@code Signature} schema of {@code docs/SCHEMA.md} §5, copied verbatim from the
   * normative document. Registered by {@link #bootstrap(SchemaRegistry)} at every
   * {@link SchemaRegistry} construction.
   */
  public static final String SIGNATURE_SCHEMA_JSON =
      """
      {"type":"record","name":"Signature","namespace":"persona","messageType":"persona.Signature@v1",
       "fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}""";

  /**
   * The {@code MultiSignature} schema of {@code docs/SCHEMA.md} §5, copied verbatim from the
   * normative document. Registered by {@link #bootstrap(SchemaRegistry)} at every
   * {@link SchemaRegistry} construction.
   */
  public static final String MULTI_SIGNATURE_SCHEMA_JSON =
      """
      {"type":"record","name":"MultiSignature","namespace":"persona","messageType":"persona.MultiSignature@v1",
       "fields":[{"name":"signedBy","type":"string"},
                 {"name":"signatures","type":{"type":"array","items":{"type":"record","name":"Signature","fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}}}]}""";

  /**
   * The {@code SignedEnvelope} schema of {@code docs/SCHEMA.md} §5, copied verbatim from the
   * normative document. Registered by {@link #bootstrap(SchemaRegistry)} at every
   * {@link SchemaRegistry} construction.
   */
  public static final String SIGNED_ENVELOPE_SCHEMA_JSON =
      """
      {"type":"record","name":"SignedEnvelope","namespace":"persona","messageType":"persona.SignedEnvelope@v1",
       "fields":[{"name":"messageType","type":"string"},
                 {"name":"payloadFormat","type":"string"},
                 {"name":"schemaId","type":"string"},
                 {"name":"payload","type":"bytes"},
                 {"name":"signedBy","type":"string"},
                 {"name":"signatures","type":{"type":"array","items":{"type":"record","name":"Signature","fields":[{"name":"algorithm","type":"string"},{"name":"value","type":"bytes"}]}}}]}""";

  /**
   * The {@code MultiSignedEnvelope} schema of {@code docs/SCHEMA.md} §5, copied verbatim from the
   * normative document. Registered by {@link #bootstrap(SchemaRegistry)} at every
   * {@link SchemaRegistry} construction.
   */
  public static final String MULTI_SIGNED_ENVELOPE_SCHEMA_JSON =
      """
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
       ]}""";

  private static final PersonaSchema SIGNATURE = SchemaParser.parse(SIGNATURE_SCHEMA_JSON);
  private static final PersonaSchema MULTI_SIGNATURE =
      SchemaParser.parse(MULTI_SIGNATURE_SCHEMA_JSON);
  private static final PersonaSchema SIGNED_ENVELOPE =
      SchemaParser.parse(SIGNED_ENVELOPE_SCHEMA_JSON);
  private static final PersonaSchema MULTI_SIGNED_ENVELOPE =
      SchemaParser.parse(MULTI_SIGNED_ENVELOPE_SCHEMA_JSON);

  private PersonaSchemas() {
  }

  /**
   * The parsed {@code persona.Signature@v1} schema (§5).
   *
   * @return the schema
   */
  public static PersonaSchema signatureSchema() {
    return SIGNATURE;
  }

  /**
   * The parsed {@code persona.MultiSignature@v1} schema (§5).
   *
   * @return the schema
   */
  public static PersonaSchema multiSignatureSchema() {
    return MULTI_SIGNATURE;
  }

  /**
   * The parsed {@code persona.SignedEnvelope@v1} schema (§5), the wire shape of an
   * {@link EnvelopeType} with {@code multi == false}.
   *
   * @return the schema
   */
  public static PersonaSchema signedEnvelopeSchema() {
    return SIGNED_ENVELOPE;
  }

  /**
   * The parsed {@code persona.MultiSignedEnvelope@v1} schema (§5), the wire shape of an
   * {@link EnvelopeType} with {@code multi == true}.
   *
   * @return the schema
   */
  public static PersonaSchema multiSignedEnvelopeSchema() {
    return MULTI_SIGNED_ENVELOPE;
  }

  /**
   * Registers the four built-in schemas in the given registry (§5). Every {@link SchemaRegistry}
   * calls this at construction.
   *
   * @param registry the registry to populate
   */
  public static void bootstrap(SchemaRegistry registry) {
    registry.register(SIGNATURE);
    registry.register(MULTI_SIGNATURE);
    registry.register(SIGNED_ENVELOPE);
    registry.register(MULTI_SIGNED_ENVELOPE);
  }
}
