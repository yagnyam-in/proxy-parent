package in.yagnyam.persona.envelope;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.message.MultiSignableMessage;
import in.yagnyam.persona.message.SignableMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Test payloads of the §5 contracts and their {@link MessageBinding}s:
 * a single-signable greeting (twice, isomorphically, for the §6.4
 * type-confusion tests), a payment authorization carrying nested envelopes
 * (§6.3), a multi-signable charter (§5.5, §A.3) and an escrow instruction
 * implementing both contracts (for the §6.4 domain-transplant tests).
 *
 * <p>The schemas of these payloads live in {@link EnvelopeWorld}; the
 * bindings here are strict (fail closed), as {@link MessageBinding}
 * requires.
 */
final class TestMessages {

  private TestMessages() {}

  // -------------------------------------------------------------------------------------
  // example.Greeting@v1 (§A.1): the minimal single-signable domain message
  // -------------------------------------------------------------------------------------

  /**
   * The greeting payload of protocol §A.1: {@code greetingId}, {@code from}
   * (signer), {@code to} (recipient), {@code body}.
   */
  record TestGreeting(
      String greetingId, PersonaId from, PersonaId to, String body) implements SignableMessage {

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
    public boolean isValid() {
      return greetingId != null && !greetingId.isEmpty()
          && from != null && from.isValid()
          && to != null && to.isValid()
          && body != null && !body.isEmpty();
    }
  }

