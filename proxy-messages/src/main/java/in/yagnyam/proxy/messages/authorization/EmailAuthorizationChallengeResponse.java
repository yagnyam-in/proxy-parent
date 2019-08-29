package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailAuthorizationChallengeResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<EmailAuthorizationChallenge> challenge;

    @NonNull
    private String response;

    @Override
    public ProxyId address() {
        return getAuthorizerProxyId();
    }

    @Override
    public ProxyId signer() {
        return getRequesterProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(challenge)
                && StringUtils.isValid(response);
    }

    @JsonIgnore
    public ProxyId getRequesterProxyId() {
        return challenge.getMessage().getRequesterProxyId();
    }

    @JsonIgnore
    public ProxyId getAuthorizerProxyId() {
        return challenge.getMessage().getAuthorizerProxyId();
    }


}
