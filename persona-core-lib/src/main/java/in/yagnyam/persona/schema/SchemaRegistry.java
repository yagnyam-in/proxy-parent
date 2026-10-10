package in.yagnyam.persona.schema;

import java.util.Optional;

/**
 * A registry of schemas keyed by {@link SchemaId} and by message type ({@code docs/SCHEMA.md}
 * §6.4: verifiers resolve the schema of a payload through the registry).
 *
 * <p>A registry is created with the built-in envelope schemas of §5 already registered; see
 * {@link PersonaSchemas#bootstrap(SchemaRegistry)}.
 *
 * <p>Schema evolution is expressed as a new messageType version, never as a mutation of an
 * existing registration: registering a schema whose messageType is already bound to a different
 * canonical form is rejected.
 */
public interface SchemaRegistry {

  /**
   * Registers a schema. Registration is idempotent: registering a schema with the same canonical
   * form again succeeds and leaves the registry unchanged.
   *
   * @param schema the schema
   * @return the registered schema instance (the previously registered one when it was already
   *     present)
   * @throws NullPointerException if {@code schema} is {@code null}
   * @throws IllegalArgumentException if the schema's messageType is already registered with a
   *     different canonical form
   */
  PersonaSchema register(PersonaSchema schema);

  /**
   * Parses and registers a schema document.
   *
   * @param schemaJson the schema JSON text
   * @return the registered schema
   * @throws SchemaValidationException if the document is invalid (§1.3)
   * @throws IllegalArgumentException if the schema's messageType is already registered with a
   *     different canonical form
   */
  PersonaSchema register(String schemaJson);

  /**
   * Looks up a schema by its digest.
   *
   * @param id the schema id
   * @return the schema, or {@link Optional#empty()} if it is not registered
   */
  Optional<PersonaSchema> byId(SchemaId id);

  /**
   * Looks up a schema by its message type.
   *
   * @param messageType the message type (e.g. {@code example.Greeting@v1})
   * @return the schema, or {@link Optional#empty()} if no schema declares that message type
   */
  Optional<PersonaSchema> byMessageType(String messageType);

  /**
   * Looks up a schema by its digest, failing when it is absent.
   *
   * @param id the schema id
   * @return the schema
   * @throws IllegalArgumentException if no schema with that id is registered
   */
  PersonaSchema requireById(SchemaId id);

  /**
   * Looks up a schema by its message type, failing when it is absent.
   *
   * @param messageType the message type
   * @return the schema
   * @throws IllegalArgumentException if no schema with that message type is registered
   */
  PersonaSchema requireByMessageType(String messageType);

  /**
   * Computes the digest of a schema. This is a pure function that does not touch the registry
   * (SCHEMA.md §2).
   *
   * @param schema the schema
   * @return the schema id
   */
  SchemaId idOf(PersonaSchema schema);
}
