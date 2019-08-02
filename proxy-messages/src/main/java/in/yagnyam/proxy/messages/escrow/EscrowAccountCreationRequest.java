package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.*;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.utils.ProxyUtils;
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
    private EscrowAccountId escrowAccountId;

    @NonNull
    private ProxyId payeeProxyId;

    @NonNull
    private ProxyId escrowProxyId;

    @NonNull
    private SignedMessage<ProxyAccount> debitProxyAccount;

    @NonNull
    private Amount amount;

    @NonNull
    private String title;

    private String description;

    @Override
    public ProxyId signer() {
        return getPayerProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(escrowAccountId)
                && ProxyUtils.isValid(payeeProxyId)
                && ProxyUtils.isValid(payeeProxyId)
                && ProxyUtils.isValid(escrowAccountId)
                && ProxyUtils.isValid(debitProxyAccount)
                && ProxyUtils.isValid(amount)
                && StringUtils.isValid(title);
    }

    @Override
    public ProxyId address() {
        return debitProxyAccount.getSignedBy();
    }

    @JsonIgnore
    public ProxyId getBankProxyId() {
        return debitProxyAccount.getMessage().getBankProxyId();
    }

    @JsonIgnore
    public ProxyId getPayerProxyId() {
        return debitProxyAccount.getMessage().getOwnerProxyId();
    }

    @Override
    public String requestId() {
        return escrowAccountId.getAccountId();
    }
}
