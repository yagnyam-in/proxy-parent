package in.yagnyam.proxy.messages.payments.offline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Map;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BuyOfflineMoneyRequest implements SignableRequestMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private Map<Integer, Integer> denominations;

    @Override
    public ProxyId signer() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && ProxyUtils.isValid(proxyAccount)
                && denominations != null && !denominations.isEmpty();
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @JsonIgnore
    public ProxyId getPayerProxyId() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyAccountId getPayerAccountId() {
        return proxyAccount.getMessage().getProxyAccountId();
    }

    @JsonIgnore
    public String getCurrency() {
        return proxyAccount.getMessage().getCurrency();
    }

    @JsonIgnore
    public ProxyId getPayerBankProxyId() {
        return proxyAccount.getSignedBy();
    }

}
