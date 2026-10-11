package in.yagnyam.persona.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Java reference runner for the Persona Protocol conformance vectors: it
 * discovers every vector file under {@code vectors/}, refuses anything it
 * cannot interpret, executes every vector against {@code persona-core-lib} and
 * asserts a minimum count per kind, so the suite cannot pass by accident.
 *
 * <p>The contract this runner implements — discovery, the {@code kind}
 * dispatch, the per-kind key sets, and the encodings — is documented in
 * {@code vectors/README.md} (Persona Conformance Vectors v1), so an
 * implementation in any language can consume the same vectors and pass. The
 * independent Python verifier {@code tools/reference_vectors.py} re-derives the
 * golden values from the specifications alone and fails on any disagreement.
 */
class ConformanceVectorSuite {

  /** The machine-readable marker of the vector format. */
  private static final String VECTOR_FORMAT = "persona-conformance-vectors";

  /** The version of the vector format this runner implements. */
  private static final int VECTOR_FORMAT_VERSION = 1;

  /** The file index, cross-checked against discovery. */
  private static final String MANIFEST = "manifest.json";

  /**
   * Common keys every vector may carry, before the per-kind keys.
   */
  private static final Set<String> COMMON_KEYS =
      Set.of("id", "spec", "description", "expect", "rejectionRule", "rejectionMessageFragment");

  /**
   * The keys each kind may carry (common keys plus the kind's inputs and
   * expected outputs); an entry carrying anything else fails the suite instead
   * of being silently ignored.
   */
  private static final Map<String, Set<String>> KEYS_BY_KIND = Map.ofEntries(
      Map.entry(VectorKinds.SCHEMA_DIGEST, Set.of(
          "schema", "schemaRef", "generatedSchema", "expectedCanonicalForm", "expectedSchemaId")),
      Map.entry(VectorKinds.BINARY_ENCODE, Set.of(
          "schema", "schemaRef", "generatedSchema", "value", "expectedHex")),
      Map.entry(VectorKinds.BINARY_DECODE, Set.of(
          "schema", "schemaRef", "generatedSchema", "inputHex", "expectedValue")),
      Map.entry(VectorKinds.JSON_ENCODE, Set.of(
          "schema", "schemaRef", "generatedSchema", "value", "expectedJson")),
      Map.entry(VectorKinds.JSON_DECODE, Set.of(
          "schema", "generatedSchema", "inputJson", "expectedValue")),
      Map.entry(VectorKinds.PERSONA_ID, Set.of(
          "input", "expectedId", "expectedThumbprint", "expectedCanonical", "expectedWildcard")),
      Map.entry(VectorKinds.PERSONA_ID_DELEGATION, Set.of("signer", "signedFor", "expected")),
      Map.entry(VectorKinds.REALM, Set.of("input", "expected")),
      Map.entry(VectorKinds.PEM, Set.of("objectType", "pem", "expected")),
      Map.entry(VectorKinds.POLICY_V0, Set.of("policy", "field", "expected")),
      Map.entry(VectorKinds.ALGORITHM_SET, Set.of("signatureAlgorithms", "expected")),
      Map.entry(VectorKinds.POLICY_V1, Set.of("policy", "field", "expected")),
      Map.entry(VectorKinds.ALGORITHM_SET_V1, Set.of("signatureAlgorithms", "expected")),
      Map.entry(VectorKinds.SIGNING_PREIMAGE, Set.of(
          "domain", "messageType", "payloadFormat", "schemaId", "payloadHex", "expectedHex")),
      Map.entry(VectorKinds.SIGNATURE_VERIFY, Set.of(
          "publicKeyPem", "algorithm", "preimageHex", "signatureHex", "expected")));

  /**
   * The keys a negative vector must carry beyond the common keys (its inputs);
   * it must not carry any expected output, or the suite fails. The schema of a
   * negative vector may be inline ({@code schema}) or a built-in
   * ({@code schemaRef}), exactly one of them, like an accepting vector.
   */
  private static final Map<String, Set<String>> REJECT_INPUT_KEYS = Map.ofEntries(
      Map.entry(VectorKinds.SCHEMA_DIGEST, Set.of("schema")),
      Map.entry(VectorKinds.BINARY_ENCODE, Set.of("value")),
      Map.entry(VectorKinds.BINARY_DECODE, Set.of("inputHex")),
      Map.entry(VectorKinds.JSON_ENCODE, Set.of("value")),
      Map.entry(VectorKinds.JSON_DECODE, Set.of("inputJson")),
      Map.entry(VectorKinds.PERSONA_ID, Set.of("input")),
      Map.entry(VectorKinds.PEM, Set.of("objectType", "pem")),
      Map.entry(VectorKinds.SIGNATURE_VERIFY, Set.of(
          "publicKeyPem", "algorithm", "preimageHex", "signatureHex")));

