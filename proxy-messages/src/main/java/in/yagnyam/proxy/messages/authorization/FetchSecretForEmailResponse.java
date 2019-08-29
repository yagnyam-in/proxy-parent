package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.*;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FetchSecretForEmailResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<FetchSecretForEmailRequest> request;

    @NonNull
    private String secret;

    @Override
    public ProxyId address() {
        return request.getMessage().getRequesterProxyId();
    }

    @Override
    public ProxyId signer() {
        return request.getMessage().emailAuthorization.getMessage().getAuthorizerProxyId();
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
