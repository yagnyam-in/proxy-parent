package in.yagnyam.proxy;


import java.util.List;

/**
 * Alert Message
 */
public interface SignableAlertMessage extends SignableMessage {

  String alertId();

  List<ProxyId> receivers();
}
