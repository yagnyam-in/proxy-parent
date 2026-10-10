package in.yagnyam.persona.envelope;

import in.yagnyam.persona.Validatable;
import java.util.Map;

/**
 * Bidirectional binding between a payload's wire value and its domain
 * message — the engineering bridge between the encoding-agnostic contracts
 * of protocol §5 and the encoding-agnostic envelope of protocol §6.
 *
 * <p>The protocol itself defines no Java bindings: a payload is any typed,
 * structured record (protocol §2) and the wire value model is a
 * {@code Map<String,Object>} of field name to value
 * ({@code docs/SCHEMA.md} §4.2). Signing and verification both need to
 * cross that boundary exactly once per direction — signing serializes the
 * payload once (protocol §7.2 step 2), verification materializes it to
 * assert {@code isValid()} (protocol §7.3 step 3) and the signer
 * authorization (protocol §7.3 step 5). A registered binding per
 * {@code messageType} is what lets the verifier do so without reflection.
 *
 * <p>Implementations are domain glue and MUST:
 * <ul>
 *   <li>return the bound {@code messageType()} (protocol §5.1) for both
 *       directions;</li>
 *   <li>fail closed in {@link #bind(Map)}: a value that does not match the
 *       message's shape is rejected with an informative
 *       {@code IllegalArgumentException} rather than silently coerced;</li>
 *   <li>reject a {@code message} of the wrong concrete type in
 *       {@link #wireValue(Validatable)} with an informative
 *       {@code IllegalArgumentException}, never a bare
 *       {@code ClassCastException};</li>
 *   <li>not retain or mutate the maps they receive.</li>
 * </ul>
 */
public interface MessageBinding {

  /**
   * The message type this binding is registered for (protocol §5.1); peers
   * resolve both the payload schema and this binding from the envelope's
   * {@code messageType}.
   *
   * @return message type, never {@code null}
   */
  String messageType();

  /**
   * Materializes the domain message from a decoded payload value
   * (protocol §7.3 step 3).
   *
   * @param payloadValue the decoded payload value ({@code docs/SCHEMA.md}
   *                     §4.2 value model)
   * @return the domain message
   * @throws NullPointerException if {@code payloadValue} is {@code null}
   * @throws IllegalArgumentException if the value does not match the
   *     message shape
   */
  Validatable bind(Map<String, Object> payloadValue);

  /**
   * The wire value of a domain message (protocol §7.2 step 2: the value
   * that is then serialized exactly once).
   *
   * @param message the domain message of this binding's concrete type
   * @return the {@code docs/SCHEMA.md} §4.2 value of {@code message}
   * @throws IllegalArgumentException if {@code message} is not of this
   *     binding's concrete type
   */
  Map<String, Object> wireValue(Validatable message);
}
