package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
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
public class PaymentForPaymentRequest implements SignableRequestMessage {

    @NonNull
    private String paymentId;

    @NonNull
    public SignedMessage<PaymentRequest> paymentRequest;

    @NonNull
    public SignedMessage<ProxyAccount> payerProxyAccount;

    private String description;

    @Override
    public ProxyId signer() {
        return payerProxyAccount.getMessage().getOwnerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(paymentId)
                && ProxyUtils.isValid(paymentRequest)
                && ProxyUtils.isValid(payerProxyAccount);
        // TODO: Validate Currency as well.
    }

    @Override
    public String requestId() {
        return paymentId;
    }

    @JsonIgnore
    public ProxyId getPayeeProxyId() {
        return paymentRequest.getMessage().getPayeeProxyId();
    }

    @JsonIgnore
    public ProxyId getPayerProxyId() {
        return payerProxyAccount.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyAccountId getPayerAccountId() {
        return payerProxyAccount.getMessage().getProxyAccountId();
    }

    @JsonIgnore
    public ProxyAccountId getPayeeAccountId() {
        return paymentRequest.getMessage().getPayeeAccountId();
    }
}
