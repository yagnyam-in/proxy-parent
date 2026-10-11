package in.yagnyam.persona.conformance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import in.yagnyam.persona.PersonaCertificate;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Realm;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.Digests;
import in.yagnyam.persona.crypto.PemService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.envelope.SigningPreimage;
import in.yagnyam.persona.schema.ArrayType;
import in.yagnyam.persona.schema.Field;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.PersonaSchemas;
import in.yagnyam.persona.schema.PrimitiveType;
import in.yagnyam.persona.schema.RecordType;
import in.yagnyam.persona.schema.SchemaCanonicalizer;
import in.yagnyam.persona.schema.SchemaParser;
import in.yagnyam.persona.schema.TypeNode;
import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

/**
 * Executes one vector against {@code persona-core-lib}, the implementation under
 * test. The dispatch is the executable definition of the vector kinds of
 * {@code vectors/README.md}: an unknown kind fails the suite instead of being
 * skipped.
 *
 * <p>A reject vector asserts two things: that the operation fails, and — where the
 * implementation under test declares an exception contract — that the failure has
 * the contracted type ({@code IllegalArgumentException} for the schema parser and
 * both payload codecs; {@code RuntimeException} for the persona-id grammar,
 * whose null input surfaces as a {@code NullPointerException}; any
 * {@code Exception} for PEM, whose contract is being tightened). It cannot
 * prove the failure was <em>caused
 * by</em> the cited {@code rejectionRule}: a vector may carry an optional
 * {@code rejectionMessageFragment} that additionally pins a substring of the
 * reference implementation's failure message when that message already names the
 * violated rule. The fragment is reference-implementation specific; a foreign
 * implementation MUST NOT be required to reproduce message text (see
 * {@code vectors/README.md}).
 */
final class VectorKinds {

  static final String SCHEMA_DIGEST = "schema.digest";
  static final String BINARY_ENCODE = "payload.binary.encode";
  static final String BINARY_DECODE = "payload.binary.decode";
  static final String JSON_ENCODE = "payload.json.encode";
  static final String JSON_DECODE = "payload.json.decode";
  static final String PERSONA_ID = "identity.persona-id";
  static final String PERSONA_ID_DELEGATION = "identity.persona-id-delegation";
  static final String REALM = "identity.realm";
  static final String PEM = "identity.pem";
  static final String POLICY_V0 = "crypto.policy-v0";
  static final String ALGORITHM_SET = "crypto.signature-algorithm-set";
  static final String POLICY_V1 = "crypto.policy-v1";
  static final String ALGORITHM_SET_V1 = "crypto.signature-algorithm-set-v1";
  static final String SIGNING_PREIMAGE = "envelope.signing-preimage";
  static final String SIGNATURE_VERIFY = "crypto.signature-verify";

  /** Every kind this runner understands; anything else is a hard failure. */
  static Set<String> knownKinds() {
    return Set.of(
        SCHEMA_DIGEST,
        BINARY_ENCODE,
        BINARY_DECODE,
        JSON_ENCODE,
        JSON_DECODE,
        PERSONA_ID,
        PERSONA_ID_DELEGATION,
        REALM,
        PEM,
        POLICY_V0,
        ALGORITHM_SET,
        POLICY_V1,
        ALGORITHM_SET_V1,
        SIGNING_PREIMAGE,
        SIGNATURE_VERIFY);
  }

  private VectorKinds() {}

  /**
   * Executes one vector.
   *
   * @param vector the vector to execute
   * @throws Exception when a PEM operation fails for a reason the vector did
   *     not mandate
   */
  static void execute(ConformanceVector vector) throws Exception {
    switch (vector.kind()) {
      case SCHEMA_DIGEST -> schemaDigest(vector);
      case BINARY_ENCODE -> binaryEncode(vector);
      case BINARY_DECODE -> binaryDecode(vector);
      case JSON_ENCODE -> jsonEncode(vector);
      case JSON_DECODE -> jsonDecode(vector);
      case PERSONA_ID -> personaId(vector);
      case PERSONA_ID_DELEGATION -> personaIdDelegation(vector);
      case REALM -> realm(vector);
      case PEM -> pem(vector);
      case POLICY_V0 -> policy(vector, PersonaVersion.of("v0"));
      case ALGORITHM_SET -> signatureAlgorithmSet(vector, PersonaVersion.of("v0"));
      case POLICY_V1 -> policy(vector, PersonaVersion.of("v1"));
      case ALGORITHM_SET_V1 -> signatureAlgorithmSet(vector, PersonaVersion.of("v1"));
      case SIGNING_PREIMAGE -> signingPreimage(vector);
      case SIGNATURE_VERIFY -> signatureVerify(vector);
      default -> throw new VectorFormatException(
          vector.describe() + ": unknown vector kind '" + vector.kind()
              + "'; this runner must be taught the kind instead of skipping it");
    }
  }

