package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
@ToString(of = {"type", "payload", "signatures"})
public class SignedMessage<T extends SignableMessage> {

    @Getter
    @ToString
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(staticName = "of")
    public static class Signature {

        private String algorithm;

        private String value;

        @JsonIgnore
        public boolean isValid() {
            return StringUtils.isValid(algorithm) && StringUtils.isValid(value);
        }
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

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public SignedMessage<T> setMessage(SignableMessage message) {
        this.message = (T) message;
        return this;
    }

    @JsonIgnore
    public boolean isValid() {
        return message != null
                && message.isValid()
                && StringUtils.isValid(type)
                && StringUtils.isValid(payload)
                && signatures != null
                && signatures.stream().allMatch(Signature::isValid);
    }
}
