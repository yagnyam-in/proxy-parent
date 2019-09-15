package in.yagnyam.proxy;


import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.services.MessageSerializerService;

import java.util.Collections;
import java.util.Set;

/**
 * Message that can be signed by Proxy.
 * <p>
 * Message should be annotated with Jackson bindings to be serialized properly.
 *
 * @see MessageSerializerService
 */
public interface SignableMessage extends ProxyBaseObject {

  /**
   * Proxy Id of the Subject that must sign this message?
   *
   * @return Signed Proxy Id
   */
  ProxyId signer();

  /**
   * Signers that can sign this message.
   * @return Set of Proxies that can sign this message
   */
  default Set<ProxyId> validSigners() {
    return Collections.singleton(signer());
  }

  /**
   * Tests if the message is valid
   *
   * @return true if message is valid, false otherwise
   */
  @JsonIgnore
  boolean isValid();

  /**
   * Type of the Message
   *
   * @return Type of the Message
   */
  default String getMessageType() {
    return getClass().getName();
  }

  /**
   * Set type of the message. Never invoke directly, meant to be used by Json DeSerializer
   *
   * @param type Type of the message
   */
  default void setMessageType(String type) {
    if (!getMessageType().equals(type)) {
      throw new IllegalArgumentException(
          "Invalid Type " + type + ". It must be " + getMessageType());
    }
  }

  /**
   * Test if the Signer can sign this message
   * @param signerId Signer Proxy Id
   * @return true if signer can sign this message
   */
  default boolean cabBeSignedBy(ProxyId signerId) {
    return validSigners().stream().anyMatch(signerId::canSignOnBehalfOf);
  }

}
