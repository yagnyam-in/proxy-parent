package in.yagnyam.proxy;

import in.yagnyam.proxy.services.CryptographyService;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Builder
@Slf4j
public class MessageVerificationService {

    @NonNull
    private MessageSerializer serializer;

    @NonNull
    private CryptographyService cryptographyService;

    @NonNull
    @Singular
    private List<String> signatureAlgorithms;

    public <T extends SignableMessage> SignedMessage<T> verify(SignedMessage<T> signedMessage, Class<T> tClass) throws IOException {
        String payload = signedMessage.getPayload();
        T message = serializer.deserialize(signedMessage.getPayload(), tClass);
        return SignedMessage.<T>builder()
                .message(message)
                .payload(payload)
                .signatures(signedMessage.getSignatures())
                .build();
    }
}