  /** {@code schema.digest}: canonical form and SHA-1 digest of a schema (§2). */
  private static void schemaDigest(ConformanceVector vector) {
    if (vector.expectsRejection()) {
      // Input extraction stays outside the asserted operation: a malformed
      // vector must fail the suite (VectorFormatException), never count as a
      // rejection of the input.
      String schemaDocument = vector.text("schema");
      assertRejection(vector, IllegalArgumentException.class,
          () -> SchemaParser.parse(schemaDocument));
      return;
    }
    PersonaSchema schema = schema(vector);
    String canonicalForm = SchemaCanonicalizer.canonicalForm(schema);
    assertThat(canonicalForm)
        .as("%s: canonical form (docs/SCHEMA.md §2.2)", vector.describe())
        .isEqualTo(vector.text("expectedCanonicalForm"));
    String digest = SchemaCanonicalizer.digestOf(schema).value();
    assertThat(digest)
        .as("%s: schema digest (docs/SCHEMA.md §2)", vector.describe())
        .isEqualTo(vector.text("expectedSchemaId"));
  }

  /** {@code payload.binary.encode}: the §3 binary codec, writer side. */
  private static void binaryEncode(ConformanceVector vector) {
    PersonaSchema schema = schema(vector);
    Map<String, Object> value =
        VectorValues.toRecordValue(schema.rootRecord(), vector.require("value"));
    BinaryPayloadCodec codec = new BinaryPayloadCodec();
    if (vector.expectsRejection()) {
      assertRejection(vector, IllegalArgumentException.class, () -> codec.encode(schema, value));
      return;
    }
    String actual = hex(codec.encode(schema, value));
    assertThat(actual)
        .as("%s: §3 binary encoding", vector.describe())
        .isEqualTo(vector.text("expectedHex"));
  }

  /** {@code payload.binary.decode}: the §3 binary codec, reader side. */
  private static void binaryDecode(ConformanceVector vector) {
    PersonaSchema schema = schema(vector);
    byte[] input = bytes(vector.text("inputHex"));
    BinaryPayloadCodec codec = new BinaryPayloadCodec();
    if (vector.expectsRejection()) {
      assertRejection(vector, IllegalArgumentException.class, () -> codec.decode(schema, input));
      return;
    }
    Map<String, Object> decoded = codec.decode(schema, input);
    Object expected =
        VectorValues.toCodecValue(schema.rootRecord(), vector.require("expectedValue"));
    VectorValues.assertEquals(expected, decoded, vector.describe() + ": decoded value");
  }

  /** {@code payload.json.encode}: the §4 canonical JSON codec, writer side. */
  private static void jsonEncode(ConformanceVector vector) {
    PersonaSchema schema = schema(vector);
    Map<String, Object> value =
        VectorValues.toRecordValue(schema.rootRecord(), vector.require("value"));
    JsonPayloadCodec codec = new JsonPayloadCodec();
    if (vector.expectsRejection()) {
      assertRejection(vector, IllegalArgumentException.class, () -> codec.encode(schema, value));
      return;
    }
    String actual = new String(codec.encode(schema, value), StandardCharsets.UTF_8);
    assertThat(actual)
        .as("%s: §4 canonical JSON encoding", vector.describe())
        .isEqualTo(vector.text("expectedJson"));
  }

  /** {@code payload.json.decode}: the §4 canonical JSON codec, reader side. */
  private static void jsonDecode(ConformanceVector vector) {
    PersonaSchema schema = schema(vector);
    byte[] input = vector.text("inputJson").getBytes(StandardCharsets.UTF_8);
    JsonPayloadCodec codec = new JsonPayloadCodec();
    if (vector.expectsRejection()) {
      assertRejection(vector, IllegalArgumentException.class, () -> codec.decode(schema, input));
      return;
    }
    Map<String, Object> decoded = codec.decode(schema, input);
    Object expected =
        VectorValues.toCodecValue(schema.rootRecord(), vector.require("expectedValue"));
    VectorValues.assertEquals(expected, decoded, vector.describe() + ": decoded value");
  }

