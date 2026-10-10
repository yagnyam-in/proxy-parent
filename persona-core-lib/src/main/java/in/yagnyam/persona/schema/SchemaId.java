package in.yagnyam.persona.schema;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The identity of a schema: the SHA-1 digest of its canonical form
 * ({@code docs/SCHEMA.md} §2), rendered as 40 lowercase hex characters.
 *
 * <p>A digest is computed from the parsed model, never from the source text, so whitespace and key
 * order in the schema document do not affect it.
 *
 * @param value 40 lowercase hex characters, or the empty string for {@link #NONE}
 */
public record SchemaId(String value) {

  private static final Pattern HEX40 = Pattern.compile("[0-9a-f]{40}");

  /**
   * The schema id used when a payload carries no schema id at all (e.g. an envelope whose
   * {@code schemaId} field is empty). Its {@link #value()} is the empty string.
   *
   * <p>Note that {@link #of(String)} deliberately rejects the empty string: only this constant
   * denotes "absent".
   */
  public static final SchemaId NONE = new SchemaId("");

  /**
   * Validates the digest text.
   *
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if {@code value} is neither empty nor 40 lowercase hex
   *     characters
   */
  public SchemaId {
    Objects.requireNonNull(value, "value");
    if (!value.isEmpty() && !HEX40.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "schemaId must be 40 lowercase hex characters (SHA-1) or empty, got '" + value + "'");
    }
  }

  /**
   * Creates a schema id from its hex form.
   *
   * @param value 40 lowercase hex characters
   * @return the schema id
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if {@code value} is not 40 lowercase hex characters
   */
  public static SchemaId of(String value) {
    Objects.requireNonNull(value, "value");
    if (!HEX40.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "schemaId must be 40 lowercase hex characters (SHA-1), got '" + value + "'");
    }
    return new SchemaId(value);
  }

  /**
   * Computes the schema id of a schema (SCHEMA.md §2).
   *
   * @param schema the schema
   * @return {@code SchemaId.of(SchemaCanonicalizer.digestOf(schema))}
   */
  public static SchemaId of(PersonaSchema schema) {
    return SchemaCanonicalizer.digestOf(schema);
  }

  /**
   * Tests whether this id is {@link #NONE}.
   *
   * @return {@code true} if this id is the empty "absent" sentinel
   */
  public boolean isEmpty() {
    return value.isEmpty();
  }
}
