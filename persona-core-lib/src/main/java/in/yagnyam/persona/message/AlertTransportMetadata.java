package in.yagnyam.persona.message;

import java.util.Objects;

/**
 * The transport-level metadata every alert transport MUST be given
 * (protocol §5.4 last paragraph, §7.6): at least {@code {realm, alertType,
 * alertId}}.
 *
 * <p>Alert substrates (FCM, Web Push, MQTT, WebSocket, mesh) deliver this
 * projection alongside (or instead of) the full {@code SignedEnvelope}, so a
 * client can dispatch or de-duplicate an alert without decoding the signed
 * body (protocol §7.6: the full envelope MAY be fetched separately or
 * attached inline).
 *
 * @param realm     realm identifier of the alerting persona
 * @param alertType same as the payload's {@code messageType()} (protocol §5.4)
 * @param alertId   unique alert identifier, used for de-duplication
 */
public record AlertTransportMetadata(String realm, String alertType, String alertId) {

  public AlertTransportMetadata {
    Objects.requireNonNull(realm, "realm");
    Objects.requireNonNull(alertType, "alertType");
    Objects.requireNonNull(alertId, "alertId");
  }

  @Override
  public String toString() {
    return "AlertTransportMetadata[realm=" + realm
        + ", alertType=" + alertType
        + ", alertId=" + alertId + "]";
  }
}
