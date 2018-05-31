package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.List;

@Builder
@Getter
@ToString(of = {"type", "payload", "signatures"})
public class SignedMessage<T extends SignableMessage> {

    @Getter
    @ToString
    @AllArgsConstructor(staticName = "of")
    public static class Signature {

        private String algorithm;

        private String value;
    }

    @JsonIgnore
    private T message;

    @NonNull
    private String type;

    @NonNull
    private String payload;

    @NonNull
    @Singular
    private List<Signature> signatures;

    public String signer() {
        return message.signer();
    }

    public SignedMessage<T> setMessage(SignableMessage message) {
        this.message = (T)message;
        return this;
    }
}
