package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Service to sign the message
 */
@Builder
public class MessageSigningService {

    @NonNull
    private MessageSerializerService serializer;

    @NonNull
    private CryptographyService cryptographyService;

    @NonNull
    @Singular
    private List<String> signatureAlgorithms;

    /**
     * Sign the message and produce signed message using the given Proxy
     *
     * @param message Message to Sign
     * @param signer  Proxy that is signing the message
     * @param <T>     Message type being signed
     * @return Signed Message
     * @throws IOException Any Signing related issues while signing
     */
    public <T extends SignableMessage> SignedMessage<T> sign(T message, Proxy signer) throws IOException, GeneralSecurityException {
        if (signatureAlgorithms.isEmpty()) {
            throw new IllegalStateException("At least one signature algorithm is required");
        }
        // TODO: Check if signer is same as message.signer
        String payload = serializer.serializeSignableMessage(message);
        SignedMessage.SignedMessageBuilder<T> builder = SignedMessage.<T>builder()
                .message(message)
                .type(message.getMessageType())
                .payload(payload);
        for (String signatureAlgorithm : signatureAlgorithms) {
            String signature = cryptographyService.getSignature(signatureAlgorithm, signer.getPrivateKey(), payload);
            builder.signature(SignedMessage.Signature.of(signatureAlgorithm, signature));
        }
        return builder.build();
    }
}

