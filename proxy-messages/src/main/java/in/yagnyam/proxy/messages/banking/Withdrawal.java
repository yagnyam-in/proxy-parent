package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Withdraw money from Proxy Account
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"withdrawalId"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Withdrawal implements SignableRequestMessage, AddressableMessage {

    @NonNull
    public SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private String withdrawalId;

    @NonNull
    private Amount amount;

    @NonNull
    private NonProxyAccount destinationAccount;

    @Override
    public ProxyId signer() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @Override
    public ProxyId address() {
        return proxyAccount.getSignedBy();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        String currency = getCurrency();
        return StringUtils.isValid(withdrawalId)
                && proxyAccount != null && proxyAccount.isValid()
                && amount != null && amount.isValid()
                && amount.getCurrency().equals(currency)
                && destinationAccount != null && destinationAccount.isValid()
                && destinationAccount.getCurrency().equals(currency);
    }

    @Override
    public String requestId() {
        return withdrawalId;
    }

    @JsonIgnore
    public ProxyAccountId getProxyAccountId() {
        return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getProxyAccountId() : null;
    }

    @JsonIgnore
    public ProxyId getOwnerProxyId() {
        return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getOwnerProxyId() : null;
    }

    @JsonIgnore
    public String getCurrency() {
        return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getCurrency() : null;
    }

}
