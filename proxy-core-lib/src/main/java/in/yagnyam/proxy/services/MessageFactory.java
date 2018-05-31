package in.yagnyam.proxy.services;

import in.yagnyam.proxy.MessageSerializer;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;

@Builder
@Slf4j
public class MessageFactory {

    @NonNull
    private MessageSerializer serializer;

    @NonNull
    private CryptographyService cryptographyService;

    public SignedMessage buildSignedMessage(String signedMessage) throws IOException {
        SignedMessage signedMessageObject = serializer.deserialize(signedMessage, SignedMessage.class);
        return verifyAndBuildSignedMessage(signedMessageObject);
    }


    <T extends SignableMessage> SignedMessage<T> verifyAndBuildSignedMessage(SignedMessage<T> signedMessage) throws IOException {
        log.info("verifying signature for " + signedMessage);
        String underlyingMessageType = signedMessage.getType();
        try {
            Class messageClass = Class.forName(underlyingMessageType);
            SignableMessage underlyingMessage = buildSignableMessage(signedMessage.getPayload(), messageClass);
            return signedMessage.setMessage(underlyingMessage);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown message type " + underlyingMessageType);
        }
    }


    private <T extends SignableMessage> T buildSignableMessage(String signableMessage, Class<T> messageClass) throws IOException {
        T signableMessageObject = serializer.deserialize(signableMessage, messageClass);
        Field[] fields = signableMessageObject.getClass().getDeclaredFields();
        try {
            // See if any signed message fields are there
            for (Field f : fields) {
                if (f.getType().isAssignableFrom(SignedMessage.class)) {
                    log.info("verifying field of type " + f.getType());
                    SignedMessage signedMessage = (SignedMessage) f.get(signableMessageObject);
                    f.set(signableMessageObject, verifyAndBuildSignedMessage(signedMessage));
                }
            }
            return signableMessageObject;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unknown message type " + messageClass.getSimpleName());
        }
    }

}
