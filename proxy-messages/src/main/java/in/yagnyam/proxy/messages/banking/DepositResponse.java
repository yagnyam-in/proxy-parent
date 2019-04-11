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
public class DepositResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<DepositRequest> request;

    @NonNull
    private String depositLink;

    @NonNull
    private DepositStatusEnum status;

    @Override
    public ProxyId address() {
        return request.getMessage().getOwnerProxyId();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().proxyAccount.getSignedBy();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return request != null && request.isValid()
                && StringUtils.isValid(depositLink)
                && status != null;
    }
}
