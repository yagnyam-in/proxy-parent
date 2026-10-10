package in.yagnyam.persona.schema;

import java.util.Objects;

/**
 * A single field of a {@link RecordType}, canonicalizing to {@code {"name":...,"type":...}}
 * ({@code docs/SCHEMA.md} §2.2).
 *
 * <p>The identifier rules of §1.3 rule 1 are enforced by {@link SchemaParser}, the only supported
 * way to build a schema.
 *
 * @param name the field name; MUST match {@code ^[A-Za-z_][A-Za-z0-9_]*$} (§1.3 rule 1)
 * @param type the field's type node
 */
public record Field(String name, TypeNode type) {

  /**
   * Validates the field.
   *
   * @throws NullPointerException if {@code name} or {@code type} is {@code null}
   */
  public Field {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
  }
}
