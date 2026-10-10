package in.yagnyam.persona.schema;

/**
 * A node of the Persona schema type tree.
 *
 * <p>Implements the node kinds of {@code docs/SCHEMA.md} §1.1: {@code record}, {@code enum},
 * {@code array}, {@code map}, {@code envelope}, {@code multi-envelope} and the primitives of §1.2.
 * Every implementation is an immutable record, so a parsed schema can be shared freely between
 * threads.
 *
 * <p>This interface is sealed: the set of node kinds is fixed by the protocol version and adding a
 * kind is an explicit, reviewable change to the model.
 */
public sealed interface TypeNode
    permits RecordType, EnumType, ArrayType, MapType, EnvelopeType, PrimitiveType {

  /**
   * Human-readable node kind, used in error messages.
   *
   * @param node a type node
   * @return the schema keyword naming the node kind (e.g. {@code "record"}, {@code "envelope"})
   */
  static String kindOf(TypeNode node) {
    return switch (node) {
      case RecordType record -> "record '" + record.name() + "'";
      case EnumType enumType -> "enum '" + enumType.name() + "'";
      case ArrayType ignored -> "array";
      case MapType ignored -> "map";
      case EnvelopeType envelopeType -> envelopeType.multi() ? "multi-envelope" : "envelope";
      case PrimitiveType primitive -> "primitive '" + primitive.name() + "'";
    };
  }
}
