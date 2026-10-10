package in.yagnyam.persona.schema;

import java.util.Objects;

/**
 * An {@code array} node ({@code docs/SCHEMA.md} §1.1). Canonical form (§2.2):
 * {@code {"items":<node>,"type":"array"}}.
 *
 * @param items the type of the array's items
 */
public record ArrayType(TypeNode items) implements TypeNode {

  /**
   * Validates the node.
   *
   * @throws NullPointerException if {@code items} is {@code null}
   */
  public ArrayType {
    Objects.requireNonNull(items, "items");
  }
}
