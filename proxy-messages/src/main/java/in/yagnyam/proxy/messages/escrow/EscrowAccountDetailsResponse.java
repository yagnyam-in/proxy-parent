package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
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
    }@Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request) && status != null;
    }

    public String getRequestId() {
        return request.getMessage().getRequestId();
    }
}
