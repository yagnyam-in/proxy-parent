package in.yagnyam.proxy;


import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.services.MessageSerializerService;

import java.util.Collections;
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
   * Signers that can sign this message.
   * @return Set of Proxies that can sign this message
   */
  Set<ProxyId> validSigners();

  /**
   * Minimum number of signature required for this message to be Complete
   * @return Minimum required signatures.
   */
  int minimumRequiredSignatures();

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
   * Validate if these are valid signers
   * @return Set of Proxies that can sign this message
   */
  default boolean validateSigners(Set<ProxyId> signers) {
    Set<ProxyId> validSigners = validSigners();
    return validSigners.containsAll(signers);
  }

}
