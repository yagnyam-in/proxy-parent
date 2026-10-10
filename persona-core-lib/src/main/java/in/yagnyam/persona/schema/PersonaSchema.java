package in.yagnyam.persona.schema;

import java.util.Objects;
import java.util.Optional;

/**
 * A parsed schema: a type tree whose root is a record ({@code docs/SCHEMA.md} §1).
 *
 * <p>Identity of a schema is the {@link SchemaId} digest of its canonical form (§2), so two
 * schemas parsed from differently formatted JSON documents are equal values.
 *
 * @param root the root type node; MUST be a {@link RecordType}
 */
public record PersonaSchema(TypeNode root) {

  /**
   * Validates the schema.
   *
   * @throws NullPointerException if {@code root} is {@code null}
   * @throws SchemaValidationException if {@code root} is not a record (§1.1: the root MUST be a
   *     record)
   */
  public PersonaSchema {
    Objects.requireNonNull(root, "root");
    if (!(root instanceof RecordType)) {
      throw new SchemaValidationException(
          "schema root must be a record, found " + TypeNode.kindOf(root) + " (SCHEMA.md §1.1)");
    }
  }

  /**
   * The root record.
   *
   * @return the root node, typed as a record
   */
  public RecordType rootRecord() {
    return (RecordType) root;
  }

  /**
   * The root record's message type, if declared (§1.3 rule 7).
   *
   * @return the message type or {@link Optional#empty()}
   */
  public Optional<String> messageType() {
    return rootRecord().messageType();
  }

  /**
   * Looks up a field of the root record by name.
   *
   * @param name the field name
   * @return the field, or {@link Optional#empty()} if the record has no such field
   */
  public Optional<Field> field(String name) {
    Objects.requireNonNull(name, "name");
    return rootRecord().fields().stream().filter(field -> field.name().equals(name)).findFirst();
  }
}
