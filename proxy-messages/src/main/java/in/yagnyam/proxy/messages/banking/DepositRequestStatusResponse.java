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
public class DepositRequestStatusResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<DepositRequestStatusRequest> request;

    @NonNull
    private DepositStatusEnum status;

    @Override
    public ProxyId address() {
        return request.getSignedBy();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().address();
    }

    @JsonIgnore
    public String getRequestId() {
        return request.getMessage().getRequestId();
    }@Override
    @JsonIgnore
    public boolean isValid() {
        return request != null && request.isValid()
                && status != null;
    }

    @JsonIgnore
    public String getDepositId() {
        return request.getMessage().getDepositId();
    }

}
