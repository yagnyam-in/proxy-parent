package in.yagnyam.proxy;

/**
 * Interfaced for Request messages with Request Id
 */
public interface RequestMessage extends ProxyBaseObject {

  /**
   * Request Id
   *
   * @return Request Id
   */
  String requestId();
}