  /**
   * The keys an accepting vector must carry beyond the common keys.
   */
  private static final Map<String, Set<String>> ACCEPT_REQUIRED_KEYS = Map.ofEntries(
      Map.entry(VectorKinds.SCHEMA_DIGEST, Set.of("expectedCanonicalForm", "expectedSchemaId")),
      Map.entry(VectorKinds.BINARY_ENCODE, Set.of("value", "expectedHex")),
      Map.entry(VectorKinds.BINARY_DECODE, Set.of("inputHex", "expectedValue")),
      Map.entry(VectorKinds.JSON_ENCODE, Set.of("value", "expectedJson")),
      Map.entry(VectorKinds.JSON_DECODE, Set.of("inputJson", "expectedValue")),
      Map.entry(VectorKinds.PERSONA_ID, Set.of(
          "input", "expectedId", "expectedThumbprint", "expectedCanonical", "expectedWildcard")),
      Map.entry(VectorKinds.PERSONA_ID_DELEGATION, Set.of("signer", "signedFor", "expected")),
      Map.entry(VectorKinds.REALM, Set.of("input", "expected")),
      Map.entry(VectorKinds.PEM, Set.of("objectType", "pem", "expected")),
      Map.entry(VectorKinds.POLICY_V0, Set.of("policy", "field", "expected")),
      Map.entry(VectorKinds.ALGORITHM_SET, Set.of("signatureAlgorithms", "expected")),
      Map.entry(VectorKinds.POLICY_V1, Set.of("policy", "field", "expected")),
      Map.entry(VectorKinds.ALGORITHM_SET_V1, Set.of("signatureAlgorithms", "expected")),
      Map.entry(VectorKinds.SIGNING_PREIMAGE, Set.of(
          "domain", "messageType", "payloadFormat", "payloadHex", "expectedHex")),
      Map.entry(VectorKinds.SIGNATURE_VERIFY, Set.of(
          "publicKeyPem", "algorithm", "preimageHex", "signatureHex", "expected")));

  /**
   * The minimum number of vectors per kind. Deliberately shrinking a phase is
   * a spec-visible change; if you removed vectors on purpose, lower the floor
   * here and regenerate {@code manifest.json} in the same commit.
   */
  private static final Map<String, Integer> MINIMUM_VECTORS_PER_KIND = Map.ofEntries(
      Map.entry(VectorKinds.SCHEMA_DIGEST, 51),
      Map.entry(VectorKinds.BINARY_ENCODE, 80),
      Map.entry(VectorKinds.BINARY_DECODE, 62),
      Map.entry(VectorKinds.JSON_ENCODE, 59),
      Map.entry(VectorKinds.JSON_DECODE, 48),
      Map.entry(VectorKinds.PERSONA_ID, 20),
      Map.entry(VectorKinds.PERSONA_ID_DELEGATION, 11),
      Map.entry(VectorKinds.REALM, 20),
      Map.entry(VectorKinds.PEM, 14),
      Map.entry(VectorKinds.POLICY_V0, 11),
      Map.entry(VectorKinds.ALGORITHM_SET, 9),
      Map.entry(VectorKinds.POLICY_V1, 12),
      Map.entry(VectorKinds.ALGORITHM_SET_V1, 10),
      Map.entry(VectorKinds.SIGNING_PREIMAGE, 20),
      Map.entry(VectorKinds.SIGNATURE_VERIFY, 14));

  /**
   * Kinds whose contract admits no negative form. For the verdict kinds a
   * negative vector is a category error (the verdict "false" already is the
   * negative); for {@code envelope.signing-preimage} §6.4 mandates no
   * rejection — the preimage builder takes the domain as given, so every
   * well-formed input has a defined preimage (see vectors/README.md).
   */
  private static final Set<String> VERDICT_ONLY_KINDS = Set.of(
      VectorKinds.PERSONA_ID_DELEGATION,
      VectorKinds.REALM,
      VectorKinds.POLICY_V0,
      VectorKinds.ALGORITHM_SET,
      VectorKinds.POLICY_V1,
      VectorKinds.ALGORITHM_SET_V1,
      VectorKinds.SIGNING_PREIMAGE);

