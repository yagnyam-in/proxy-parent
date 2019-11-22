package in.yagnyam.proxy.messages.geo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FindCircleResponse implements SignableMessage {

    @NonNull
    private SignedMessage<FindCircleRequest> request;

    @NonNull
    private SignedMessage<FindCircleSession> session;

    @Override
    public ProxyId signer() {
        return request.getMessage().getServiceProviderProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request)
                && ProxyUtils.isValid(session);
    }

}
