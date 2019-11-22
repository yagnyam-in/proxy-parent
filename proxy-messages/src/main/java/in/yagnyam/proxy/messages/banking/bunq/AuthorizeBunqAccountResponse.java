package in.yagnyam.proxy.messages.banking.bunq;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.utils.CollectionUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizeBunqAccountResponse implements SignableMessage, AddressableMessage {

    @NonNull
    private SignedMessage<AuthorizeBunqAccountRequest> request;

    @NonNull
    public List<SignedMessage<ProxyAccount>> proxyAccounts;

    @Override
    public ProxyId address() {
        return request.getMessage().getOwnerProxyId();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().getBankProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request)
                && CollectionUtils.isNotEmpty(proxyAccounts)
                && proxyAccounts.stream().allMatch(ProxyUtils::isValid);
    }

}
