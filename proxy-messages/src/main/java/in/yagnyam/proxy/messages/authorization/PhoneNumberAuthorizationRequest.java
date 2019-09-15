package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneNumberAuthorizationRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String authorizationId;

    @NonNull
    private ProxyId requesterProxyId;

    @NonNull
    private String phoneNumber;

    @NonNull
    private ProxyId authorizerProxyId;

    @Override
    public ProxyId address() {
        return authorizerProxyId;
    }

    @Override
    public String requestId() {
        return authorizationId;
    }

    @Override
    public ProxyId signer() {
        return requesterProxyId;
    }@Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(authorizationId)
                && ProxyUtils.isValid(requesterProxyId)
                && StringUtils.isValid(phoneNumber)
                && ProxyUtils.isValid(authorizerProxyId)
                ;
    }

}
