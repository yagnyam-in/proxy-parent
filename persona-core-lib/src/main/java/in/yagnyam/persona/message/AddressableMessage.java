package in.yagnyam.persona.message;

import in.yagnyam.persona.PersonaId;

/**
 * A payload that declares its intended recipient (protocol §5.3).
 *
 * <p>The {@link #address()} enables routing without inspecting the payload
 * contents: the envelope carries the payload as opaque bytes (protocol
 * §6.1), so the recipient persona must be available from the payload's own
 * metadata.
 *
 * <p>Orthogonal to {@link SignableMessage}: an addressable message is signed
 * like any other payload.
 */
public interface AddressableMessage {

  /**
   * The target persona of this message (protocol §5.3).
   *
   * @return recipient persona id, never {@code null} for a valid payload
   */
  PersonaId address();
}
