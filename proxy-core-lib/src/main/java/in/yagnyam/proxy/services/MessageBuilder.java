package in.yagnyam.proxy.services;

import in.yagnyam.proxy.MultiSignableMessage;
import in.yagnyam.proxy.MultiSignedMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import java.io.IOException;
import java.lang.reflect.Field;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@SuppressWarnings("unchecked")
public class MessageBuilder {

  @NonNull
  private MessageSerializerService serializer;

  public <T extends SignableMessage> SignedMessage<T> buildSignedMessage(
      String signedMessage, Class<T> underlyingMessageClass)
      throws IOException {
    SignedMessage signedMessageObject = serializer.deserializeSignedMessage(signedMessage);
    return populateSignedMessage(signedMessageObject, underlyingMessageClass);
  }

  public <T extends MultiSignableMessage> MultiSignedMessage<T> buildMultiSignedMessage(
      String signedMessage, Class<T> underlyingMessageClass)
      throws IOException {
    MultiSignedMessage signedMessageObject = serializer.deserializeMultiSignedMessage(signedMessage);
    return populateMultiSignedMessage(signedMessageObject, underlyingMessageClass);
  }

  public <T extends SignableMessage> SignedMessage<T> populateSignedMessage(
      SignedMessage signedMessage, Class<T> underlyingMessageClass)
      throws IOException {
    log.debug("populating " + underlyingMessageClass);
    SignedMessage extracted = populateSignedMessage(signedMessage);
    String underlyingMessageType = signedMessage.getType();
    if (underlyingMessageClass != null &&
        underlyingMessageClass.getSimpleName().equals(underlyingMessageType)) {
      throw new IllegalArgumentException("Message type " + underlyingMessageType
          + " from SignedMessage is not " + underlyingMessageClass);
    }
    return extracted;
  }


  public <T extends MultiSignableMessage> MultiSignedMessage<T> populateMultiSignedMessage(
      MultiSignedMessage signedMessage, Class<T> underlyingMessageClass)
      throws IOException {
    log.debug("populating " + underlyingMessageClass);
    MultiSignedMessage extracted = populateMultiSignedMessage(signedMessage);
    String underlyingMessageType = signedMessage.getType();
    if (underlyingMessageClass != null &&
        underlyingMessageClass.getSimpleName().equals(underlyingMessageType)) {
      throw new IllegalArgumentException("Message type " + underlyingMessageType
          + " from MultiSignedMessage is not " + underlyingMessageClass);
    }
    return extracted;
  }

  private SignedMessage populateSignedMessage(SignedMessage signedMessage) throws IOException {
    log.debug("verifying signature for " + signedMessage);
    String underlyingMessageType = signedMessage.getType();
    try {
      Class messageClass = Class.forName(underlyingMessageType);
      SignableMessage underlyingMessage = buildSignableMessage(signedMessage.getPayload(), messageClass);
      if (!underlyingMessageType.equals(underlyingMessage.getMessageType())) {
        log.error("Message type " + underlyingMessageType
            + " from SignedMessage doesn't match Message type from Payload " + underlyingMessage
            .getMessageType());
        throw new IllegalArgumentException("Message type " + underlyingMessageType
            + " from SignedMessage doesn't match Message type from Payload " + underlyingMessage
            .getMessageType());
      }
      if (!underlyingMessage.isValid()) {
        log.error("Invalid message: " + underlyingMessage);
        throw new IllegalArgumentException("Invalid message");
      }
      return signedMessage.setMessage(underlyingMessage);
    } catch (ClassNotFoundException e) {
      log.error("Unknown message type " + underlyingMessageType, e);
      throw new IllegalArgumentException("Unknown message type " + underlyingMessageType);
    }
  }

  private MultiSignedMessage populateMultiSignedMessage(MultiSignedMessage signedMessage) throws IOException {
    log.debug("verifying signature for " + signedMessage);
    String underlyingMessageType = signedMessage.getType();
    try {
      Class messageClass = Class.forName(underlyingMessageType);
      MultiSignableMessage underlyingMessage = buildMultiSignableMessage(signedMessage.getPayload(), messageClass);
      if (!underlyingMessageType.equals(underlyingMessage.getMessageType())) {
        log.error("Message type " + underlyingMessageType
            + " from SignedMessage doesn't match Message type from Payload " + underlyingMessage
            .getMessageType());
        throw new IllegalArgumentException("Message type " + underlyingMessageType
            + " from SignedMessage doesn't match Message type from Payload " + underlyingMessage
            .getMessageType());
      }
      if (!underlyingMessage.isValid()) {
        log.error("Invalid message: " + underlyingMessage);
        throw new IllegalArgumentException("Invalid message");
      }
      return signedMessage.setMessage(underlyingMessage);
    } catch (ClassNotFoundException e) {
      log.error("Unknown message type " + underlyingMessageType, e);
      throw new IllegalArgumentException("Unknown message type " + underlyingMessageType);
    }
  }


  private <T extends SignableMessage> T buildSignableMessage(String signableMessage,
      Class<T> messageClass) throws IOException {
    log.debug("buildSignableMessage({}, {})", signableMessage, messageClass);
    T signableMessageObject = serializer.deserializeSignableMessage(signableMessage, messageClass);
    populateChildMessages(signableMessageObject, messageClass);
    return signableMessageObject;
  }


  private <T extends MultiSignableMessage> T buildMultiSignableMessage(String signableMessage,
      Class<T> messageClass) throws IOException {
    log.debug("buildMultiSignableMessage({}, {})", signableMessage, messageClass);
    T multiSignableMessageObject = serializer.deserializeMultiSignableMessage(signableMessage, messageClass);
    populateChildMessages(multiSignableMessageObject, messageClass);
    return multiSignableMessageObject;
  }

  private <T> void populateChildMessages(T message, Class<T> messageClass) throws IOException {
    Field[] fields = message.getClass().getDeclaredFields();
    try {
      // See if any signed message fields are there
      for (Field f : fields) {
        if (f.getType().isAssignableFrom(SignedMessage.class)) {
          log.debug("verifying field of type " + f.getType());
          SignedMessage signedMessage = (SignedMessage) f.get(message);
          // TODO: Use Setter instead of directly setting the field
          f.set(message, populateSignedMessage(signedMessage));
        }
        if (f.getType().isAssignableFrom(MultiSignedMessage.class)) {
          log.debug("verifying field of type " + f.getType());
          MultiSignedMessage signedMessage = (MultiSignedMessage) f.get(message);
          // TODO: Use Setter instead of directly setting the field
          f.set(message, populateMultiSignedMessage(signedMessage));
        }
      }
    } catch (IllegalAccessException e) {
      log.error("Unknown message type " + messageClass, e);
      throw new IllegalArgumentException("Unknown message type " + messageClass.getSimpleName());
    }
  }

}
