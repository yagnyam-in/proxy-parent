package in.yagnyam.proxy.messages.identity.aadhaar;

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
public class AadhaarVerificationChallengeResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<AadhaarVerificationChallenge> challenge;

    @NonNull
    private String response;

    @Override
    public ProxyId signer() {
        return getOwnerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(challenge) && StringUtils.isValid(response);
    }

    @Override
    public ProxyId address() {
        return getIssuerProxyId();
    }

    @JsonIgnore
    public ProxyId getOwnerProxyId() {
        return challenge.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyId getIssuerProxyId() {
        return challenge.getMessage().getIssuerProxyId();
    }

    @JsonIgnore
    public String getAadhaarNumber() {
        return challenge.getMessage().getAadhaarNumber();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return challenge.getMessage().getProxyUniverse();
    }
}
