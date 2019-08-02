package in.yagnyam.proxy.services;

import in.yagnyam.proxy.MultiSignableMessage;
import in.yagnyam.proxy.MultiSignedMessage;
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
  private MessageBuilder messageBuilder;

  @NonNull
  private MessageVerificationService verificationService;

  public <T extends SignableMessage> SignedMessage<T> buildSignedMessage(String signedMessage,
      Class<T> underlyingMessageClass)
      throws IOException {
    return messageBuilder.buildSignedMessage(signedMessage, underlyingMessageClass);
  }


  public <T extends MultiSignableMessage> MultiSignedMessage<T> buildMultiSignedMessage(String signedMessage,
      Class<T> underlyingMessageClass)
      throws IOException {
    return messageBuilder.buildMultiSignedMessage(signedMessage, underlyingMessageClass);
  }

  public <T extends SignableMessage> SignedMessage<T> buildAndVerifySignedMessage(String signedMessage,
      Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    SignedMessage<T> signedMessageObject = buildSignedMessage(signedMessage, underlyingMessageClass);
    verifySignedMessage(signedMessageObject);
    return signedMessageObject;
  }

  public <T extends MultiSignableMessage> MultiSignedMessage<T> buildAndVerifyMultiSignedMessage(String signedMessage,
      Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    MultiSignedMessage<T> signedMessageObject = buildMultiSignedMessage(signedMessage, underlyingMessageClass);
    verifyMultiSignedMessage(signedMessageObject);
    return signedMessageObject;
  }


  public <T extends SignableMessage> SignedMessage<T> populateAndVerifySignedMessage(SignedMessage signedMessage,
      Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    SignedMessage<T> signedMessageObject = messageBuilder.populateSignedMessage(signedMessage, underlyingMessageClass);
    verifySignedMessage(signedMessageObject);
    return signedMessageObject;
  }

  public <T extends MultiSignableMessage> MultiSignedMessage<T> populateAndVerifyMultiSignedMessage(
      MultiSignedMessage signedMessage,
      Class<T> underlyingMessageClass)
      throws IOException, GeneralSecurityException {
    MultiSignedMessage<T> signedMessageObject = messageBuilder
        .populateMultiSignedMessage(signedMessage, underlyingMessageClass);
    verifyMultiSignedMessage(signedMessageObject);
    return signedMessageObject;
  }


  <T extends SignableMessage> void verifySignedMessage(@NonNull SignedMessage<T> signedMessage)
      throws GeneralSecurityException {
    verificationService.verify(signedMessage);
    verifyChildMessages(signedMessage.getMessage());
  }


  <T extends MultiSignableMessage> void verifyMultiSignedMessage(@NonNull MultiSignedMessage<T> signedMessage)
      throws GeneralSecurityException {
    verificationService.verify(signedMessage);
    verifyChildMessages(signedMessage.getMessage());
  }

  private <T> void verifyChildMessages(@NonNull T message) throws GeneralSecurityException {
    Class messageClass = message.getClass();
    Field[] fields = messageClass.getDeclaredFields();
    try {
      // See if any signed message fields are there
      for (Field f : fields) {
        if (f.getType().isAssignableFrom(SignedMessage.class)) {
          log.debug("verifying field of type " + f.getType());
          SignedMessage childSignedMessage = (SignedMessage) f.get(message);
          verifySignedMessage(childSignedMessage);
        }
        if (f.getType().isAssignableFrom(MultiSignedMessage.class)) {
          log.debug("verifying field of type " + f.getType());
          MultiSignedMessage childMultiSignedMessage = (MultiSignedMessage) f.get(message);
          verifyMultiSignedMessage(childMultiSignedMessage);
        }
      }
    } catch (IllegalAccessException e) {
      log.error("Failed to verify message of type " + messageClass, e);
      throw new IllegalArgumentException("Failed to verify message type " + messageClass.getSimpleName());
    }

  }


}
