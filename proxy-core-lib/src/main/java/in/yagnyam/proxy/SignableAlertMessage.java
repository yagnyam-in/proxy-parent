package in.yagnyam.proxy;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alert Message
 */
public interface SignableAlertMessage extends SignableMessage {

  String FIELD_ALERT_TYPE = "alertType";
  String FIELD_ALERT_ID = "alertId";
  String FIELD_PROXY_UNIVERSE = "proxyUniverse";
  String FIELD_RECEIVER_PROXY_ID = "receiverProxyId";

  /**
   * Proxy Universe
   * @return Proxy Universe
   */
  String proxyUniverse();

  /**
   * Alert Id
   * @return Alert Id
   */
  String alertId();

  /**
   * List of Proxy Id's that shall receive this alert
   * @return Alert receivers
   */
  List<ProxyId> receivers();

  /**
   * Minimum required fields for Sending this message as FCM Message so that Client can act on it.
   *
   * @return Map of minimum required fields for this Alert.
   */
  default Map<String, String> toFcmMap() {
    Map<String, String> map = new HashMap<>();
    map.put(FIELD_PROXY_UNIVERSE, proxyUniverse());
    map.put(FIELD_ALERT_TYPE, getMessageType());
    map.put(FIELD_ALERT_ID, alertId());
    return map;
  }
}
