package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FetchSecretForPhoneNumberResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<FetchSecretForPhoneNumberRequest> request;

    @NonNull
    private String secret;

    @Override
    public ProxyId address() {
        return request.getMessage().getRequesterProxyId();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().phoneNumberAuthorization.getMessage().getAuthorizerProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request)
                && StringUtils.isValid(secret);
    }

}
