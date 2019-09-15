package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.*;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FetchSecretForEmailRequest implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<EmailAuthorization> emailAuthorization;

    @NonNull
    private Hash secretHash;

    @Override
    public ProxyId address() {
        return emailAuthorization.getMessage().getAuthorizerProxyId();
    }

    @Override
    public ProxyId signer() {
        return emailAuthorization.getMessage().getAuthorizedProxyId();
    }@Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(emailAuthorization)
                && ProxyUtils.isValid(secretHash);
    }

    public ProxyId getRequesterProxyId() {
        return emailAuthorization.getMessage().getAuthorizedProxyId();
    }
}
