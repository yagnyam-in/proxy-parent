package in.yagnyam.persona.schema;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@code record} node ({@code docs/SCHEMA.md} §1.1).
 *
 * <p>The root of every schema is a record. Canonical form (§2.2):
 * {@code {"fields":[...],"messageType":"..."?,"name":"...","namespace":"..."?,"type":"record"}} —
 * the {@code fields} array keeps the declared order because field order is semantic.
 *
 * <p>The identifier, namespace, messageType and uniqueness rules of §1.3 are enforced by
 * {@link SchemaParser}, the only supported way to build a schema.
 *
 * @param name the record name; MUST match {@code ^[A-Za-z_][A-Za-z0-9_]*$}
 * @param namespace the optional namespace (§1.3 rule 2)
 * @param messageType the optional message type; at most one per document, on the root only
 *     (§1.3 rule 7)
 * @param fields the fields in declaration order; names MUST be unique (§1.3 rule 4)
 */
public record RecordType(
    String name, Optional<String> namespace, Optional<String> messageType, List<Field> fields)
    implements TypeNode {

  /**
   * Validates the node and takes a defensive copy of {@code fields}.
   *
   * @throws NullPointerException if any argument is {@code null}
   */
  public RecordType {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(namespace, "namespace");
    Objects.requireNonNull(messageType, "messageType");
    Objects.requireNonNull(fields, "fields");
    fields = List.copyOf(fields);
  }
}