  /** An isomorphic copy of the greeting under a second message type. */
  record TestGreeting2(
      String greetingId, PersonaId from, PersonaId to, String body) implements SignableMessage {

    static final String MESSAGE_TYPE = "example.Greeting2@v1";

    @Override
    public String messageType() {
      return MESSAGE_TYPE;
    }

    @Override
    public PersonaId signer() {
      return from;
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
  // example.PaymentAuthorization@v1 (§6.3): payload with nested envelopes
  // -------------------------------------------------------------------------------------

  /**
   * A payment authorization (protocol §6.3): the payer's account is a nested
   * envelope signed by another persona, and the approvals array carries
   * more nested envelopes — so the recursive verification of §7.5 walks a
   * field, a collection and a second signer.
   */
  record TestPaymentAuthorization(
      String authorizationId,
      PersonaId payer,
      SignedEnvelope payerAccount,
      String amount,
      List<SignedEnvelope> approvals) implements SignableMessage {

    static final String MESSAGE_TYPE = "example.PaymentAuthorization@v1";

    @Override
    public String messageType() {
      return MESSAGE_TYPE;
    }

    @Override
    public PersonaId signer() {
      return payer;
    }

    @Override
    public boolean isValid() {
      return authorizationId != null && !authorizationId.isEmpty()
          && payer != null && payer.isValid()
          && payerAccount != null
          && amount != null && !amount.isEmpty()
          && approvals != null;
    }
  }

  // -------------------------------------------------------------------------------------
  // example.Charter@v1 (§5.5, §A.3): the multi-signable quorum payload
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
  // example.EscrowInstruction@v1: both contracts (for §6.4 domain-transplant tests)
  // -------------------------------------------------------------------------------------

  /**
   * An escrow instruction implementing <em>both</em> contracts: either
   * party may sign it alone (§5.1) or the two may co-sign it (§5.5), which
   * is what lets the tests transplant a single-signed signature into a
   * multi-signed envelope (and back) carrying identical payload bytes —
   * only the §6.4 domain separation can reject it.
   */
  record TestEscrowInstruction(
      String instructionId, PersonaId partyA, PersonaId partyB, String amount)
      implements SignableMessage, MultiSignableMessage {

    static final String MESSAGE_TYPE = "example.EscrowInstruction@v1";

    @Override
    public String messageType() {
      return MESSAGE_TYPE;
    }

    @Override
    public PersonaId signer() {
      return partyA;
    }

    /**
     * One implementation for both interfaces: §5.1's default is
     * overridden because a multi-signable payload authorizes its whole
     * signer set, and §5.5's abstract member resolves to the same method.
     */
    @Override
    public Set<PersonaId> validSigners() {
      return Set.of(partyA, partyB);
    }

    @Override
    public int minimumRequiredSignatures() {
      return 1;
    }

    @Override
    public boolean isValid() {
      return instructionId != null && !instructionId.isEmpty()
          && partyA != null && partyA.isValid()
          && partyB != null && partyB.isValid()
          && amount != null && !amount.isEmpty();
    }
  }

  // -------------------------------------------------------------------------------------
  // Bindings
  // -------------------------------------------------------------------------------------

  /**
   * The bindings for every test message type.
   *
   * @return an immutable binding registry
   */
  static MessageBindings bindings() {
    return MessageBindings.of(
        greetingBinding(), greeting2Binding(), paymentAuthorizationBinding(),
        charterBinding(), escrowInstructionBinding());
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

  private static MessageBinding greeting2Binding() {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return TestGreeting2.MESSAGE_TYPE;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        return new TestGreeting2(
            text(payloadValue, "greetingId"),
            personaId(payloadValue, "from"),
            personaId(payloadValue, "to"),
            text(payloadValue, "body"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        TestGreeting2 greeting = as(message, TestGreeting2.class);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("greetingId", greeting.greetingId());
        value.put("from", greeting.from().uniqueId());
        value.put("to", greeting.to().uniqueId());
        value.put("body", greeting.body());
        return value;
      }
    };
  }

  private static MessageBinding paymentAuthorizationBinding() {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return TestPaymentAuthorization.MESSAGE_TYPE;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        return new TestPaymentAuthorization(
            text(payloadValue, "authorizationId"),
            personaId(payloadValue, "payer"),
            signedEnvelope(payloadValue, "payerAccount"),
            text(payloadValue, "amount"),
            signedEnvelopes(payloadValue, "approvals"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        TestPaymentAuthorization authorization = as(message, TestPaymentAuthorization.class);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("authorizationId", authorization.authorizationId());
        value.put("payer", authorization.payer().uniqueId());
        value.put("payerAccount", authorization.payerAccount().toValue());
        value.put("amount", authorization.amount());
        List<Object> approvals = new ArrayList<>();
        for (SignedEnvelope approval : authorization.approvals()) {
          approvals.add(approval.toValue());
        }
        value.put("approvals", approvals);
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

  private static MessageBinding escrowInstructionBinding() {
    return new MessageBinding() {
      @Override
      public String messageType() {
        return TestEscrowInstruction.MESSAGE_TYPE;
      }

      @Override
      public Validatable bind(Map<String, Object> payloadValue) {
        return new TestEscrowInstruction(
            text(payloadValue, "instructionId"),
            personaId(payloadValue, "partyA"),
            personaId(payloadValue, "partyB"),
            text(payloadValue, "amount"));
      }

      @Override
      public Map<String, Object> wireValue(Validatable message) {
        TestEscrowInstruction instruction = as(message, TestEscrowInstruction.class);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("instructionId", instruction.instructionId());
        value.put("partyA", instruction.partyA().uniqueId());
        value.put("partyB", instruction.partyB().uniqueId());
        value.put("amount", instruction.amount());
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
    // Type-strict but content-agnostic: whether a string may be empty is a
    // domain rule of the payload's isValid(), not of the binding — an
    // isValid() violation must be reachable for the verifier to reject it
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

  private static SignedEnvelope signedEnvelope(Map<String, Object> value, String field) {
    if (!(value.get(field) instanceof Map<?, ?> envelopeValue)) {
      throw new IllegalArgumentException("field '" + field + "' must be a nested envelope record");
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> entries = (Map<String, Object>) envelopeValue;
    return SignedEnvelope.fromValue(entries);
  }

  private static List<SignedEnvelope> signedEnvelopes(Map<String, Object> value, String field) {
    List<SignedEnvelope> envelopes = new ArrayList<>();
    for (Object item : list(value, field)) {
      if (!(item instanceof Map<?, ?> envelopeValue)) {
        throw new IllegalArgumentException(
            "field '" + field + "' must be an array of nested envelope records");
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> entries = (Map<String, Object>) envelopeValue;
      envelopes.add(SignedEnvelope.fromValue(entries));
    }
    return envelopes;
  }

  private static <M extends Validatable> M as(Validatable message, Class<M> type) {
    if (!type.isInstance(message)) {
      throw new IllegalArgumentException(
          "expected a " + type.getSimpleName() + " for messageType "
              + (message instanceof SignableMessage signable
                  ? signable.messageType()
                  : "<unknown>")
              + " but found " + (message == null ? "null" : message.getClass().getName()));
    }
    return type.cast(Objects.requireNonNull(message, "message"));
  }
}
