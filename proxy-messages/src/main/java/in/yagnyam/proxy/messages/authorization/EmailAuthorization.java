package in.yagnyam.proxy.messages.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailAuthorization implements SignableMessage, AddressableMessage {

    @NonNull
    private ProxyId authorizedProxyId;

    @NonNull
    private String email;

    @NonNull
    private ProxyId authorizerProxyId;

    @NonNull
    private Date validFrom;

    @NonNull
    private Date validTill;

    @Override
    public ProxyId address() {
        return authorizedProxyId;
    }

    @Override
    public ProxyId signer() {
        return authorizerProxyId;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(authorizedProxyId)
                && StringUtils.isValid(email)
                && ProxyUtils.isValid(authorizerProxyId)
                && DateUtils.isValid(validFrom)
                && DateUtils.isValid(validTill);
    }

}
