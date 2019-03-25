package in.yagnyam.proxy;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alert Message
 */
public interface SignableAlertMessage extends SignableMessage {

  String ALERT_TYPE = "alertType";
  String ALERT_ID = "alertId";

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
    map.put(ALERT_TYPE, getMessageType());
    map.put(ALERT_ID, alertId());
    return map;
  }
}
