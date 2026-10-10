package in.yagnyam.persona.envelope;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@link MessageBinding} registry of a peer: one binding per message
 * type (protocol §5.1 — the message type is the stable routing identifier).
 *
 * <p>Verification requires a binding for every message type it accepts: a
 * payload the recipient cannot materialize cannot have its
 * {@code isValid()} (protocol §7.3 step 3) or its signer authorization
 * (protocol §7.3 step 5) asserted, and protocol §4 makes either a hard
 * rejection. Registering a binding is therefore the recipient's statement
 * that it understands a message type, exactly as registering the schema is.
 */
public final class MessageBindings {

  private final Map<String, MessageBinding> bindingsByMessageType;

  private MessageBindings(Map<String, MessageBinding> bindingsByMessageType) {
    this.bindingsByMessageType = bindingsByMessageType;
  }

  /**
   * Creates a registry of the given bindings.
   *
   * @param bindings the bindings, in any order
   * @return an immutable registry
   * @throws NullPointerException if {@code bindings} or an element is
   *     {@code null}
   * @throws IllegalArgumentException if two bindings declare the same
   *     message type
   */
  public static MessageBindings of(MessageBinding... bindings) {
    Objects.requireNonNull(bindings, "bindings");
    Map<String, MessageBinding> byMessageType = new LinkedHashMap<>();
    for (MessageBinding binding : bindings) {
      Objects.requireNonNull(binding, "binding");
      MessageBinding existing = byMessageType.putIfAbsent(binding.messageType(), binding);
      if (existing != null) {
        throw new IllegalArgumentException(
            "messageType '" + binding.messageType() + "' is bound twice (protocol §5.1: the"
                + " message type is the stable identifier)");
      }
    }
    return new MessageBindings(Map.copyOf(byMessageType));
  }

  /**
   * Looks up the binding for a message type.
   *
   * @param messageType message type
   * @return the binding, or {@link Optional#empty()} when the peer has not
   *     registered one
   */
  public Optional<MessageBinding> forMessageType(String messageType) {
    Objects.requireNonNull(messageType, "messageType");
    return Optional.ofNullable(bindingsByMessageType.get(messageType));
  }

  /**
   * Looks up the binding for a message type, failing when absent.
   *
   * @param messageType message type
   * @return the binding
   * @throws IllegalArgumentException when no binding is registered for the
   *     message type
   */
  public MessageBinding require(String messageType) {
    return forMessageType(messageType)
        .orElseThrow(() -> new IllegalArgumentException(
            "no message binding registered for messageType '" + messageType
                + "' (protocol §7.3 steps 3 and 5: the payload must be materialized to"
                + " assert its validity and the signer's authorization)"));
  }

  @Override
  public String toString() {
    return "MessageBindings[" + bindingsByMessageType.keySet() + "]";
  }
}
