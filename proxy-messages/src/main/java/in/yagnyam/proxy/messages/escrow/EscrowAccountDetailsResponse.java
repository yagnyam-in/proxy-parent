package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountDetailsResponse implements SignableMessage {

    @NonNull
    public SignedMessage<EscrowAccountDetailsRequest> request;

    @NonNull
    private EscrowAccountStatusEnum status;

    @Override
    public ProxyId signer() {
        return request.getMessage().getEscrowAccount().getSignedBy();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return request != null && request.isValid() && status != null;
    }

    public String getRequestId() {
        return request != null && request.getMessage() != null ? request.getMessage().getRequestId() : null;
    }
}
