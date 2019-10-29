package in.yagnyam.proxy.messages.identity.aadhaar;

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
public class AadhaarVerificationRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    private ProxyId ownerProxyId;

    @NonNull
    private String aadhaarNumber;

    @NonNull
    private ProxyId identityProviderProxyId;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId signer() {
        return ownerProxyId;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && ProxyUtils.isValid(ownerProxyId)
                && ProxyUtils.isValid(identityProviderProxyId)
                && StringUtils.isValid(aadhaarNumber);
    }

    @Override
    public ProxyId address() {
        return identityProviderProxyId;
    }
}
