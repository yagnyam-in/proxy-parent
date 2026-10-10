package in.yagnyam.persona.message;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Realm;
import java.util.List;

/**
 * A signed broadcast payload addressed to a set of receiver personas
 * (protocol §5.4: {@code SignableMessage} + broadcast).
 *
 * <p>Members of §5.4:
 * <ul>
 *   <li>{@link #realm()} — the realm identifier; the realm boundary MUST NOT
 *       be crossed (protocol §3.5, §11 rule 12);</li>
 *   <li>{@link #alertId()} — unique alert identifier for de-duplication;</li>
 *   <li>{@link #receivers()} — the target audience, which MUST be non-empty
 *       (protocol §11 rule 6);</li>
 *   <li>{@link #alertType()} — the same value as {@link #messageType()},
 *       surfaced separately so a client can dispatch without decoding the
 *       full body.</li>
 * </ul>
 *
 * <p>{@link #transportMetadata()} projects exactly
 * {@code {realm, alertType, alertId}} — the minimum every alert transport
 * MUST be given (protocol §5.4 last paragraph). Delivery substrates
 * themselves are outside this library (protocol §7.6 is about the wire
 * contract, not about push infrastructure).
 */
public interface AlertMessage extends SignableMessage {

  /**
   * The realm identifier of this alert (protocol §5.4).
   *
   * @return realm, e.g. {@code production}; never {@code null} for a valid
   *     payload
   */
  String realm();

  /**
   * The unique alert identifier, used for de-duplication (protocol §5.4).
   *
   * @return alert id, never {@code null} for a valid payload
   */
  String alertId();

  /**
   * The receiver personas of this broadcast (protocol §5.4); MUST be
   * non-empty (protocol §11 rule 6).
   *
   * @return receivers, never {@code null} for a valid payload
   */
  List<PersonaId> receivers();

  /**
   * The alert type: the same value as {@link #messageType()}, surfaced
   * separately so a client can dispatch without decoding the full body
   * (protocol §5.4).
   *
   * @return alert type, equal to {@link #messageType()} for a valid payload
   */
  String alertType();

  /**
   * The transport-level metadata projection {@code {realm, alertType,
   * alertId}} that every alert transport MUST be given (protocol §5.4 last
   * paragraph).
   *
   * @return transport metadata, never {@code null}
   */
  default AlertTransportMetadata transportMetadata() {
    return new AlertTransportMetadata(realm(), alertType(), alertId());
  }

  /**
   * The alert-specific invariants of §5.4 and §11 rule 6: a well-formed
   * realm, a non-empty alert id, {@code alertType() == messageType()}, and a
   * non-empty receiver list of valid persona ids.
   *
   * <p>Payloads overriding this method to add their own domain rules MUST
   * include this check, e.g.
   * {@code isValid() && AlertMessage.super.isValid() && myOwnRules()},
   * otherwise protocol §11 rule 6 is no longer enforced for them.
   *
   * @return {@code true} if the alert metadata of this payload is valid
   */
  @Override
  default boolean isValid() {
    if (messageType() == null || messageType().isEmpty()) {
      return false;
    }
    if (realm() == null || !Realm.isValidRealm(realm())) {
      return false;
    }
    if (alertId() == null || alertId().isEmpty()) {
      return false;
    }
    if (alertType() == null || !alertType().equals(messageType())) {
      return false;
    }
    List<PersonaId> audience = receivers();
    if (audience == null || audience.isEmpty()) {
      return false;
    }
    for (PersonaId receiver : audience) {
      if (receiver == null || !receiver.isValid()) {
        return false;
      }
    }
    return true;
  }
}
