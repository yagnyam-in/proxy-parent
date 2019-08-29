package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
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
public class SendSecretsResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<SendSecretsRequest> request;

    @Override
    public ProxyId address() {
        return request.getMessage().getSenderProxyId();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().getRouterProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request);
    }

}