  /** {@code identity.persona-id}: the §3.2 {@code <id>#<thumbprint>} grammar. */
  private static void personaId(ConformanceVector vector) {
    if (vector.expectsRejection()) {
      // Hoisted out of the asserted operation: a malformed vector (e.g. a
      // non-string 'input') must fail the suite, never satisfy the rejection.
      String input = vector.optionalText("input");
      // The declared contract is RuntimeException: PersonaId.of rejects a
      // malformed grammar with IllegalArgumentException but a null input with
      // NullPointerException (a persona-core-lib wart, reported separately;
      // VectorFormatException is an AssertionError and can never pose as
      // either).
      assertRejection(vector, RuntimeException.class, () -> PersonaId.of(input));
      return;
    }
    PersonaId parsed = PersonaId.of(vector.text("input"));
    assertThat(parsed.id())
        .as("%s: id", vector.describe())
        .isEqualTo(vector.text("expectedId"));
    assertThat(parsed.sha256Thumbprint().orElse(null))
        .as("%s: thumbprint (null when unpinned)", vector.describe())
        .isEqualTo(vector.optionalText("expectedThumbprint"));
    assertThat(parsed.uniqueId())
        .as("%s: canonical form", vector.describe())
        .isEqualTo(vector.text("expectedCanonical"));
    assertThat(parsed.isWildcard())
        .as("%s: wildcard flag", vector.describe())
        .isEqualTo(vector.booleanValue("expectedWildcard"));
  }

  /** {@code identity.persona-id-delegation}: the §3.2 delegation rule. */
  private static void personaIdDelegation(ConformanceVector vector) {
    PersonaId signer = PersonaId.of(vector.text("signer"));
    JsonNode target = vector.node("signedFor");
    PersonaId signedFor = target == null || target.isNull() ? null : PersonaId.of(vector.text("signedFor"));
    assertThat(signer.canSignOnBehalfOf(signedFor))
        .as("%s: %s can sign for %s", vector.describe(), signer, target)
        .isEqualTo(vector.booleanValue("expected"));
  }

  /** {@code identity.realm}: realm identifier validity (§3.5). */
  private static void realm(ConformanceVector vector) {
    assertThat(Realm.isValidRealm(vector.optionalText("input")))
        .as("%s: realm validity (PERSONA-PROTOCOL.md §3.5)", vector.describe())
        .isEqualTo(vector.booleanValue("expected"));
  }

  /** {@code identity.pem}: §10.1 PEM decoding and its derived facts. */
  private static void pem(ConformanceVector vector) throws Exception {
    String objectType = vector.text("objectType");
    if (vector.expectsRejection()) {
      // Hoisted out of the asserted operation: a malformed vector (e.g. an
      // unknown 'objectType') must fail the suite, never satisfy the rejection.
      String pem = vector.text("pem");
      // §10.1 mandates that these PEM inputs must not parse. Certificate
      // inputs go through PersonaCertificate.parsePem, whose declared contract
      // is IllegalArgumentException for any invalid certificate PEM (including
      // the empty-body certificate-header-only case, an IOException inside
      // PemService since PR #193); the other object types decode via PemService
      // directly, whose contract is the checked IOException /
      // GeneralSecurityException pair, so their check stays at Exception level.
      Class<? extends Throwable> expectedType =
          "certificate".equals(objectType) ? IllegalArgumentException.class : Exception.class;
      assertRejection(vector, expectedType, () -> decodePem(objectType, pem));
      return;
    }
    checkPemFacts(vector, objectType);
  }

  /**
   * Asserts that {@code operation} fails with the contracted exception type,
   * and — when the vector pins {@code rejectionMessageFragment} — that the
   * reference implementation's failure message names the violated rule. The
   * fragment pins reference behavior only (see {@code vectors/README.md}).
   *
   * @param vector the reject vector
   * @param expectedType the exception type the operation's contract declares
   * @param operation the operation that must fail
   */
  private static void assertRejection(
      ConformanceVector vector,
      Class<? extends Throwable> expectedType,
      org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
    var assertion = assertThatThrownBy(operation)
        .as("%s: must be rejected per %s", vector.describe(), vector.rejectionRule())
        .isInstanceOf(expectedType);
    String fragment = vector.optionalText("rejectionMessageFragment");
    if (fragment != null) {
      assertion
          .as("%s: the rejection must name the violated rule (reference implementation message)",
              vector.describe())
          .hasMessageContaining(fragment);
    }
  }

