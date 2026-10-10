package in.yagnyam.persona.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InMemorySchemaRegistry}: built-in bootstrap, id/ messageType lookup, idempotent
 * registration, rejection of conflicting re-registration, and thread safety.
 */
class InMemorySchemaRegistryTest {

  private static final String GREETING_V2_JSON =
      "{\"type\":\"record\",\"name\":\"Greeting\",\"namespace\":\"example\","
          + "\"messageType\":\"example.Greeting@v2\",\"fields\":[{\"name\":\"greetingId\","
          + "\"type\":\"string\"}]}";

  @Test
  void registersTheBuiltInsAtConstruction() {
    SchemaRegistry registry = new InMemorySchemaRegistry();

    assertThat(registry.byMessageType("persona.Signature@v1")).isPresent();
    assertThat(registry.byMessageType("persona.MultiSignature@v1")).isPresent();
    assertThat(registry.byMessageType("persona.SignedEnvelope@v1")).isPresent();
    assertThat(registry.byMessageType("persona.MultiSignedEnvelope@v1")).isPresent();
    assertThat(registry.requireById(SchemaId.of(SchemaFixtures.SIGNED_ENVELOPE_SCHEMA_ID)))
        .isEqualTo(PersonaSchemas.signedEnvelopeSchema());
    assertThat(registry.requireByMessageType("persona.SignedEnvelope@v1").messageType())
        .contains("persona.SignedEnvelope@v1");
  }

  @Test
  void registersAndLooksUpByIdAndMessageType() {
    SchemaRegistry registry = new InMemorySchemaRegistry();
    PersonaSchema greeting = registry.register(SchemaFixtures.GREETING_SCHEMA_JSON);

    assertThat(greeting).isEqualTo(SchemaFixtures.GREETING);
    assertThat(registry.byId(SchemaId.of(SchemaFixtures.GREETING_SCHEMA_ID))).contains(greeting);
    assertThat(registry.byMessageType("example.Greeting@v1")).contains(greeting);
    assertThat(registry.requireById(SchemaId.of(SchemaFixtures.GREETING_SCHEMA_ID)))
        .isEqualTo(greeting);
    assertThat(registry.requireByMessageType("example.Greeting@v1")).isEqualTo(greeting);
    assertThat(registry.idOf(greeting).value()).isEqualTo(SchemaFixtures.GREETING_SCHEMA_ID);
  }

  @Test
  void registrationIsIdempotent() {
    SchemaRegistry registry = new InMemorySchemaRegistry();
    PersonaSchema first = registry.register(SchemaFixtures.GREETING);
    PersonaSchema second = registry.register(SchemaFixtures.GREETING);
    PersonaSchema third = registry.register(SchemaFixtures.GREETING_SCHEMA_JSON);

    assertThat(second).isSameAs(first);
    assertThat(third).isSameAs(first);
  }

  @Test
  void registersSchemasWithoutAMessageTypeByIdOnly() {
    SchemaRegistry registry = new InMemorySchemaRegistry();
    PersonaSchema schema = registry.register(SchemaFixtures.SIMPLE_SCHEMA_JSON);

    assertThat(registry.byId(SchemaId.of(SchemaFixtures.SIMPLE_SCHEMA_ID))).contains(schema);
    assertThat(schema.messageType()).isEmpty();
    assertThat(registry.byMessageType("")).isEmpty();
  }

  @Test
  void registersANewMessageTypeVersionAlongsideTheOldOne() {
    SchemaRegistry registry = new InMemorySchemaRegistry();
    registry.register(SchemaFixtures.GREETING_SCHEMA_JSON);
    registry.register(GREETING_V2_JSON);

    assertThat(registry.byMessageType("example.Greeting@v1")).isPresent();
    assertThat(registry.byMessageType("example.Greeting@v2")).isPresent();
  }

  @Test
  void rejectsMutatingTheCanonicalFormOfARegisteredMessageType() {
    SchemaRegistry registry = new InMemorySchemaRegistry();
    registry.register(GREETING_V2_JSON);

    String mutated =
        "{\"type\":\"record\",\"name\":\"Greeting\",\"namespace\":\"example\","
            + "\"messageType\":\"example.Greeting@v2\",\"fields\":[{\"name\":\"greetingId\","
            + "\"type\":\"long\"}]}";

    assertThatThrownBy(() -> registry.register(mutated))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("schema evolution requires a new messageType version");
    assertThat(registry.byMessageType("example.Greeting@v2"))
        .contains(registry.requireByMessageType("example.Greeting@v2"));
  }

  @Test
  void rejectsAConflictingBuiltIn() {
    SchemaRegistry registry = new InMemorySchemaRegistry();

    assertThatThrownBy(
            () ->
                registry.register(
                    "{\"type\":\"record\",\"name\":\"SignedEnvelope\",\"namespace\":\"persona\","
                        + "\"messageType\":\"persona.SignedEnvelope@v1\",\"fields\":[]}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("persona.SignedEnvelope@v1");
  }

  @Test
  void requireByThrowsOnMiss() {
    SchemaRegistry registry = new InMemorySchemaRegistry();

    assertThat(registry.byId(SchemaId.of(SchemaFixtures.SIMPLE_SCHEMA_ID))).isEmpty();
    assertThat(registry.byMessageType("nope.Nope@v1")).isEmpty();
    assertThatThrownBy(() -> registry.requireById(SchemaId.of(SchemaFixtures.SIMPLE_SCHEMA_ID)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(SchemaFixtures.SIMPLE_SCHEMA_ID);
    assertThatThrownBy(() -> registry.requireByMessageType("nope.Nope@v1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nope.Nope@v1");
    assertThatThrownBy(() -> registry.register((PersonaSchema) null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> registry.byId(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> registry.byMessageType(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void idOfIsAPureFunction() {
    SchemaRegistry one = new InMemorySchemaRegistry();
    SchemaRegistry other = new InMemorySchemaRegistry();

    assertThat(one.idOf(SchemaFixtures.KITCHEN)).isEqualTo(other.idOf(SchemaFixtures.KITCHEN));
    assertThat(one.idOf(SchemaFixtures.KITCHEN))
        .isEqualTo(SchemaCanonicalizer.digestOf(SchemaFixtures.KITCHEN));
  }

  @Test
  void registersConcurrentlyWithoutDuplicates() throws Exception {
    SchemaRegistry registry = new InMemorySchemaRegistry();
    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      List<Callable<PersonaSchema>> tasks = new ArrayList<>();
      for (int i = 0; i < 64; i++) {
        tasks.add(() -> registry.register(SchemaFixtures.GREETING_SCHEMA_JSON));
      }
      List<Future<PersonaSchema>> results = pool.invokeAll(tasks);
      for (Future<PersonaSchema> result : results) {
        assertThat(result.get()).isEqualTo(SchemaFixtures.GREETING);
      }
    } finally {
      pool.shutdownNow();
    }
    assertThat(registry.byMessageType("example.Greeting@v1")).isPresent();
  }
}
