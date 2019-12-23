package in.yagnyam.proxy.messages.payments;

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
public class PaymentRequest implements SignableRequestMessage {

    @NonNull
    private String paymentRequestId;

    @NonNull
    public SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private Amount amount;

    private String description;

    private String transaction;

    @Override
    public ProxyId signer() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(paymentRequestId)
                && ProxyUtils.isValid(proxyAccount)
                && ProxyUtils.isValid(amount);
    }

    @Override
    public String requestId() {
        return paymentRequestId;
    }

    @JsonIgnore
    public ProxyId getPayeeProxyId() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyAccountId getPayeeAccountId() {
        return proxyAccount.getMessage().getProxyAccountId();
    }

    @JsonIgnore
    public String getCurrency() {
        return proxyAccount.getMessage().getCurrency();
    }

}