  /**
   * {@code crypto.policy-v0} / {@code crypto.policy-v1}: the §8 policy table
   * of {@code version}, field by field.
   */
  private static void policy(ConformanceVector vector, PersonaVersion version) {
    String field = vector.text("field");
    JsonNode expected = vector.require("expected");
    assertThat(version.version())
        .as("%s: the Persona Version under test must be %s", vector.describe(), vector.text("policy"))
        .isEqualTo(vector.text("policy"));
    switch (field) {
      case "version" -> assertThat(version.version()).isEqualTo(text(expected));
      case "validSignatureAlgorithmSets" -> assertThat(version.validSignatureAlgorithmSets())
          .isEqualTo(sets(expected));
      case "preferredSignatureAlgorithmSet" -> assertThat(version.preferredSignatureAlgorithmSet())
          .isEqualTo(stringSet(expected));
      case "validAsymmetricEncryptionAlgorithms" -> assertThat(
              version.validAsymmetricEncryptionAlgorithms())
          .isEqualTo(texts(expected));
      case "preferredAsymmetricEncryptionAlgorithm" -> assertThat(
              version.preferredAsymmetricEncryptionAlgorithm())
          .isEqualTo(text(expected));
      case "validSymmetricEncryptionAlgorithms" -> assertThat(
              version.validSymmetricEncryptionAlgorithms())
          .isEqualTo(texts(expected));
      case "preferredSymmetricEncryptionAlgorithm" -> assertThat(
              version.preferredSymmetricEncryptionAlgorithm())
          .isEqualTo(text(expected));
      case "certificateSignatureAlgorithm" -> assertThat(version.certificateSignatureAlgorithm())
          .isEqualTo(text(expected));
      case "keyGenerationAlgorithm" -> assertThat(version.keyGenerationAlgorithm())
          .isEqualTo(text(expected));
      case "encryptionKeyGenerationAlgorithms" -> assertThat(
              version.encryptionKeyGenerationAlgorithms())
          .isEqualTo(texts(expected));
      case "keySize" -> assertThat(version.keySize()).isEqualTo(expected.intValue());
      case "preferredHashAlgorithm" -> assertThat(version.preferredHashAlgorithm())
          .isEqualTo(text(expected));
      default -> throw new VectorFormatException(
          vector.describe() + ": unknown policy field '" + field + "'");
    }
  }

  /**
   * {@code crypto.signature-algorithm-set} (v0) /
   * {@code crypto.signature-algorithm-set-v1}: §11 rule 3 exact-match
   * membership.
   */
  private static void signatureAlgorithmSet(ConformanceVector vector, PersonaVersion version) {
    List<String> algorithms = texts(vector.require("signatureAlgorithms"));
    assertThat(version.supportsSignatureAlgorithmSet(new LinkedHashSet<>(algorithms)))
        .as("%s: acceptance of the signature algorithm set %s under %s (PERSONA-PROTOCOL.md"
            + " §11 rule 3)", vector.describe(), algorithms, version.version())
        .isEqualTo(vector.booleanValue("expected"));
  }

  /**
   * {@code envelope.signing-preimage}: the §6.4 signing preimage, byte-exact.
   *
   * <p>The kind is accept-only: the preimage builder takes the domain as given
   * (the two protocol domains are pinned by accept vectors), so §6.4 mandates
   * no rejection a vector could pin — a malformed vector (bad hex, a missing
   * key) is a bug in the suite and surfaces as {@link VectorFormatException},
   * never as a protocol rejection. An absent {@code schemaId} is JSON
   * {@code null} and means the empty string, exactly as §6.4 requires.
   */
  private static void signingPreimage(ConformanceVector vector) {
    if (vector.expectsRejection()) {
      throw new VectorFormatException(
          vector.describe() + ": kind " + SIGNING_PREIMAGE + " has no negative form; "
              + "the preimage builder accepts every well-formed input");
    }
    String schemaId = vector.optionalText("schemaId");
    byte[] preimage = SigningPreimage.of(
        vector.text("domain"),
        vector.text("messageType"),
        vector.text("payloadFormat"),
        schemaId == null ? "" : schemaId,
        bytes(vector.text("payloadHex")));
    assertThat(hex(preimage))
        .as("%s: §6.4 signing preimage", vector.describe())
        .isEqualTo(vector.text("expectedHex"));
  }

