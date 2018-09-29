package in.yagnyam.proxy.services;

import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
@SuppressWarnings("unchecked")
public class MessageFactory {

  @NonNull
  private MessageSerializerService serializer;

  @NonNull
  private CryptographyService cryptographyService;

  @NonNull
  private MessageVerificationService verificationService;

  SignedMessage buildSignedMessage(String signedMessage)
      throws IOException, GeneralSecurityException {
    SignedMessage signedMessageObject = serializer.deserializeSignedMessage(signedMessage);
    return verifyAndPopulateSignedMessage(signedMessageObject);
  }

  public <T extends SignableMessage> SignedMessage<T> verifyAndPopulateSignedMessage(
      SignedMessage signedMessage, Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    log.debug("verifying signature for " + signedMessage);
    SignedMessage extracted = verifyAndPopulateSignedMessage(signedMessage, true);
    String underlyingMessageType = signedMessage.getType();
    if (underlyingMessageClass != null &&
        underlyingMessageClass.getSimpleName().equals(underlyingMessageType)) {
      throw new IllegalArgumentException("Message type " + underlyingMessageType
          + " from SignedMessage is not " + underlyingMessageClass);
    }
    return extracted;
  }

  public SignedMessage verifyAndPopulateSignedMessage(
      SignedMessage signedMessage) throws IOException, GeneralSecurityException {
    return verifyAndPopulateSignedMessage(signedMessage, true);
  }

  public <T extends SignableMessage> SignedMessage<T> populateSignedMessage(
      SignedMessage signedMessage, Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    log.debug("populating " + underlyingMessageClass);
    SignedMessage extracted = verifyAndPopulateSignedMessage(signedMessage, false);
    String underlyingMessageType = signedMessage.getType();
    if (underlyingMessageClass != null &&
        underlyingMessageClass.getSimpleName().equals(underlyingMessageType)) {
      throw new IllegalArgumentException("Message type " + underlyingMessageType
          + " from SignedMessage is not " + underlyingMessageClass);
    }
    return extracted;
  }

  public SignedMessage populateSignedMessage(SignedMessage signedMessage)
      throws IOException, GeneralSecurityException {
    return verifyAndPopulateSignedMessage(signedMessage, false);
  }


  private SignedMessage verifyAndPopulateSignedMessage(
      SignedMessage signedMessage, boolean verify) throws IOException, GeneralSecurityException {
    log.debug("verifying signature for " + signedMessage);
    String underlyingMessageType = signedMessage.getType();
    try {
      Class messageClass = Class.forName(underlyingMessageType);
      SignableMessage underlyingMessage = buildSignableMessage(signedMessage.getPayload(),
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
      SignedMessage extracted = signedMessage.setMessage(underlyingMessage);
      if (verify) {
        verificationService.verify(extracted);
      }
      return extracted;
    } catch (ClassNotFoundException e) {
      log.error("Unknown message type " + underlyingMessageType, e);
      throw new IllegalArgumentException("Unknown message type " + underlyingMessageType);
    }
  }


  private <T extends SignableMessage> T buildSignableMessage(String signableMessage,
      Class<T> messageClass, boolean verify) throws IOException, GeneralSecurityException {
    log.debug("buildSignableMessage({}, {})", signableMessage, messageClass);
    T signableMessageObject = serializer.deserializeSignableMessage(signableMessage, messageClass);
    Field[] fields = signableMessageObject.getClass().getDeclaredFields();
    try {
      // See if any signed message fields are there
      for (Field f : fields) {
        if (f.getType().isAssignableFrom(SignedMessage.class)) {
          log.debug("verifying field of type " + f.getType());
          SignedMessage signedMessage = (SignedMessage) f.get(signableMessageObject);
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
