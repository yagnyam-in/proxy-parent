package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class DepositRequestStatusRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<DepositRequest> depositRequest;

    @Override
    public ProxyId address() {
        return depositRequest.getMessage().signer();
    }

    @Override
    public ProxyId signer() {
        return depositRequest.getSignedBy();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return depositRequest != null && depositRequest.isValid();
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
    public ProxyAccountId getProxyAccountId() {
        return depositRequest.getMessage().getProxyAccountId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return getProxyAccountId().getProxyUniverse();
    }
}
