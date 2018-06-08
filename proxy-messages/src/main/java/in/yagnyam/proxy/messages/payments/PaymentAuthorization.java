package in.yagnyam.proxy.messages.payments;

import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

import java.util.Date;

/**
 * Payment Authorization
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentAuthorizationId", "proxyAccount"})
public class PaymentAuthorization implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    private SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private String paymentAuthorizationId;

    /**
     * Payee is mandatory (Anonymous Cheques are not supported to prevent Fraud)
     */
    @NonNull
    private String payeeId;

    @NonNull
    private Date validFrom;

    @NonNull
    private Date validTill;

    @NonNull
    private Date issueDate;

    @NonNull
    private Amount amount;

    @NonNull
    private String transactionId;

    @Override
    public String signer() {
        return proxyAccount.signer();
    }

    @Override
    public String address() {
        return payeeId;
    }

    @Override
    public String requestId() {
        return requestId;
    }
}
