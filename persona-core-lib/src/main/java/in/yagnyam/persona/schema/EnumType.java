package in.yagnyam.persona.schema;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An {@code enum} node ({@code docs/SCHEMA.md} §1.1).
 *
 * <p>Symbol order is semantic: the binary encoding (§3.2) writes the zero-based index of the
 * symbol, so reordering the symbols changes both the schema digest and the wire bytes. Canonical
 * form (§2.2): {@code {"name":"...","namespace":"..."?,"symbols":[...],"type":"enum"}}.
 *
 * <p>The identifier and uniqueness rules of §1.3 rule 4 are enforced by {@link SchemaParser}, the
 * only supported way to build a schema.
 *
 * @param name the enum name; MUST match {@code ^[A-Za-z_][A-Za-z0-9_]*$}
 * @param namespace the optional namespace (§1.3 rule 2)
 * @param symbols the symbols in declaration order; MUST be non-empty and unique (§1.3 rule 4)
 */
public record EnumType(String name, Optional<String> namespace, List<String> symbols)
    implements TypeNode {

  /**
   * Validates the node and takes a defensive copy of {@code symbols}.
   *
   * @throws NullPointerException if any argument or symbol is {@code null}
   */
  public EnumType {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(namespace, "namespace");
    Objects.requireNonNull(symbols, "symbols");
    symbols = List.copyOf(symbols);
  }
}
