package in.yagnyam.persona.conformance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Self-tests of the runner harness with synthetic in-memory vectors: a
 * malformed reject vector must FAIL the suite, never satisfy the rejection
 * assertion it wraps. These guard the two sabotage shapes an adversarial
 * review demonstrated against the runner: (1) input extraction that used to
 * happen inside the asserted lambda let a {@link VectorFormatException} pose
 * as a rejection, and (2) a reject vector must fail for the rule it cites, not
 * for any failure.
 */
class VectorKindsHarnessTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static ConformanceVector vector(String kind, String entry) {
    try {
      JsonNode document = MAPPER.readTree(entry);
      return new ConformanceVector("<synthetic>", kind, document);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void malformedPersonaIdRejectVectorFailsInsteadOfPassingAsRejection() {
    // 'input' is a number, not a string: the vector file is malformed, so the
    // harness must raise VectorFormatException — it must NOT count as the
    // mandated rejection of the persona id.
    ConformanceVector malformed = vector(VectorKinds.PERSONA_ID, """
        {
          "id": "identity/persona-id-reject/synthetic-malformed",
          "spec": "PERSONA-PROTOCOL.md §3.2",
          "description": "synthetic: non-string input",
          "expect": "reject",
          "rejectionRule": "PERSONA-PROTOCOL.md §3.2",
          "input": 42
        }
        """);
    assertThatThrownBy(() -> VectorKinds.execute(malformed))
        .isInstanceOf(VectorFormatException.class);
  }

  @Test
  void malformedPemRejectVectorFailsInsteadOfPassingAsRejection() {
    // 'objectType' is unknown: decoding would fail inside the harness, not in
    // the PEM parser, so this must surface as VectorFormatException.
    ConformanceVector malformed = vector(VectorKinds.PEM, """
        {
          "id": "identity/pem-reject/synthetic-malformed",
          "spec": "PERSONA-PROTOCOL.md §10.1",
          "description": "synthetic: unknown objectType",
          "expect": "reject",
          "rejectionRule": "PERSONA-PROTOCOL.md §10.1",
          "objectType": "bogus",
          "pem": "-----BEGIN CERTIFICATE-----\\n-----END CERTIFICATE-----\\n"
        }
        """);
    // The rejection assertion fails because the VectorFormatException (an
    // AssertionError, never an Exception) cannot pose as the mandated
    // rejection: the suite fails, which is the required outcome.
    assertThatThrownBy(() -> VectorKinds.execute(malformed))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("unknown PEM objectType 'bogus'");
  }

  @Test
  void vectorFormatExceptionIsNotAnExceptionSoNoRejectionAssertionCanSwallowIt() {
    assertThat(Exception.class.isAssignableFrom(VectorFormatException.class)).isFalse();
  }

  @Test
  void rejectVectorWithMessageFragmentFailsWhenTheRuleDoesNotFire() {
    // A rejection pinned to the §3.4 rule-1 zero-progress message must NOT be
    // satisfied by a rejection with a different cause: this input is plain
    // truncation (block count 2, no element bytes) over a string array, so the
    // codec fails with "truncated input" and the fragment assertion fails.
    ConformanceVector truncated = vector(VectorKinds.BINARY_DECODE, """
        {
          "id": "binary/decode-reject/synthetic-truncated",
          "spec": "docs/SCHEMA.md §3.4",
          "description": "synthetic: truncated input pinned to the zero-progress rule",
          "expect": "reject",
          "rejectionRule": "docs/SCHEMA.md §3.4 rule 1",
          "rejectionMessageFragment": "element decoded without consuming any input byte",
          "schema": "{\\"type\\":\\"record\\",\\"name\\":\\"Single\\",\\"fields\\":[{\\"name\\":\\"v\\",\\"type\\":{\\"type\\":\\"array\\",\\"items\\":\\"string\\"}}]}",
          "inputHex": "04"
        }
        """);
    assertThatThrownBy(() -> VectorKinds.execute(truncated))
        .isInstanceOf(AssertionError.class)
        .isNotInstanceOf(VectorFormatException.class);
  }

  @Test
  void honestZeroProgressRejectVectorPassesWithItsMessageFragment() {
    ConformanceVector honest = vector(VectorKinds.BINARY_DECODE, """
        {
          "id": "binary/decode-reject/synthetic-zero-progress",
          "spec": "docs/SCHEMA.md §3.4",
          "description": "synthetic: the genuine zero-progress shape",
          "expect": "reject",
          "rejectionRule": "docs/SCHEMA.md §3.4 rule 1",
          "rejectionMessageFragment": "element decoded without consuming any input byte",
          "schema": "{\\"type\\":\\"record\\",\\"name\\":\\"Single\\",\\"fields\\":[{\\"name\\":\\"v\\",\\"type\\":{\\"type\\":\\"array\\",\\"items\\":{\\"type\\":\\"record\\",\\"name\\":\\"Empty\\",\\"fields\\":[]}}}]}",
          "inputHex": "0400"
        }
        """);
    assertThatCode(() -> VectorKinds.execute(honest)).doesNotThrowAnyException();
  }
}
