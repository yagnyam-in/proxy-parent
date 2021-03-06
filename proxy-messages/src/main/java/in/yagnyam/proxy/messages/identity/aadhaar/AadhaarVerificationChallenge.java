package in.yagnyam.proxy.messages.identity.aadhaar;

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
public class AadhaarVerificationChallenge implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<AadhaarVerificationRequest> request;

    @NonNull
    private Hash challengeHash;

    @Override
    public ProxyId signer() {
        return request.getMessage().getIdentityProviderProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request) && ProxyUtils.isValid(challengeHash);
    }

    @Override
    public ProxyId address() {
        return getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyId getOwnerProxyId() {
        return request.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyId getIdentityProviderProxyId() {
        return request.getMessage().getIdentityProviderProxyId();
    }

    @JsonIgnore
    public String getAadhaarNumber() {
        return request.getMessage().getAadhaarNumber();
    }
}
