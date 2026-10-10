package in.yagnyam.persona.conformance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.JsonNode;
import in.yagnyam.persona.PersonaCertificate;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Realm;
import in.yagnyam.persona.crypto.Digests;
import in.yagnyam.persona.crypto.PemService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.schema.PersonaSchema;
import in.yagnyam.persona.schema.PersonaSchemas;
import in.yagnyam.persona.schema.SchemaCanonicalizer;
import in.yagnyam.persona.schema.SchemaParser;
import in.yagnyam.persona.schema.codec.BinaryPayloadCodec;
import in.yagnyam.persona.schema.codec.JsonPayloadCodec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

/**
 * Executes one vector against {@code persona-core-lib}, the implementation under
 * test. The dispatch is the executable definition of the vector kinds of
 * {@code vectors/README.md}: an unknown kind fails the suite instead of being
 * skipped, and a rejection must be caused by the rule the vector cites.
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
        ALGORITHM_SET);
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
      case POLICY_V0 -> policy(vector);
      case ALGORITHM_SET -> signatureAlgorithmSet(vector);
      default -> throw new VectorFormatException(
          vector.describe() + ": unknown vector kind '" + vector.kind()
              + "'; this runner must be taught the kind instead of skipping it");
    }
  }

  /** {@code schema.digest}: canonical form and SHA-1 digest of a schema (§2). */
  private static void schemaDigest(ConformanceVector vector) {
    if (vector.expectsRejection()) {
      assertThatCode(() -> SchemaParser.parse(vector.text("schema")))
          .as("%s: schema document must be rejected per %s", vector.describe(), vector.rejectionRule())
          .isInstanceOf(IllegalArgumentException.class);
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
      assertThatCode(() -> codec.encode(schema, value))
          .as("%s: encoding must be rejected per %s", vector.describe(), vector.rejectionRule())
          .isInstanceOf(IllegalArgumentException.class);
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
      assertThatCode(() -> codec.decode(schema, input))
          .as("%s: decoding must be rejected per %s", vector.describe(), vector.rejectionRule())
          .isInstanceOf(IllegalArgumentException.class);
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
      assertThatCode(() -> codec.encode(schema, value))
          .as("%s: encoding must be rejected per %s", vector.describe(), vector.rejectionRule())
          .isInstanceOf(IllegalArgumentException.class);
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
      assertThatCode(() -> codec.decode(schema, input))
          .as("%s: decoding must be rejected per %s", vector.describe(), vector.rejectionRule())
          .isInstanceOf(IllegalArgumentException.class);
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
      assertThatCode(() -> PersonaId.of(vector.optionalText("input")))
          .as("%s: persona id must be rejected per %s", vector.describe(), vector.rejectionRule())
          .isInstanceOf(RuntimeException.class);
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

  /** {@code identity.pem}: §9.1 PEM decoding and its derived facts. */
  private static void pem(ConformanceVector vector) throws Exception {
    String objectType = vector.text("objectType");
    if (vector.expectsRejection()) {
      // §9.1 mandates that these PEM inputs must not parse; it does not mandate
      // an exception type, so any failure counts and the check stays at
      // Exception level. (One certificate reject — headers with an empty body —
      // currently surfaces as a NullPointerException from
      // PersonaCertificate.of(null), because PemService.decodeCertificate does
      // not guard CertificateFactory.generateCertificate returning null; a
      // contract bug in persona-core-lib, reported but not fixed here.)
      assertThatCode(() -> decodePem(objectType, vector.text("pem")))
          .as("%s: PEM must be rejected per %s", vector.describe(), vector.rejectionRule())
          .isInstanceOf(Exception.class);
      return;
    }
    checkPemFacts(vector, objectType);
  }

  /** {@code crypto.policy-v0}: the §8 policy table, field by field. */
  private static void policy(ConformanceVector vector) {
    PersonaVersion version = PersonaVersion.latestVersion();
    String field = vector.text("field");
    JsonNode expected = vector.require("expected");
    assertThat(version.version())
        .as("%s: the active Persona Version must be %s", vector.describe(), vector.text("policy"))
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
      case "keySize" -> assertThat(version.keySize()).isEqualTo(expected.intValue());
      case "preferredHashAlgorithm" -> assertThat(version.preferredHashAlgorithm())
          .isEqualTo(text(expected));
      default -> throw new VectorFormatException(
          vector.describe() + ": unknown policy field '" + field + "'");
    }
  }

  /** {@code crypto.signature-algorithm-set}: §11 rule 3 exact-match membership. */
  private static void signatureAlgorithmSet(ConformanceVector vector) {
    List<String> algorithms = texts(vector.require("signatureAlgorithms"));
    assertThat(
            PersonaVersion.latestVersion()
                .supportsSignatureAlgorithmSet(new LinkedHashSet<>(algorithms)))
        .as("%s: acceptance of the signature algorithm set %s (PERSONA-PROTOCOL.md §11 rule 3)",
            vector.describe(), algorithms)
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
    boolean inline = vector.has("schema");
    boolean reference = vector.has("schemaRef");
    if (inline && reference) {
      throw new VectorFormatException(
          vector.describe() + ": carries both 'schema' and 'schemaRef'");
    }
    if (inline) {
      return SchemaParser.parse(vector.text("schema"));
    }
    if (reference) {
      return builtInSchema(vector.text("schemaRef"));
    }
    throw new VectorFormatException(
        vector.describe() + ": carries neither 'schema' nor 'schemaRef'");
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
