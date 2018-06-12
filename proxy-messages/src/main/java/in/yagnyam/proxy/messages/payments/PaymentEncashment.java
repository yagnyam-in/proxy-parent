package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Payment Encashment
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentAuthorization"})
public class PaymentEncashment implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<PaymentAuthorization> paymentAuthorization;

    private Amount amount;

    private ProxyAccountId receivingProxyAccountId;

    private NonProxyAccount receivingNonProxyAccount;

    @Override
    public String signer() {
        return paymentAuthorization.getMessage().getPayeeId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && paymentAuthorization != null
                && paymentAuthorization.isValid()
                && amount != null
                && amount.isValid()
                && (receivingProxyAccountId != null && receivingProxyAccountId.isValid()
                || receivingNonProxyAccount != null && receivingNonProxyAccount.isValid());
    }

    @Override
    public String address() {
        return paymentAuthorization.getMessage().getProxyAccount().signer();
    }

    @Override
    public String requestId() {
        return requestId;
    }
}
