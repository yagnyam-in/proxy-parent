package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
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
public class PaymentConfirmation implements SignableMessage {

    @NonNull
    public SignedMessage<PaymentRequest> paymentRequest;

    @NonNull
    private ProxyAccountId payerAccountId;

    @NonNull
    private ProxyId payerProxyId;

    @NonNull
    private String bankTransaction;

    @Override
    public ProxyId signer() {
        return payerAccountId.getBankProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(paymentRequest)
                && ProxyUtils.isValid(payerAccountId)
                && ProxyUtils.isValid(payerProxyId)
                && StringUtils.isValid(bankTransaction);
    }

    @JsonIgnore
    public ProxyId getPayeeProxyId() {
        return paymentRequest.getMessage().getPayeeProxyId();
    }

    @JsonIgnore
    public ProxyAccountId getPayeeAccountId() {
        return paymentRequest.getMessage().getPayeeAccountId();
    }

}
