package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ProxySubject implements SignableMessage {

    @NonNull
    private String relyingPartyId;

    @NonNull
    private ProxySubjectId proxySubjectId;

    @NonNull
    private ProxyId ownerProxyId;

    @NonNull
    private ProxyId relyingPartyProxyId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @Override
    public ProxyId signer() {
        return proxySubjectId.getIdentityProviderProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(relyingPartyId)
                && ProxyUtils.isValid(proxySubjectId)
                && ProxyUtils.isValid(ownerProxyId)
                && ProxyUtils.isValid(relyingPartyProxyId)
                && DateUtils.isValid(creationDate)
                && DateUtils.isValid(expiryDate);
    }

    @JsonIgnore
    public ProxyId getIdentityProviderProxyId() {
        return proxySubjectId.getIdentityProviderProxyId();
    }
}
