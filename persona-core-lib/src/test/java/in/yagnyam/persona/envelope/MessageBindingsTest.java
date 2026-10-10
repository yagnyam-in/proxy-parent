package in.yagnyam.persona.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.yagnyam.persona.Validatable;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MessageBindings}: registry semantics, duplicate
 * registration rejection and the failure-closed lookup.
 */
class MessageBindingsTest {

  private static final class StubBinding implements MessageBinding {

    private final String messageType;

    private StubBinding(String messageType) {
      this.messageType = messageType;
    }

    @Override
    public String messageType() {
      return messageType;
    }

    @Override
    public Validatable bind(Map<String, Object> payloadValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> wireValue(Validatable message) {
      throw new UnsupportedOperationException();
    }
  }

  @Test
  void registersAndLooksUpBindings() {
    MessageBindings bindings =
        MessageBindings.of(new StubBinding("example.A@v1"), new StubBinding("example.B@v1"));

    assertThat(bindings.forMessageType("example.A@v1")).isPresent();
    assertThat(bindings.forMessageType("example.B@v1")).isPresent();
    assertThat(bindings.forMessageType("example.C@v1")).isEmpty();
    assertThat(bindings.require("example.A@v1").messageType()).isEqualTo("example.A@v1");
  }

  @Test
  void rejectsDuplicateMessageTypes() {
    assertThatThrownBy(() -> MessageBindings.of(
        new StubBinding("example.A@v1"), new StubBinding("example.A@v1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bound twice")
        .hasMessageContaining("example.A@v1");
  }

  @Test
  void requireNamesTheMissingMessageTypeAndTheProtocolSteps() {
    MessageBindings bindings = MessageBindings.of(new StubBinding("example.A@v1"));

    assertThatThrownBy(() -> bindings.require("example.Missing@v1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no message binding registered for messageType")
        .hasMessageContaining("example.Missing@v1")
        .hasMessageContaining("§7.3");
  }

  @Test
  void rejectsNullLookups() {
    MessageBindings bindings = MessageBindings.of();
    assertThatThrownBy(() -> bindings.forMessageType(null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> bindings.require(null))
        .isInstanceOf(NullPointerException.class);
  }
}
