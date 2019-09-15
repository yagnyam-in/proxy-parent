package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class AccountDetailsResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<AccountDetailsRequest> request;

    @NonNull
    private NonProxyAccount underlyingAccount;

    @NonNull
    private Amount balance;

    @Override
    public ProxyId address() {
        return request.getSignedBy();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().getProxyAccount().getSignedBy();
    }@Override
    @JsonIgnore
    public boolean isValid() {
        return request != null && request.isValid()
                && underlyingAccount != null && underlyingAccount.isValid()
                && balance != null && balance.isValid();
    }

    public String getRequestId() {
        return request != null && request.getMessage() != null ? request.getMessage().getRequestId() : null;
    }
}