  /**
   * {@code crypto.signature-verify}: RSASSA-PKCS1-v1_5 verification of golden
   * signatures over §6.4 signing preimages (protocol §8, §11 rules 3 and 10).
   *
   * <p>The kind is verify-only (the suite ships no private-key material, so the
   * sign side cannot be goldenized portably — see {@code vectors/README.md}).
   * Accept vectors assert the boolean verdict; the single negative form is the
   * unknown-algorithm reject: an algorithm name outside the v0 policy set
   * {@code {SHA256withRSA}} must be rejected with an error, and the declared
   * {@link in.yagnyam.persona.crypto.CryptographyService#verifySignature(String,
   * java.security.PublicKey, byte[], byte[])} contract throws
   * {@link GeneralSecurityException} for it (the JCA reports an unimplemented
   * algorithm as {@code NoSuchAlgorithmException}). All inputs are extracted —
   * and the public key decoded — outside the asserted operation, so a malformed
   * vector fails the suite ({@link VectorFormatException}) instead of posing as
   * a rejection or a {@code false} verdict.
   */
  private static void signatureVerify(ConformanceVector vector) throws Exception {
    String publicKeyPem = vector.text("publicKeyPem");
    String algorithm = vector.text("algorithm");
    byte[] preimage = bytes(vector.text("preimageHex"));
    byte[] signature = bytes(vector.text("signatureHex"));
    PublicKey key = PemService.instance().decodePublicKey(publicKeyPem);
    if (vector.expectsRejection()) {
      assertRejection(vector, GeneralSecurityException.class,
          () -> BouncyCastleCryptographyService.instance()
              .verifySignature(algorithm, key, preimage, signature));
      return;
    }
    assertThat(BouncyCastleCryptographyService.instance()
            .verifySignature(algorithm, key, preimage, signature))
        .as("%s: signature verdict (PERSONA-PROTOCOL.md §6.4, §8, §11 rule 10)", vector.describe())
        .isEqualTo(vector.booleanValue("expected"));
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  /**
   * Resolves a vector's schema: inline {@code schema} text, or the built-in
   * registration named by {@code schemaRef} (§5).
   */
  private static PersonaSchema schema(ConformanceVector vector) {
    int forms = (vector.has("schema") ? 1 : 0)
        + (vector.has("schemaRef") ? 1 : 0)
        + (vector.has("generatedSchema") ? 1 : 0);
    if (forms > 1) {
      throw new VectorFormatException(
          vector.describe() + ": carries more than one of 'schema', 'schemaRef', 'generatedSchema'");
    }
    if (vector.has("schema")) {
      return SchemaParser.parse(vector.text("schema"));
    }
    if (vector.has("schemaRef")) {
      return builtInSchema(vector.text("schemaRef"));
    }
    if (vector.has("generatedSchema")) {
      return generatedSchema(vector);
    }
    throw new VectorFormatException(
        vector.describe() + ": carries none of 'schema', 'schemaRef', 'generatedSchema'");
  }

  /**
   * Builds the schema a {@code generatedSchema} block describes, without
   * parsing JSON: the templates exist for schemas whose type tree is deeper
   * than a JSON document should be (the §3.4 rule 2 nesting vectors). The only
   * template is {@code nested-array}: a one-field record {@code Nest} whose
   * field {@code v} is {@code depth} nested arrays over {@code elementType}.
   */
  private static PersonaSchema generatedSchema(ConformanceVector vector) {
    JsonNode node = vector.require("generatedSchema");
    String template = text(node.required("template"));
    String elementType = text(node.required("elementType"));
    JsonNode depthNode = node.required("depth");
    if (!depthNode.isIntegralNumber() || !depthNode.canConvertToInt() || depthNode.intValue() < 1) {
      throw new VectorFormatException(
          vector.describe() + ": generatedSchema.depth must be a positive integer");
    }
    if (!"nested-array".equals(template) || !"int".equals(elementType)) {
      throw new VectorFormatException(
          vector.describe() + ": unknown generatedSchema template " + node
              + " (supported: {\"template\":\"nested-array\",\"elementType\":\"int\",\"depth\":N})");
    }
    TypeNode type = new PrimitiveType(elementType);
    for (int i = 0; i < depthNode.intValue(); i++) {
      type = new ArrayType(type);
    }
    return new PersonaSchema(
        new RecordType("Nest", Optional.empty(), Optional.empty(), List.of(new Field("v", type))));
  }

  /** Resolves a {@code schemaRef} against the implementation's §5 built-ins. */
  private static PersonaSchema builtInSchema(String messageType) {
    return switch (messageType) {
      case "persona.Signature@v1" -> PersonaSchemas.signatureSchema();
      case "persona.MultiSignature@v1" -> PersonaSchemas.multiSignatureSchema();
      case "persona.SignedEnvelope@v1" -> PersonaSchemas.signedEnvelopeSchema();
      case "persona.MultiSignedEnvelope@v1" -> PersonaSchemas.multiSignedEnvelopeSchema();
      default -> throw new VectorFormatException(
          "unknown built-in schemaRef '" + messageType + "' (docs/SCHEMA.md §5)");
    };
  }

  /** Checks every fact of an accepting {@code identity.pem} vector. */
  private static void checkPemFacts(ConformanceVector vector, String objectType) throws Exception {
    String pem = vector.text("pem");
    JsonNode expected = vector.require("expected");
    if (!expected.isObject()) {
      throw new VectorFormatException(vector.describe() + ": 'expected' must be an object");
    }
    switch (objectType) {
      case "certificate" -> {
        PersonaCertificate certificate = PersonaCertificate.parsePem(pem);
        Iterator<Map.Entry<String, JsonNode>> facts = expected.fields();
        while (facts.hasNext()) {
          Map.Entry<String, JsonNode> fact = facts.next();
          switch (fact.getKey()) {
            case "subjectCn" -> assertThat(certificate.owner())
                .as("%s: subject CN", vector.describe())
                .isEqualTo(text(fact.getValue()));
            case "serialNumber" -> assertThat(certificate.serialNumber())
                .as("%s: serial number", vector.describe())
                .isEqualTo(text(fact.getValue()));
            case "sha256Thumbprint" -> assertThat(certificate.sha256Thumbprint())
                .as("%s: SHA-256 thumbprint of the DER encoding", vector.describe())
                .isEqualTo(text(fact.getValue()));
            case "validFrom" -> assertThat(certificate.validFrom())
                .as("%s: notBefore", vector.describe())
                .isEqualTo(Instant.parse(text(fact.getValue())));
            case "validTill" -> assertThat(certificate.validTill())
                .as("%s: notAfter", vector.describe())
                .isEqualTo(Instant.parse(text(fact.getValue())));
            case "certificatePem" -> assertThat(certificate.certificatePem())
                .as("%s: re-encoded certificate PEM must be byte-identical", vector.describe())
                .isEqualTo(text(fact.getValue()));
            case "publicKeyPem" -> assertThat(certificate.publicKeyPem())
                .as("%s: re-encoded public key PEM must be byte-identical", vector.describe())
                .isEqualTo(text(fact.getValue()));
            case "publicKeySha256Thumbprint" -> assertThat(certificate.publicKeySha256Thumbprint())
                .as("%s: public key thumbprint", vector.describe())
                .isEqualTo(text(fact.getValue()));
            default -> throw new VectorFormatException(
                vector.describe() + ": unknown expected PEM fact '" + fact.getKey() + "'");
          }
        }
      }
      case "publicKey" -> {
        PublicKey key = PemService.instance().decodePublicKey(pem);
        Iterator<Map.Entry<String, JsonNode>> facts = expected.fields();
        while (facts.hasNext()) {
          Map.Entry<String, JsonNode> fact = facts.next();
          switch (fact.getKey()) {
            case "publicKeyPem" -> assertThat(PemService.instance().encodePublicKey(key))
                .as("%s: re-encoded public key PEM must be byte-identical", vector.describe())
                .isEqualTo(text(fact.getValue()));
            case "publicKeySha256Thumbprint" -> assertThat(Digests.sha256Hex(key.getEncoded()))
                .as("%s: SHA-256 thumbprint of the SubjectPublicKeyInfo DER", vector.describe())
                .isEqualTo(text(fact.getValue()));
            default -> throw new VectorFormatException(
                vector.describe() + ": unknown expected PEM fact '" + fact.getKey() + "'");
          }
        }
      }
      case "privateKey" -> {
        PrivateKey key = PemService.instance().decodePrivateKey(pem);
        Iterator<Map.Entry<String, JsonNode>> facts = expected.fields();
        while (facts.hasNext()) {
          Map.Entry<String, JsonNode> fact = facts.next();
          switch (fact.getKey()) {
            case "privateKeyPem" -> assertThat(PemService.instance().encodePrivateKey(key))
                .as("%s: re-encoded private key PEM must be byte-identical", vector.describe())
                .isEqualTo(text(fact.getValue()));
            default -> throw new VectorFormatException(
                vector.describe() + ": unknown expected PEM fact '" + fact.getKey() + "'");
          }
        }
      }
      case "csr" -> {
        PKCS10CertificationRequest request = PemService.instance().decodeCertificateRequest(pem);
        Iterator<Map.Entry<String, JsonNode>> facts = expected.fields();
        while (facts.hasNext()) {
          Map.Entry<String, JsonNode> fact = facts.next();
          switch (fact.getKey()) {
            case "csrPem" -> assertThat(PemService.instance().encodeCertificateRequest(request))
                .as("%s: re-encoded CSR PEM must be byte-identical", vector.describe())
                .isEqualTo(text(fact.getValue()));
            case "subjectCn" -> assertThat(commonName(request.getSubject()))
                .as("%s: CSR subject CN", vector.describe())
                .isEqualTo(text(fact.getValue()));
            case "publicKeySha256Thumbprint" -> assertThat(
                    Digests.sha256Hex(request.getSubjectPublicKeyInfo().getEncoded()))
                .as("%s: CSR public key thumbprint", vector.describe())
                .isEqualTo(text(fact.getValue()));
            default -> throw new VectorFormatException(
                vector.describe() + ": unknown expected PEM fact '" + fact.getKey() + "'");
          }
        }
      }
      default -> throw new VectorFormatException(
          vector.describe() + ": unknown PEM objectType '" + objectType + "'");
    }
  }

  /** Decodes a PEM object for a reject vector; the exception type is not checked here. */
  private static Object decodePem(String objectType, String pem) throws Exception {
    return switch (objectType) {
      case "publicKey" -> PemService.instance().decodePublicKey(pem);
      case "privateKey" -> PemService.instance().decodePrivateKey(pem);
      case "csr" -> PemService.instance().decodeCertificateRequest(pem);
      case "certificate" -> PersonaCertificate.parsePem(pem);
      default -> throw new VectorFormatException("unknown PEM objectType '" + objectType + "'");
    };
  }

  /** Extracts the single Common Name of an X.500 subject. */
  private static String commonName(X500Name subject) {
    RDN[] names = subject.getRDNs(BCStyle.CN);
    if (names.length != 1) {
      throw new VectorFormatException(
          "expected exactly one CN in " + subject + ", found " + names.length);
    }
    return names[0].getFirst().getValue().toString();
  }

  /** Renders bytes as the vectors' lowercase hex. */
  private static String hex(byte[] bytes) {
    return HexFormat.of().formatHex(bytes);
  }

  /** Parses the vectors' lowercase hex, failing hard on malformed hex. */
  private static byte[] bytes(String hex) {
    if (hex.length() % 2 != 0) {
      throw new VectorFormatException("hex string has an odd length: " + hex);
    }
    for (int i = 0; i < hex.length(); i++) {
      char c = hex.charAt(i);
      if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
        throw new VectorFormatException(
            "hex string must be lowercase [0-9a-f], found '" + c + "' at " + i + " in " + hex);
      }
    }
    return HexFormat.of().parseHex(hex);
  }

  private static String text(JsonNode node) {
    if (!node.isTextual()) {
      throw new VectorFormatException("expected a JSON string, found " + node);
    }
    return node.textValue();
  }

  private static List<String> texts(JsonNode node) {
    if (!node.isArray()) {
      throw new VectorFormatException("expected a JSON array, found " + node);
    }
    List<String> values = new ArrayList<>(node.size());
    node.forEach(item -> values.add(text(item)));
    return values;
  }

  private static Set<String> stringSet(JsonNode node) {
    return new LinkedHashSet<>(texts(node));
  }

  private static List<Set<String>> sets(JsonNode node) {
    if (!node.isArray()) {
      throw new VectorFormatException("expected a JSON array of arrays, found " + node);
    }
    List<Set<String>> sets = new ArrayList<>(node.size());
    node.forEach(item -> sets.add(stringSet(item)));
    return sets;
  }
}
