package in.yagnyam.persona.p2p;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.envelope.MessageBinding;
import in.yagnyam.persona.envelope.MessageBindings;
import in.yagnyam.persona.message.AddressableMessage;
import in.yagnyam.persona.message.AlertMessage;
import in.yagnyam.persona.message.MultiSignableMessage;
import in.yagnyam.persona.message.RequestMessage;
import in.yagnyam.persona.message.SignableMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Test payloads of the p2p tests and their strict {@link MessageBinding}s:
 * an addressed request greeting (§5.1 + §5.2 + §5.3 in one), a
 * multi-signable charter (§5.5) and a broadcast alert (§5.4).
 *
 * <p>The schemas live in {@link P2pWorld}.
 */
final class P2pTestMessages {

  private P2pTestMessages() {}

  // -------------------------------------------------------------------------------------
  // example.Greeting@v1: signable + addressable + request (dedup target)
  // -------------------------------------------------------------------------------------

  /** An addressed greeting: signed by {@code from}, routed to {@code to}. */
  record TestGreeting(String greetingId, PersonaId from, PersonaId to, String body)
      implements SignableMessage, AddressableMessage, RequestMessage {

    static final String MESSAGE_TYPE = "example.Greeting@v1";

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
      return greetingId != null && !greetingId.isEmpty()
          && from != null && from.isValid()
          && to != null && to.isValid()
          && body != null && !body.isEmpty();
    }
  }

  // -------------------------------------------------------------------------------------
  // example.Charter@v1: the multi-signable quorum payload (§A.3)
  // -------------------------------------------------------------------------------------

  /** The charter payload of protocol §A.3: founders, a quorum and terms. */
  record TestCharter(
      String charterId, Set<PersonaId> founders, int minimumSignatures, String terms)
      implements MultiSignableMessage {

    static final String MESSAGE_TYPE = "example.Charter@v1";

    @Override
    public Set<PersonaId> validSigners() {
      return founders;
    }

    @Override
    public int minimumRequiredSignatures() {
      return minimumSignatures;
    }

    @Override
    public boolean isValid() {
      return charterId != null && !charterId.isEmpty()
          && founders != null && !founders.isEmpty()
          && founders.stream().allMatch(id -> id != null && id.isValid())
          && minimumSignatures >= 1
          && minimumSignatures <= founders.size()
          && terms != null && !terms.isEmpty();
    }
  }

  // -------------------------------------------------------------------------------------
  // example.JointRequest@v1: multi-signable + request (§5.5 + §5.2 — the
  // replay-dedup target for multi-signed requests)
  // -------------------------------------------------------------------------------------

  /** A quorum-authorized request: founders co-sign one request id (§5.2, §5.5). */
  record TestJointRequest(
      String requestId, Set<PersonaId> founders, int minimumSignatures, String action)
      implements MultiSignableMessage, RequestMessage {

    static final String MESSAGE_TYPE = "example.JointRequest@v1";

    @Override
    public Set<PersonaId> validSigners() {
      return founders;
    }

    @Override
    public int minimumRequiredSignatures() {
      return minimumSignatures;
    }

    @Override
    public String requestId() {
      return requestId;
    }

    @Override
    public boolean isValid() {
      return requestId != null && !requestId.isEmpty()
          && founders != null && !founders.isEmpty()
          && founders.stream().allMatch(id -> id != null && id.isValid())
          && minimumSignatures >= 1
          && minimumSignatures <= founders.size()
          && action != null && !action.isEmpty();
    }
  }

  // -------------------------------------------------------------------------------------
  // example.MaintenanceAlert@v1: the alert broadcast payload (§5.4)
  // -------------------------------------------------------------------------------------

  /** A maintenance alert broadcast to a set of receivers (§5.4). */
  record TestAlert(
      String alertId, String realm, PersonaId from, List<PersonaId> receivers, String body)
      implements AlertMessage {

    static final String MESSAGE_TYPE = "example.MaintenanceAlert@v1";

    @Override
    public String messageType() {
      return MESSAGE_TYPE;
    }

    @Override
    public String alertType() {
      return MESSAGE_TYPE;
    }

    @Override
    public PersonaId signer() {
      return from;
    }

    @Override
    public boolean isValid() {
      return AlertMessage.super.isValid()
          && from != null && from.isValid()
          && body != null && !body.isEmpty();
    }
  }

  // -------------------------------------------------------------------------------------
  // Bindings (strict, fail closed, as MessageBinding requires)
  // -------------------------------------------------------------------------------------

  /**
   * The bindings for every p2p test message type.
   *
   * @return an immutable binding registry
   */
  static MessageBindings bindings() {
    return MessageBindings.of(
        greetingBinding(), charterBinding(), jointRequestBinding(), alertBinding());
  }

  private static MessageBinding greetingBinding() {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return TestGreeting.MESSAGE_TYPE;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        return new TestGreeting(
            text(payloadValue, "greetingId"),
            personaId(payloadValue, "from"),
            personaId(payloadValue, "to"),
            text(payloadValue, "body"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        TestGreeting greeting = as(message, TestGreeting.class);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("greetingId", greeting.greetingId());
        value.put("from", greeting.from().uniqueId());
        value.put("to", greeting.to().uniqueId());
        value.put("body", greeting.body());
        return value;
      }
    };
  }

  private static MessageBinding charterBinding() {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return TestCharter.MESSAGE_TYPE;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        Set<PersonaId> founders = new LinkedHashSet<>();
        for (Object founder : list(payloadValue, "founders")) {
          founders.add(PersonaId.of(asString(founder, "founders")));
        }
        return new TestCharter(
            text(payloadValue, "charterId"),
            founders,
            integer(payloadValue, "minimumSignatures"),
            text(payloadValue, "terms"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        TestCharter charter = as(message, TestCharter.class);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("charterId", charter.charterId());
        List<Object> founders = new ArrayList<>();
        for (PersonaId founder : charter.founders()) {
          founders.add(founder.uniqueId());
        }
        value.put("founders", founders);
        value.put("minimumSignatures", charter.minimumSignatures());
        value.put("terms", charter.terms());
        return value;
      }
    };
  }

  private static MessageBinding jointRequestBinding() {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return TestJointRequest.MESSAGE_TYPE;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        Set<PersonaId> founders = new LinkedHashSet<>();
        for (Object founder : list(payloadValue, "founders")) {
          founders.add(PersonaId.of(asString(founder, "founders")));
        }
        return new TestJointRequest(
            text(payloadValue, "requestId"),
            founders,
            integer(payloadValue, "minimumSignatures"),
            text(payloadValue, "action"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        TestJointRequest request = as(message, TestJointRequest.class);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("requestId", request.requestId());
        List<Object> founders = new ArrayList<>();
        for (PersonaId founder : request.founders()) {
          founders.add(founder.uniqueId());
        }
        value.put("founders", founders);
        value.put("minimumSignatures", request.minimumSignatures());
        value.put("action", request.action());
        return value;
      }
    };
  }

  private static MessageBinding alertBinding() {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return TestAlert.MESSAGE_TYPE;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        List<PersonaId> receivers = new ArrayList<>();
        for (Object receiver : list(payloadValue, "receivers")) {
          receivers.add(PersonaId.of(asString(receiver, "receivers")));
        }
        return new TestAlert(
            text(payloadValue, "alertId"),
            text(payloadValue, "realm"),
            personaId(payloadValue, "from"),
            receivers,
            text(payloadValue, "body"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        TestAlert alert = as(message, TestAlert.class);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("alertId", alert.alertId());
        value.put("realm", alert.realm());
        value.put("from", alert.from().uniqueId());
        List<Object> receivers = new ArrayList<>();
        for (PersonaId receiver : alert.receivers()) {
          receivers.add(receiver.uniqueId());
        }
        value.put("receivers", receivers);
        value.put("body", alert.body());
        return value;
      }
    };
  }

  // -------------------------------------------------------------------------------------
  // Strict bind-side helpers
  // -------------------------------------------------------------------------------------

  private static String text(Map<String, Object> value, String field) {
    return asString(value.get(field), field);
  }

  private static String asString(Object value, String field) {
    if (!(value instanceof String text)) {
      throw new IllegalArgumentException("field '" + field + "' must be a String");
    }
    return text;
  }

  private static PersonaId personaId(Map<String, Object> value, String field) {
    return PersonaId.of(asString(value.get(field), field));
  }

  private static int integer(Map<String, Object> value, String field) {
    if (!(value.get(field) instanceof Integer number)) {
      throw new IllegalArgumentException("field '" + field + "' must be an Integer");
    }
    return number;
  }

  private static List<?> list(Map<String, Object> value, String field) {
    if (!(value.get(field) instanceof List<?> list)) {
      throw new IllegalArgumentException("field '" + field + "' must be an array");
    }
    return list;
  }

  private static <M extends Validatable> M as(Validatable message, Class<M> type) {
    if (!type.isInstance(message)) {
      throw new IllegalArgumentException(
          "expected a " + type.getSimpleName() + " but found "
              + (message == null ? "null" : message.getClass().getName()));
    }
    return type.cast(Objects.requireNonNull(message, "message"));
  }
}
