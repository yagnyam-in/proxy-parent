package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Builder
@Slf4j
public class MessageSignerService {

    @NonNull
    private MessageSerializerService serializer;

    @NonNull
    private CryptographyService cryptographyService;

    @NonNull
    @Singular
    private List<String> signatureAlgorithms;

    public <T extends SignableMessage> SignedMessage<T> sign(T message, Proxy signer) throws IOException {
        if (signatureAlgorithms.isEmpty()) {
            throw new IllegalStateException("At least one signature algorithm is required");
        }
        // TODO: Check if signer is same as message.signer
        String payload = serializer.serialize(message);
        byte[] payloadBytes = payload.getBytes();
        SignedMessage.SignedMessageBuilder<T> builder = SignedMessage.<T>builder()
                .message(message)
                .type(message.getClass().getName())
                .payload(payload);
        signatureAlgorithms.forEach((a) -> builder.signature(SignedMessage.Signature.of(a, cryptographyService.getSignature(payloadBytes, a, signer.getPrivateKey()))));
        return builder.build();
    }
}
