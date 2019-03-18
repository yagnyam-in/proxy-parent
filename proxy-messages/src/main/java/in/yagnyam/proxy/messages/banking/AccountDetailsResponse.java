package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class AccountDetailsResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<AccountDetailsRequest> request;

    @NonNull
    private String accountNumber;

    @Override
    public ProxyId address() {
        return request.getSignedBy();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().getProxyAccount().getSignedBy();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return request != null && request.isValid()
                && StringUtils.isValid(accountNumber);
    }

    public String getRequestId() {
        return request != null && request.getMessage() != null ? request.getMessage().getRequestId() : null;
    }
}
