package in.yagnyam.proxy;


import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.services.MessageSerializerService;

import java.util.Set;

/**
 * Message that can be requires multiple signatures.
 * <p>
 * Message should be annotated with Jackson bindings to be serialized properly.
 *
 * @see MessageSerializerService
 */
public interface MultiSignableMessage {

    /**
     * Validate if these are valid signers
     * @return Set of Proxies that can sign this message
     */
   boolean validateSigners(Set<ProxyId> signers);


  /**
   * Check if the message has required number of signatures from given Signers
   * @return Set of Proxies that can sign this message
   */
  boolean hasRequiredSignatures(Set<ProxyId> signers);

  /**
   * Return this message in human readable format
   *
   * @return Message in Human readable format
   */
  String toReadableString();

  /**
   * Tests if the message is valid
   *
   * @return true if message is valid, false otherwise
   */
  @JsonIgnore
  boolean isValid();

  /**
   * Test if the Signer can sign this message
   * @param signerId Signer Proxy Id
   * @return true if signer can sign this message
   */
  boolean canBeSignedBy(ProxyId signerId);

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

}
