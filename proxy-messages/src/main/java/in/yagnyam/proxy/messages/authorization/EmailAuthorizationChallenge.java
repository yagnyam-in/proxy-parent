package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.*;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailAuthorizationChallenge implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<EmailAuthorizationRequest> request;

    @NonNull
    private Hash challengeHash;

    @Override
    public ProxyId address() {
        return getRequesterProxyId();
    }

    @Override
    public ProxyId signer() {
        return getAuthorizerProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request)
                && ProxyUtils.isValid(challengeHash);
    }

    @JsonIgnore
    public ProxyId getRequesterProxyId() {
        return request.getMessage().getRequesterProxyId();
    }

    @JsonIgnore
    public ProxyId getAuthorizerProxyId() {
        return request.getMessage().getAuthorizerProxyId();
    }

}
