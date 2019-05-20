package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.*;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class DepositRequestCancelRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<DepositRequest> depositRequest;

    @Override
    public ProxyId address() {
        return depositRequest.getSignedBy();
    }

    @Override
    public ProxyId signer() {
        return depositRequest.getMessage().getOwnerProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId) && depositRequest != null && depositRequest.isValid();
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @JsonIgnore
    public String getDepositId() {
        return depositRequest.getMessage().getDepositId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return depositRequest.getMessage().getProxyUniverse();
    }
}
