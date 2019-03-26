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
public class WithdrawalStatusRequest implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<Withdrawal> request;

    @Override
    public ProxyId address() {
        return request.getMessage().address();
    }

    @Override
    public ProxyId signer() {
        return request.getSignedBy();
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
}
