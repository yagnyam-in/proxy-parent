package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
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
    public SignedMessage<ProxyAccount> proxyAccount;

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
    public ProxyId signer() {
        return proxyAccount.signer();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return false;
    }

}
