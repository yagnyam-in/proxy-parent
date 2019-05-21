package in.yagnyam.proxy.messages.payments.legacy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.NonProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * PaymentAuthorization Authorization Encashment
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentAuthorization"})
public class LegacyPaymentAuthorizationEncashment implements SignableRequestMessage, AddressableMessage {

    @NonNull
    public SignedMessage<LegacyPaymentAuthorization> paymentAuthorization;

    @NonNull
    private String requestId;

    @NonNull
    private Amount amount;

    private ProxyAccountId receivingProxyAccountId;

    private NonProxyAccount receivingNonProxyAccount;

    @Override
    public ProxyId signer() {
        return paymentAuthorization.getMessage().getPayeeId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        String currency = getCurrency();
        return StringUtils.isValid(requestId)
                && paymentAuthorization != null && paymentAuthorization.isValid()
                && amount != null && amount.isValid()
                && amount.getCurrency().equals(currency)
                && (receivingProxyAccountId != null && receivingProxyAccountId.isValid()
                || receivingNonProxyAccount != null && receivingNonProxyAccount.isValid() && receivingNonProxyAccount.getCurrency().equals(currency));
    }

    @Override
    public ProxyId address() {
        return paymentAuthorization.getMessage().getProxyAccount().getSignedBy();
    }

    @Override
    public String requestId() {
        return requestId;
    }


    @JsonIgnore
    public ProxyAccountId getPayerAccountId() {
        return paymentAuthorization != null && paymentAuthorization.getMessage() != null ? paymentAuthorization.getMessage().getPayerAccountId() : null;
    }

    @JsonIgnore
    public String getCurrency() {
        return paymentAuthorization != null && paymentAuthorization.getMessage() != null ? paymentAuthorization.getMessage().getCurrency() : null;
    }
}