  /**
   * Duplicate-key detection keeps vector files honest while parsing. The
   * nesting constraint is raised because the §3.4 rule 2 vectors nest values
   * ~1000 levels deep *inside* a vector document, deeper than Jackson's
   * default document limit of 1000.
   */
  private static final ObjectMapper MAPPER =
      new ObjectMapper(
              com.fasterxml.jackson.core.JsonFactory.builder()
                  .streamReadConstraints(
                      com.fasterxml.jackson.core.StreamReadConstraints.builder()
                          .maxNestingDepth(100_000)
                          .build())
                  .build())
          .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

  /** One discovered vector file: its path, kind and parsed entries. */
  private record VectorFile(Path path, String relativePath, String kind, List<ConformanceVector> vectors) {}

  /** The discovered files, in deterministic order. */
  private static List<VectorFile> files = List.of();

  @BeforeAll
  static void discoverVectorFiles() {
    files = discover();
  }

  // -----------------------------------------------------------------------
  // Integrity: the suite cannot pass if the vectors themselves are broken
  // -----------------------------------------------------------------------

  @Test
  void everyVectorFileIsWellFormed() {
    List<String> problems = new ArrayList<>();
    Set<String> seenIds = new HashSet<>();
    for (VectorFile file : files) {
      for (ConformanceVector vector : file.vectors()) {
        if (!seenIds.add(vector.id())) {
          problems.add("duplicate vector id '" + vector.id() + "'");
        }
        validate(vector, problems);
      }
    }
    assertThat(problems)
        .as("vector files must be well formed (%d files, %d vectors)", files.size(),
            files.stream().mapToInt(file -> file.vectors().size()).sum())
        .isEmpty();
  }

  @Test
  void manifestMatchesTheDiscoveredFiles() throws IOException {
    Path manifestPath = resolveVectorsDirectory().resolve(MANIFEST);
    assertThat(Files.exists(manifestPath))
        .as("%s must exist: regenerate it with 'python3 tools/reference_vectors.py manifest'", MANIFEST)
        .isTrue();
    JsonNode manifest = MAPPER.readTree(Files.newBufferedReader(manifestPath));
    List<String> problems = new ArrayList<>();
    checkFormatMarker(manifest, MANIFEST, problems);
    Map<String, JsonNode> listed = new LinkedHashMap<>();
    for (JsonNode entry : manifest.get("files")) {
      listed.put(entry.get("path").asText(), entry);
    }
    Set<String> discoveredPaths = new HashSet<>();
    for (VectorFile file : files) {
      discoveredPaths.add(file.relativePath());
      JsonNode entry = listed.get(file.relativePath());
      if (entry == null) {
        problems.add(MANIFEST + " does not list " + file.relativePath());
        continue;
      }
      if (!entry.get("kind").asText().equals(file.kind())) {
        problems.add(MANIFEST + " says " + file.relativePath() + " is kind "
            + entry.get("kind").asText() + " but the file says " + file.kind());
      }
      if (entry.get("vectorCount").asInt() != file.vectors().size()) {
        problems.add(MANIFEST + " says " + file.relativePath() + " holds "
            + entry.get("vectorCount").asInt() + " vectors but it holds " + file.vectors().size());
      }
    }
    for (String listedPath : listed.keySet()) {
      if (!discoveredPaths.contains(listedPath)) {
        problems.add(MANIFEST + " lists " + listedPath + " which was not discovered");
      }
    }
    int discoveredTotal = files.stream().mapToInt(file -> file.vectors().size()).sum();
    if (manifest.get("vectorCount").asInt() != discoveredTotal) {
      problems.add(MANIFEST + " says vectorCount " + manifest.get("vectorCount").asInt()
          + " but " + discoveredTotal + " vectors were discovered");
    }
    Map<String, Integer> discoveredByKind = new LinkedHashMap<>();
    files.forEach(file -> file.vectors()
        .forEach(vector -> discoveredByKind.merge(vector.kind(), 1, Integer::sum)));
    Map<String, Integer> listedByKind = new LinkedHashMap<>();
    JsonNode kinds = manifest.get("kinds");
    if (kinds == null || !kinds.isArray()) {
      problems.add(MANIFEST + " must carry a 'kinds' array of per-kind vector counts");
    } else {
      for (JsonNode entry : kinds) {
        listedByKind.put(entry.get("kind").asText(), entry.get("vectorCount").asInt());
      }
      if (!listedByKind.equals(discoveredByKind)) {
        problems.add(MANIFEST + " kinds are " + listedByKind
            + " but discovery counts " + discoveredByKind);
      }
    }
    assertThat(problems)
        .as("%s must be regenerated after vector files change", MANIFEST)
        .isEmpty();
  }

