package in.yagnyam.proxy.messages.sack;


import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateMoneySackRequest implements SignableRequestMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<ProxyAccount> debitProxyAccount;

    @NonNull
    private Amount amount;

    @Override
    public ProxyId signer() {
        return getOwnerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && ProxyUtils.isValid(debitProxyAccount)
                && ProxyUtils.isValid(amount);
    }

    @JsonIgnore
    public ProxyId getOwnerProxyId() {
        return debitProxyAccount.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyAccountId getDebitProxyAccountId() {
        return debitProxyAccount.getMessage().getProxyAccountId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return getDebitProxyAccountId().getProxyUniverse();
    }

    @Override
    public String requestId() {
        return requestId;
    }
}
