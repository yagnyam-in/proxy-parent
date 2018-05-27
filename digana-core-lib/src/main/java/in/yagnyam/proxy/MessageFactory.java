package in.yagnyam.proxy;

import in.yagnyam.digana.services.CryptographyService;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.BiFunction;

@Builder
@Slf4j
public class MessageFactory {

    @NonNull
    private MessageSerializer serializer;

    @NonNull
    private CryptographyService cryptographyService;

    @Singular
    private Map<String, BiFunction<String, Class<? extends SignedMessage>, SignedMessage<? extends SignableMessage>>> factoryFunctions;

    public SignedMessage<? extends SignableMessage> buildSignedMessage(String signedMessage) throws IOException {
        SignedMessage<? extends SignableMessage> signedMessageObject = (SignedMessage<? extends SignableMessage>)serializer.deserialize(signedMessage, SignedMessage.class);
        return verifySignedMessage(signedMessageObject);
    }


    SignedMessage<? extends SignableMessage> verifySignedMessage(SignedMessage<? extends SignableMessage> signedMessage) throws IOException {
        log.info("verifying signature for " + signedMessage);
        String underlyingMessageType = signedMessage.getType();
        try {
            Class<? extends SignableMessage> messageClass = (Class<? extends SignableMessage>) Class.forName(underlyingMessageType);
            SignableMessage underlyingMessage = buildSignableMessage(signedMessage.getPayload(), messageClass);
            return signedMessage.setMessage(underlyingMessage);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown message type " + underlyingMessageType);
        }
    }


    SignableMessage buildSignableMessage(String signableMessage, Class<? extends SignableMessage> messageClass) throws IOException {
        SignableMessage signableMessageObject = serializer.deserialize(signableMessage, messageClass);
        Field[] fields = signableMessageObject.getClass().getDeclaredFields();
        try {
            // See if any signed message fields are there
            for (Field f : fields) {
                if (f.getType().isAssignableFrom(SignedMessage.class)) {
                    log.info("verifying field of type " + f.getType());
                    SignedMessage signedMessage = (SignedMessage) f.get(signableMessageObject);
                    f.set(signableMessageObject, verifySignedMessage(signedMessage));
                }
            }
            return signableMessageObject;
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Unknown message type " + messageClass.getSimpleName());
        }
    }

}