  @Test
  void everyKindReachesItsMinimumVectorCount() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    files.forEach(file -> counts.merge(file.kind(), file.vectors().size(), Integer::sum));
    int total = counts.values().stream().mapToInt(Integer::intValue).sum();
    StringBuilder report = new StringBuilder("executed vectors per kind:\n");
    counts.keySet().stream().sorted()
        .forEach(kind -> report.append("  ").append(kind).append(": ").append(counts.get(kind)).append('\n'));
    report.append("  total: ").append(total).append(" across ").append(files.size()).append(" files");
    System.out.println(report);

    List<String> shrunk = new ArrayList<>();
    for (Map.Entry<String, Integer> minimum : MINIMUM_VECTORS_PER_KIND.entrySet()) {
      int actual = counts.getOrDefault(minimum.getKey(), 0);
      if (actual < minimum.getValue()) {
        shrunk.add(minimum.getKey() + " has " + actual + " vectors, minimum is " + minimum.getValue());
      }
    }
    List<String> unknownKinds = new ArrayList<>();
    for (String kind : counts.keySet()) {
      if (!MINIMUM_VECTORS_PER_KIND.containsKey(kind)) {
        unknownKinds.add(kind);
      }
    }
    assertThat(unknownKinds)
        .as("every discovered kind must have a recorded minimum count")
        .isEmpty();
    assertThat(shrunk)
        .as("the suite shrank: %s", report)
        .isEmpty();
  }

  // -----------------------------------------------------------------------
  // Execution: one dynamic test per vector
  // -----------------------------------------------------------------------

  @TestFactory
  Stream<DynamicContainer> executeEveryVector() {
    return files.stream()
        .map(file -> DynamicContainer.dynamicContainer(
            file.relativePath(),
            file.vectors().stream().map(vector -> DynamicTest.dynamicTest(
                vector.id(),
                () -> VectorKinds.execute(vector)))));
  }

  // -----------------------------------------------------------------------
  // Discovery
  // -----------------------------------------------------------------------

  /** Discovers and validates every vector file under the vectors directory. */
  private static List<VectorFile> discover() {
    Path directory = resolveVectorsDirectory();
    List<Path> paths = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(directory)) {
      stream.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".json"))
          .filter(path -> !(path.getParent().equals(directory)
              && path.getFileName().toString().equals(MANIFEST)))
          .forEach(paths::add);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot walk " + directory, e);
    }
    paths.sort(Path::compareTo);
    if (paths.isEmpty()) {
      throw new VectorFormatException("no vector files found under " + directory);
    }
    List<String> problems = new ArrayList<>();
    List<VectorFile> discovered = new ArrayList<>(paths.size());
    for (Path path : paths) {
      String relativePath = directory.relativize(path).toString().replace('\\', '/');
      JsonNode document;
      try {
        document = MAPPER.readTree(Files.newBufferedReader(path));
      } catch (IOException e) {
        throw new VectorFormatException("cannot parse " + relativePath + ": " + e.getMessage(), e);
      }
      checkFormatMarker(document, relativePath, problems);
      JsonNode kindNode = document.get("kind");
      if (kindNode == null || !kindNode.isTextual()) {
        problems.add(relativePath + ": missing or non-textual 'kind'");
        continue;
      }
      String kind = kindNode.textValue();
      if (!VectorKinds.knownKinds().contains(kind)) {
        problems.add(relativePath + ": unknown vector kind '" + kind
            + "'; teach the runner the kind or fix the file");
        continue;
      }
      JsonNode vectorsNode = document.get("vectors");
      if (vectorsNode == null || !vectorsNode.isArray()) {
        problems.add(relativePath + ": 'vectors' must be an array");
        continue;
      }
      List<ConformanceVector> vectors = new ArrayList<>(vectorsNode.size());
      for (JsonNode entry : vectorsNode) {
        vectors.add(new ConformanceVector(relativePath, kind, entry));
      }
      if (vectors.isEmpty()) {
        problems.add(relativePath + ": holds no vectors");
        continue;
      }
      discovered.add(new VectorFile(path, relativePath, kind, vectors));
    }
    assertThat(problems)
        .as("vector files must be well formed (format marker, kind, non-empty)")
        .isEmpty();
    return List.copyOf(discovered);
  }

  /** Checks the machine-readable format marker of a vector file or the manifest. */
  private static void checkFormatMarker(JsonNode document, String what, List<String> problems) {
    JsonNode format = document.get("vectorFormat");
    if (format == null || !format.isTextual()
        || !VECTOR_FORMAT.equals(format.textValue())) {
      problems.add(what + ": 'vectorFormat' must be \"" + VECTOR_FORMAT + "\"");
      return;
    }
    JsonNode version = document.get("vectorFormatVersion");
    if (version == null || !version.isInt() || version.intValue() != VECTOR_FORMAT_VERSION) {
      problems.add(what + ": 'vectorFormatVersion' must be " + VECTOR_FORMAT_VERSION);
    }
  }

  /** Validates one vector entry's key set against its kind's contract. */
  private static void validate(ConformanceVector vector, List<String> problems) {
    String what = vector.describe();
    if (!vector.has("id")) {
      problems.add(what + ": missing 'id'");
    }
    if (!vector.has("spec")) {
      problems.add(what + ": missing 'spec'");
    }
    if (!vector.has("description")) {
      problems.add(what + ": missing 'description'");
    }
    Set<String> allowed = new HashSet<>(COMMON_KEYS);
    allowed.addAll(KEYS_BY_KIND.getOrDefault(vector.kind(), Set.of()));
    List<String> unknown = new ArrayList<>();
    vector.document().fieldNames().forEachRemaining(name -> {
      if (!allowed.contains(name)) {
        unknown.add(name);
      }
    });
    if (!unknown.isEmpty()) {
      problems.add(what + ": unknown keys " + unknown + " for kind " + vector.kind());
    }
    boolean rejects = false;
    try {
      rejects = vector.expectsRejection();
    } catch (VectorFormatException e) {
      problems.add(e.getMessage());
      return;
    }
    if (rejects) {
      if (!vector.has("rejectionRule")) {
        problems.add(what + ": a negative vector must cite 'rejectionRule'");
      }
      for (String key : REJECT_INPUT_KEYS.getOrDefault(vector.kind(), Set.of())) {
        if (!vector.has(key)) {
          problems.add(what + ": a negative vector must carry '" + key + "'");
        }
      }
      for (String key : List.of("expectedCanonicalForm", "expectedSchemaId", "expectedHex",
          "expectedJson", "expectedValue", "expectedId", "expectedThumbprint",
          "expectedCanonical", "expectedWildcard", "expected")) {
        if (vector.has(key)) {
          problems.add(what + ": a negative vector must not carry an expected output ('" + key + "')");
        }
      }
      if (VERDICT_ONLY_KINDS.contains(vector.kind())) {
        problems.add(what + ": kind " + vector.kind() + " has no negative form; fix the vector");
      }
    } else {
      for (String key : ACCEPT_REQUIRED_KEYS.getOrDefault(vector.kind(), Set.of())) {
        if (!vector.has(key)) {
          problems.add(what + ": missing '" + key + "'");
        }
      }
    }
    boolean schemaDriven = VectorKinds.SCHEMA_DIGEST.equals(vector.kind())
        || VectorKinds.BINARY_ENCODE.equals(vector.kind())
        || VectorKinds.BINARY_DECODE.equals(vector.kind())
        || VectorKinds.JSON_ENCODE.equals(vector.kind())
        || VectorKinds.JSON_DECODE.equals(vector.kind());
    int schemaForms = (vector.has("schema") ? 1 : 0)
        + (vector.has("schemaRef") ? 1 : 0)
        + (vector.has("generatedSchema") ? 1 : 0);
    if (schemaDriven && schemaForms != 1) {
      problems.add(what + ": carry exactly one of 'schema', 'schemaRef' and 'generatedSchema'");
    }
  }

  /**
   * Resolves the vectors directory: the {@code conformance.vectors.dir} system
   * property (set by surefire), then a few well-known relative locations, so
   * the suite runs from Maven, from an IDE and from the repository root alike.
   */
  static Path resolveVectorsDirectory() {
    List<String> candidates = new ArrayList<>();
    String configured = System.getProperty("conformance.vectors.dir");
    if (configured != null && !configured.isBlank()) {
      candidates.add(configured);
    }
    candidates.add("vectors");
    candidates.add("persona-conformance/vectors");
    candidates.add("../persona-conformance/vectors");
    candidates.add("src/test/resources/vectors");
    List<String> attempted = new ArrayList<>();
    for (String candidate : candidates) {
      Path path = Path.of(candidate).toAbsolutePath().normalize();
      attempted.add(path.toString());
      if (Files.isDirectory(path)) {
        return path;
      }
    }
    throw new VectorFormatException(
        "cannot find the conformance vectors directory (tried " + attempted
            + "; conformance.vectors.dir=" + configured + ")");
  }
}
