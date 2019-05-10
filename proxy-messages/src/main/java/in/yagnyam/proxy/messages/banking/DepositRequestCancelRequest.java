package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.*;
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
    public SignedMessage<DepositRequest> request;

    @Override
    public ProxyId address() {
        return request.getSignedBy();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().getOwnerProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return request != null && request.isValid();
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @JsonIgnore
    public String getDepositId() {
        return request.getMessage().getDepositId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return request.getMessage().getProxyUniverse();
    }
}
