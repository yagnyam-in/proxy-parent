package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;


@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountCreationRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    private String proxyUniverse;

    @NonNull
    private ProxyId buyerProxyId;

    @NonNull
    private ProxyId sellerProxyId;

    @NonNull
    private ProxyId escrowProxyId;

    @NonNull
    private SignedMessage<ProxyAccount> debitProxyAccount;

    @NonNull
    private Amount amount;

    @NonNull
    private String title;

    @NonNull
    private String description;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId signer() {
        return buyerProxyId;
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && StringUtils.isValid(proxyUniverse)
                && buyerProxyId != null && buyerProxyId.isValid()
                && sellerProxyId != null && sellerProxyId.isValid()
                && escrowProxyId != null && escrowProxyId.isValid()
                && debitProxyAccount != null && debitProxyAccount.isValid()
                && amount != null && amount.isValid()
                && StringUtils.isValid(title)
                && StringUtils.isValid(description);
    }

    @Override
    public ProxyId address() {
        return debitProxyAccount.getSignedBy();
    }

    @JsonIgnore
    ProxyId bankProxyId() {
        return debitProxyAccount.getSignedBy();
    }
}
