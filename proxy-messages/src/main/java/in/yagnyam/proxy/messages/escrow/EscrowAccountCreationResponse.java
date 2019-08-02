package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountCreationResponse implements SignableMessage {

    @NonNull
    private SignedMessage<EscrowAccountCreationRequest> request;

    @NonNull
    public SignedMessage<EscrowAccount> escrowAccount;

    @Override
    public ProxyId signer() {
        return request.getMessage().getBankProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request) && ProxyUtils.isValid(escrowAccount)
                && request.getMessage().getEscrowAccountId().equals(escrowAccount.getMessage().getEscrowAccountId());
        // TODO: Add additional field checks.
    }

}
