package in.yagnyam.persona.schema;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread-safe, in-memory {@link SchemaRegistry}.
 *
 * <p>The constructor registers the four built-in envelope schemas of {@code docs/SCHEMA.md} §5.
 *
 * <p>Registration rules (§6.1: schema identity is the digest of the canonical form):
 *
 * <ul>
 *   <li>registering a schema whose digest is already known returns the registered instance and is a
 *       no-op — the canonical form is identical by construction of the digest;
 *   <li>registering a schema whose messageType is already bound to a different canonical form is
 *       rejected; schema evolution requires a new messageType version, never a mutation.
 * </ul>
 */
public final class InMemorySchemaRegistry implements SchemaRegistry {

  private final ConcurrentMap<SchemaId, PersonaSchema> schemasById = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, PersonaSchema> schemasByMessageType =
      new ConcurrentHashMap<>();

  /** Creates a registry with the built-in schemas of §5 registered. */
  public InMemorySchemaRegistry() {
    PersonaSchemas.bootstrap(this);
  }

  @Override
  public synchronized PersonaSchema register(PersonaSchema schema) {
    Objects.requireNonNull(schema, "schema");
    SchemaId id = idOf(schema);
    String canonical = SchemaCanonicalizer.canonicalForm(schema);
    Optional<String> messageType = schema.messageType();
    if (messageType.isPresent()) {
      PersonaSchema bound = schemasByMessageType.get(messageType.get());
      if (bound != null && !SchemaCanonicalizer.canonicalForm(bound).equals(canonical)) {
        throw new IllegalArgumentException(
            "messageType '"
                + messageType.get()
                + "' is already registered with schema "
                + idOf(bound)
                + "; schema evolution requires a new messageType version, the registered schema"
                + " cannot be replaced");
      }
    }
    PersonaSchema existing = schemasById.get(id);
    if (existing != null) {
      if (!SchemaCanonicalizer.canonicalForm(existing).equals(canonical)) {
        throw new IllegalArgumentException(
            "schemaId "
                + id
                + " is already registered with a different canonical form (SHA-1 collision)");
      }
      messageType.ifPresent(value -> schemasByMessageType.putIfAbsent(value, existing));
      return existing;
    }
    schemasById.put(id, schema);
    messageType.ifPresent(value -> schemasByMessageType.putIfAbsent(value, schema));
    return schema;
  }

  @Override
  public PersonaSchema register(String schemaJson) {
    return register(SchemaParser.parse(schemaJson));
  }

  @Override
  public Optional<PersonaSchema> byId(SchemaId id) {
    return Optional.ofNullable(schemasById.get(Objects.requireNonNull(id, "id")));
  }

  @Override
  public Optional<PersonaSchema> byMessageType(String messageType) {
    return Optional.ofNullable(
        schemasByMessageType.get(Objects.requireNonNull(messageType, "messageType")));
  }

  @Override
  public PersonaSchema requireById(SchemaId id) {
    return byId(id)
        .orElseThrow(
            () -> new IllegalArgumentException("no schema registered with schemaId " + id.value()));
  }

  @Override
  public PersonaSchema requireByMessageType(String messageType) {
    return byMessageType(messageType)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "no schema registered with messageType '" + messageType + "'"));
  }

  @Override
  public SchemaId idOf(PersonaSchema schema) {
    return SchemaCanonicalizer.digestOf(schema);
  }
}
