package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.CollectionUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateProxyAccountRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private ProxyId newOwnerProxyId;

    @NonNull
    private List<ProxyAccountPermissionEnum> permissions;

    @Override
    public ProxyId address() {
        return proxyAccount.getMessage().getBankProxyId();
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId signer() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && ProxyUtils.isValid(proxyAccount)
                && ProxyUtils.isValid(newOwnerProxyId)
                && CollectionUtils.isNotEmpty(permissions);
    }

    @JsonIgnore
    public ProxyAccountId getProxyAccountId() {
        return proxyAccount.getMessage().getProxyAccountId();
    }

}
