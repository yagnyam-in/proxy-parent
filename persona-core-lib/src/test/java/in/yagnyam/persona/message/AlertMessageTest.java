package in.yagnyam.persona.message;

import static org.assertj.core.api.Assertions.assertThat;

import in.yagnyam.persona.PersonaId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link AlertMessage} contract (protocol §5.4, §7.6): the
 * four members, the {@code {realm, alertType, alertId}} transport-metadata
 * projection every alert transport MUST be given, and the default validity
 * check enforcing §11 rule 6 (receivers MUST be non-empty).
 *
 * <p>The fixture is a plain (non-record) class so the tests can override
 * individual members anonymously.
 */
class AlertMessageTest {

  /** Minimal §5.4 alert payload for the contract tests. */
  private static class TestAlert implements AlertMessage {

    private final String alertId;
    private final List<PersonaId> receivers;
    private final PersonaId from;

    private TestAlert(String alertId, List<PersonaId> receivers, PersonaId from) {
      this.alertId = alertId;
      this.receivers = receivers;
      this.from = from;
    }

    @Override
    public String messageType() {
      return "example.FraudSuspicion@v1";
    }

    @Override
    public PersonaId signer() {
      return from;
    }

    @Override
    public String realm() {
      return "production";
    }

    @Override
    public String alertId() {
      return alertId;
    }

    @Override
    public List<PersonaId> receivers() {
      return receivers;
    }

    @Override
    public String alertType() {
      return messageType();
    }

    @Override
    public boolean isValid() {
      return from != null && from.isValid() && AlertMessage.super.isValid();
    }
  }

  /** An alert payload that does NOT call the default validity check. */
  private record TestOverridingAlert(String alertId, List<PersonaId> receivers)
      implements AlertMessage {

    @Override
    public String messageType() {
      return "example.Overriding@v1";
    }

    @Override
    public PersonaId signer() {
      return PersonaId.of("p-issuer");
    }

    @Override
    public String realm() {
      return "production";
    }

    @Override
    public String alertType() {
      return messageType();
    }

    @Override
    public boolean isValid() {
      // Overrides without calling AlertMessage.super.isValid(): §11 rule 6
      // is then the implementer's responsibility, not the contract's
      return true;
    }
  }

  @Test
  void exposesTheFourMembersOfSectionFiveFour() {
    PersonaId issuer = PersonaId.of("p-issuer");
    TestAlert alert = new TestAlert("a-1", List.of(PersonaId.of("p-bob")), issuer);

    assertThat(alert.messageType()).isEqualTo("example.FraudSuspicion@v1");
    assertThat(alert.signer()).isEqualTo(issuer);
    assertThat(alert.realm()).isEqualTo("production");
    assertThat(alert.alertId()).isEqualTo("a-1");
    assertThat(alert.receivers()).containsExactly(PersonaId.of("p-bob"));
    assertThat(alert.alertType()).isEqualTo(alert.messageType());
  }

  @Test
  void projectsTheTransportMetadataEveryTransportMustBeGiven() {
    TestAlert alert =
        new TestAlert("a-1", List.of(PersonaId.of("p-bob")), PersonaId.of("p-issuer"));

    AlertTransportMetadata metadata = alert.transportMetadata();

    assertThat(metadata.realm()).isEqualTo("production");
    assertThat(metadata.alertType()).isEqualTo("example.FraudSuspicion@v1");
    assertThat(metadata.alertId()).isEqualTo("a-1");
    assertThat(metadata.toString())
        .contains("production")
        .contains("example.FraudSuspicion@v1")
        .contains("a-1");
  }

  @Test
  void defaultIsValidAcceptsAWellFormedAlert() {
    TestAlert alert = new TestAlert("a-1",
        List.of(PersonaId.of("p-bob"), PersonaId.of("p-carol")), PersonaId.of("p-issuer"));

    assertThat(alert.isValid()).isTrue();
  }

  @Test
  void defaultIsValidEnforcesRuleSixAndTheAlertShape() {
    PersonaId issuer = PersonaId.of("p-issuer");

    // §11 rule 6: receivers MUST be non-empty
    assertThat(new TestAlert("a-1", List.of(), issuer).isValid()).isFalse();
    assertThat(new TestAlert("a-1", null, issuer).isValid()).isFalse();
    assertThat(new TestAlert("a-1", Arrays.asList(PersonaId.of("p-bob"), null), issuer)
        .isValid()).isFalse();

    // A malformed realm is rejected (§3.5: realms are lowercase/hyphen/dot)
    assertThat(new TestAlert("a-1", List.of(PersonaId.of("p-bob")), issuer) {
      @Override
      public String realm() {
        return "Not A Realm";
      }
    }.isValid()).isFalse();

    // A missing alert id is rejected
    assertThat(new TestAlert("", List.of(PersonaId.of("p-bob")), issuer).isValid()).isFalse();

    // A missing messageType is rejected
    assertThat(new TestAlert("a-1", List.of(PersonaId.of("p-bob")), issuer) {
      @Override
      public String messageType() {
        return "";
      }
    }.isValid()).isFalse();

    // alertType must equal messageType (§5.4)
    assertThat(new TestAlert("a-1", List.of(PersonaId.of("p-bob")), issuer) {
      @Override
      public String alertType() {
        return "example.SomethingElse@v1";
      }
    }.isValid()).isFalse();
  }

  @Test
  void overridingIsValidWithoutTheSuperCallIsTheImplementersResponsibility() {
    // Pins the contract's boundary: the default enforces §11 rule 6 only
    // for payloads that keep it; a payload overriding isValid() without
    // AlertMessage.super.isValid() silently loses the rule — which is why
    // the javadoc requires the super call.
    TestOverridingAlert overriding = new TestOverridingAlert("a-1", List.of());

    assertThat(overriding.isValid()).isTrue();
    assertThat(overriding.receivers()).isEmpty();
  }
}
