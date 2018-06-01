package in.yagnyam.proxy.messages.payments;

import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

import java.util.Date;

/**
 * Payment Authorization that is protected by a Secret
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentAuthorizationId", "proxyAccount"})
public class SecretProtectedPaymentAuthorization implements SignableMessage {

    @NonNull
    private SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private String paymentAuthorizationId;

    @NonNull
    private Date validFrom;

    @NonNull
    private Date validTill;

    @NonNull
    private Date issueDate;

    @NonNull
    private Amount amount;

    @NonNull
    private String ivPrefixedSecretHash;

    @Override
    public String signer() {
        return proxyAccount.signer();
    }

}
