package in.yagnyam.persona.schema;

import java.util.Objects;

/**
 * A {@code map} node ({@code docs/SCHEMA.md} §1.1). Keys are always strings. Canonical form
 * (§2.2): {@code {"type":"map","values":<node>}}.
 *
 * @param values the type of the map's values
 */
public record MapType(TypeNode values) implements TypeNode {

  /**
   * Validates the node.
   *
   * @throws NullPointerException if {@code values} is {@code null}
   */
  public MapType {
    Objects.requireNonNull(values, "values");
  }
}
