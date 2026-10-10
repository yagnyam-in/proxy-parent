/**
 * The Persona schema model: the type tree of {@code docs/SCHEMA.md} §1, its canonical JSON form
 * and SHA-1 digest (§2), the parser that enforces the §1.3 validation rules, and the schema
 * registry used to resolve a payload's schema from its {@code schemaId} or {@code messageType}.
 *
 * <p>Schema identity is the {@code SchemaId} digest of the canonical form, and the model is
 * immutable throughout: a parsed schema can be shared between threads.
 */
package in.yagnyam.persona.schema;
