package in.yagnyam.proxy.payments;

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

    @NonNull
    private AccountId receivingAccountId;

    @Override
    public String signer() {
        return paymentAuthorization.getMessage().getPayeeId();
    }

    @Override
    public String address() {
        return paymentAuthorization.getMessage().getProxyAccount().signer();
    }

}
