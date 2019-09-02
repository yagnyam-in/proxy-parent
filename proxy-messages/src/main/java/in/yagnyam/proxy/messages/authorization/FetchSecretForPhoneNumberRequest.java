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
public class FetchSecretForPhoneNumberRequest implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<PhoneNumberAuthorization> phoneNumberAuthorization;

    @NonNull
    private Hash secretHash;

    @Override
    public ProxyId address() {
        return phoneNumberAuthorization.getMessage().getAuthorizerProxyId();
    }

    @Override
    public ProxyId signer() {
        return phoneNumberAuthorization.getMessage().getAuthorizedProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(phoneNumberAuthorization)
                && ProxyUtils.isValid(secretHash);
    }

    public ProxyId getRequesterProxyId() {
        return phoneNumberAuthorization.getMessage().getAuthorizedProxyId();
    }
}
