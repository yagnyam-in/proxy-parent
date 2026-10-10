package in.yagnyam.persona.examples.p2p;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.envelope.MessageBinding;
import in.yagnyam.persona.message.AddressableMessage;
import in.yagnyam.persona.message.RequestMessage;
import in.yagnyam.persona.message.SignableMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The demo's domain message {@code example.Greeting@v1} (protocol Appendix
 * A.1): schema document and strict {@link MessageBinding}.
 */
final class GreetingSchema {

  static final String MESSAGE_TYPE = "example.Greeting@v1";

  /** The greeting schema of protocol Appendix A.1 (with {@code issuedAt}). */
  static final String SCHEMA = """
      {"type":"record","name":"Greeting","namespace":"example","messageType":"example.Greeting@v1",
       "fields":[{"name":"greetingId","type":"string"},
                 {"name":"from","type":"string"},
                 {"name":"to","type":"string"},
                 {"name":"body","type":"string"},
                 {"name":"issuedAt","type":"timestamp-millis"}]}""";

  private GreetingSchema() {}

  /**
   * The greeting payload of protocol Appendix A.1: {@code signer()} is
   * {@code from}; {@code isValid()} requires non-empty ids and body, valid
   * persona ids, and {@code issuedAt} within a day of clock skew.
   */
  record Greeting(
      String greetingId, PersonaId from, PersonaId to, String body, Instant issuedAt)
      implements SignableMessage, AddressableMessage, RequestMessage {

    private static final Duration CLOCK_SKEW = Duration.ofDays(1);

    @Override
    public String messageType() {
      return MESSAGE_TYPE;
    }

    @Override
    public PersonaId signer() {
      return from;
    }

    @Override
    public PersonaId address() {
      return to;
    }

    @Override
    public String requestId() {
      return greetingId;
    }

    @Override
    public boolean isValid() {
      Instant now = Instant.now();
      return greetingId != null && !greetingId.isEmpty()
          && from != null && from.isValid()
          && to != null && to.isValid()
          && body != null && !body.isEmpty()
          && issuedAt != null
          && !issuedAt.isBefore(now.minus(CLOCK_SKEW))
          && !issuedAt.isAfter(now.plus(CLOCK_SKEW));
    }

    @Override
    public String toString() {
      return "Greeting[greetingId=" + greetingId
          + ", from=" + from
          + ", to=" + to
          + ", body=" + body
          + ", issuedAt=" + issuedAt + "]";
    }
  }

  /**
   * The strict binding for {@code example.Greeting@v1} (fail closed, as
   * {@link MessageBinding} requires).
   *
   * @return the binding
   */
  static MessageBinding binding() {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return MESSAGE_TYPE;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        return new Greeting(
            text(payloadValue, "greetingId"),
            PersonaId.of(text(payloadValue, "from")),
            PersonaId.of(text(payloadValue, "to")),
            text(payloadValue, "body"),
            instant(payloadValue, "issuedAt"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        if (!(message instanceof Greeting greeting)) {
          throw new IllegalArgumentException("expected a Greeting for messageType " + MESSAGE_TYPE
              + " but found "
              + (message == null ? "null" : message.getClass().getName()));
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("greetingId", greeting.greetingId());
        value.put("from", greeting.from().uniqueId());
        value.put("to", greeting.to().uniqueId());
        value.put("body", greeting.body());
        value.put("issuedAt", greeting.issuedAt());
        return value;
      }
    };
  }

  private static String text(Map<String, Object> value, String field) {
    if (!(value.get(field) instanceof String text)) {
      throw new IllegalArgumentException("field '" + field + "' must be a String");
    }
    return text;
  }

  private static Instant instant(Map<String, Object> value, String field) {
    if (!(value.get(field) instanceof Instant instant)) {
      throw new IllegalArgumentException("field '" + field + "' must be an Instant");
    }
    return instant;
  }

}
