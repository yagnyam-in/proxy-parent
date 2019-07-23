package in.yagnyam.proxy.services;

import in.yagnyam.proxy.MultiSignableMessage;
import in.yagnyam.proxy.MultiSignedMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;

@Builder
@Slf4j
@SuppressWarnings("unchecked")
public class MultiSignedMessageFactory {

  @NonNull
  private MessageSerializerService serializer;

  @NonNull
  private CryptographyService cryptographyService;

  @NonNull
  private MessageVerificationService verificationService;

  MultiSignedMessage buildAndVerifySignedMessage(String signedMessage)
      throws IOException, GeneralSecurityException {
    MultiSignedMessage signedMessageObject = serializer.deserializeMultiSignedMessage(signedMessage);
    return verifyAndPopulateSignedMessage(signedMessageObject);
  }

  public <T extends MultiSignableMessage> MultiSignedMessage<T> buildSignedMessage(
      String signedMessage, Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    MultiSignedMessage signedMessageObject = serializer.deserializeMultiSignedMessage(signedMessage);
    return populateSignedMessage(signedMessageObject, underlyingMessageClass);
  }


  public <T extends MultiSignableMessage> MultiSignedMessage<T> verifyAndPopulateSignedMessage(
      MultiSignedMessage signedMessage, Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    log.debug("verifying signature for " + signedMessage);
    MultiSignedMessage extracted = verifyAndPopulateSignedMessage(signedMessage, true);
    String underlyingMessageType = signedMessage.getType();
    if (underlyingMessageClass != null &&
        underlyingMessageClass.getSimpleName().equals(underlyingMessageType)) {
      throw new IllegalArgumentException("Message type " + underlyingMessageType
          + " from SignedMessage is not " + underlyingMessageClass);
    }
    return extracted;
  }

  public MultiSignedMessage verifyAndPopulateSignedMessage(
      MultiSignedMessage signedMessage) throws IOException, GeneralSecurityException {
    return verifyAndPopulateSignedMessage(signedMessage, true);
  }

  public <T extends MultiSignableMessage> MultiSignedMessage<T> populateSignedMessage(
      MultiSignedMessage signedMessage, Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    log.debug("populating " + underlyingMessageClass);
    MultiSignedMessage extracted = verifyAndPopulateSignedMessage(signedMessage, false);
    String underlyingMessageType = signedMessage.getType();
    if (underlyingMessageClass != null &&
        underlyingMessageClass.getSimpleName().equals(underlyingMessageType)) {
      throw new IllegalArgumentException("Message type " + underlyingMessageType
          + " from SignedMessage is not " + underlyingMessageClass);
    }
    return extracted;
  }

  public MultiSignedMessage populateSignedMessage(MultiSignedMessage signedMessage)
      throws IOException, GeneralSecurityException {
    return verifyAndPopulateSignedMessage(signedMessage, false);
  }


  private MultiSignedMessage verifyAndPopulateSignedMessage(
          MultiSignedMessage signedMessage, boolean verify) throws IOException, GeneralSecurityException {
    log.debug("verifying signature for " + signedMessage);
    String underlyingMessageType = signedMessage.getType();
    try {
      Class messageClass = Class.forName(underlyingMessageType);
      MultiSignableMessage underlyingMessage = buildSignableMessage(signedMessage.getPayload(),
          messageClass, verify);
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
      MultiSignedMessage extracted = signedMessage.setMessage(underlyingMessage);
      if (verify) {
        verificationService.isValid(extracted);
      }
      return extracted;
    } catch (ClassNotFoundException e) {
      log.error("Unknown message type " + underlyingMessageType, e);
      throw new IllegalArgumentException("Unknown message type " + underlyingMessageType);
    }
  }


  private <T extends MultiSignableMessage> T buildSignableMessage(String signableMessage,
      Class<T> messageClass, boolean verify) throws IOException, GeneralSecurityException {
    log.debug("buildSignableMessage({}, {})", signableMessage, messageClass);
    T signableMessageObject = serializer.deserializeMultiSignableMessage(signableMessage, messageClass);
    Field[] fields = signableMessageObject.getClass().getDeclaredFields();
    try {
      // See if any signed message fields are there
      for (Field f : fields) {
        if (f.getType().isAssignableFrom(SignedMessage.class)) {
          log.debug("verifying field of type " + f.getType());
          MultiSignedMessage signedMessage = (MultiSignedMessage) f.get(signableMessageObject);
          // TODO: Use Setter instead of directly setting the field
          f.set(signableMessageObject, verifyAndPopulateSignedMessage(signedMessage, verify));
        }
      }
      return signableMessageObject;
    } catch (IllegalAccessException e) {
      log.error("Unknown message type " + messageClass, e);
      throw new IllegalArgumentException("Unknown message type " + messageClass.getSimpleName());
    }
  }

}
