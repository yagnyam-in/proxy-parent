package in.yagnyam.proxy.messages.payments;

import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

/**
 * Payment Encashment
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentAuthorization"})
public class PaymentEncashment implements SignableMessage, AddressableMessage {

    @NonNull
    private SignedMessage<PaymentAuthorization> paymentAuthorization;

    private Amount amount;

    private ProxyAccountId receivingProxyAccountId;

    /**
     * Non proxy Account Number
     */
    private String accountNumber;

    @Override
    public String signer() {
        return paymentAuthorization.getMessage().getPayeeId();
    }

    @Override
    public String address() {
        return paymentAuthorization.getMessage().getProxyAccount().signer();
    }

}
