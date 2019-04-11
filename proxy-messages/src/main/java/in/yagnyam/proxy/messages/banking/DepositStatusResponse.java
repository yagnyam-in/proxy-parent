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
public class DepositStatusResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<DepositStatusRequest> request;

    @NonNull
    private DepositStatusEnum status;

    @Override
    public ProxyId address() {
        return request.getSignedBy();
    }

    @Override
    public ProxyId signer() {
        DepositRequest deposit = request.getMessage().getRequest().getMessage();
        return deposit.address();
    }

    @JsonIgnore
    public String getRequestId() {
        return request != null && request.getMessage() != null ? request.getMessage().getRequestId()
                : null;
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return request != null && request.isValid()
                && status != null;
    }
}
