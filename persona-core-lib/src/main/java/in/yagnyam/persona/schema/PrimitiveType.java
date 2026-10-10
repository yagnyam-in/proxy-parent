package in.yagnyam.persona.schema;

import java.util.Objects;
import java.util.Set;

/**
 * A primitive node ({@code docs/SCHEMA.md} §1.2). Canonical form (§2.2): the bare JSON string
 * naming the primitive, e.g. {@code "string"}.
 *
 * @param name one of {@code string}, {@code boolean}, {@code int}, {@code long}, {@code double},
 *     {@code bytes}, {@code timestamp-millis}
 */
public record PrimitiveType(String name) implements TypeNode {

  /** {@code string} — UTF-8 text of arbitrary length. */
  public static final String STRING = "string";

  /** {@code boolean} — {@code true}/{@code false}. */
  public static final String BOOLEAN = "boolean";

  /** {@code int} — signed 32-bit integer. */
  public static final String INT = "int";

  /** {@code long} — signed 64-bit integer. */
  public static final String LONG = "long";

  /** {@code double} — IEEE 754 binary64. */
  public static final String DOUBLE = "double";

  /** {@code bytes} — arbitrary byte sequence. */
  public static final String BYTES = "bytes";

  /** {@code timestamp-millis} — instant on the UTC timeline, millisecond precision. */
  public static final String TIMESTAMP_MILLIS = "timestamp-millis";

  private static final Set<String> NAMES =
      Set.of(STRING, BOOLEAN, INT, LONG, DOUBLE, BYTES, TIMESTAMP_MILLIS);

  /**
   * Validates the primitive name.
   *
   * @throws NullPointerException if {@code name} is {@code null}
   * @throws SchemaValidationException if {@code name} is not one of the §1.2 primitives
   */
  public PrimitiveType {
    Objects.requireNonNull(name, "name");
    if (!NAMES.contains(name)) {
      throw new SchemaValidationException(
          "unknown primitive type '" + name + "'; allowed: " + NAMES.stream().sorted().toList());
    }
  }

  /**
   * Tests whether a name denotes a primitive type.
   *
   * @param name a candidate type name
   * @return {@code true} if {@code name} is one of the §1.2 primitives
   */
  public static boolean isPrimitive(String name) {
    return NAMES.contains(name);
  }
}
