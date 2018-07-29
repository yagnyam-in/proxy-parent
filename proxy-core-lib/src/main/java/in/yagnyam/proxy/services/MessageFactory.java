package in.yagnyam.proxy.services;

import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;

@Builder
@SuppressWarnings("unchecked")
@Slf4j
public class MessageFactory {

    @NonNull
    private MessageSerializerService serializer;

    @NonNull
    private CryptographyService cryptographyService;

    @NonNull
    private MessageVerificationService verificationService;

    public SignedMessage buildSignedMessage(String signedMessage) throws IOException, GeneralSecurityException {
        SignedMessage signedMessageObject = serializer.deserializeSignedMessage(signedMessage);
        return verifySignedMessage(signedMessageObject);
    }


    public <T extends SignableMessage> SignedMessage<T> verifySignedMessage(SignedMessage<T> signedMessage) throws IOException, GeneralSecurityException {
        log.debug("verifying signature for " + signedMessage);
        String underlyingMessageType = signedMessage.getType();
        try {
            Class messageClass = Class.forName(underlyingMessageType);
            SignableMessage underlyingMessage = buildSignableMessage(signedMessage.getPayload(), messageClass);
            if (!underlyingMessageType.equals(underlyingMessage.getType())) {
                log.error("Message type " + underlyingMessageType + " from SignedMessage doesn't match Message type from Payload " + underlyingMessage.getType());
                throw new IllegalArgumentException("Message type " + underlyingMessageType + " from SignedMessage doesn't match Message type from Payload " + underlyingMessage.getType());
            }
            SignedMessage<T> extracted = signedMessage.setMessage(underlyingMessage);
            verificationService.verify(extracted);
            return extracted;
        } catch (ClassNotFoundException e) {
            log.error("Unknown message type " + underlyingMessageType, e);
            throw new IllegalArgumentException("Unknown message type " + underlyingMessageType);
        }
    }


    private <T extends SignableMessage> T buildSignableMessage(String signableMessage, Class<T> messageClass) throws IOException, GeneralSecurityException {
        log.debug("buildSignableMessage({}, {})", signableMessage, messageClass);
        T signableMessageObject = serializer.deserializeSignableMessage(signableMessage, messageClass);
        Field[] fields = signableMessageObject.getClass().getDeclaredFields();
        try {
            // See if any signed message fields are there
            for (Field f : fields) {
                if (f.getType().isAssignableFrom(SignedMessage.class)) {
                    log.debug("verifying field of type " + f.getType());
                    SignedMessage signedMessage = (SignedMessage) f.get(signableMessageObject);
                    f.set(signableMessageObject, verifySignedMessage(signedMessage));
                }
            }
            return signableMessageObject;
        } catch (IllegalAccessException e) {
            log.error("Unknown message type " + messageClass, e);
            throw new IllegalArgumentException("Unknown message type " + messageClass.getSimpleName());
        }
    }

}
