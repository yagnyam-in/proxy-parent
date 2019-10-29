package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
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
public class CreateProxySubjectRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    private String relyingPartyId;

    @NonNull
    private ProxyId relyingPartyProxyId;

    @NonNull
    public SignedMessage<SubjectIdentity> subjectIdentity;

    @NonNull
    public ProxyId newOwnerProxyId;

    @NonNull
    private SubjectDetails subjectDetails;

    @Override
    public ProxyId signer() {
        return getOriginalOwnerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && StringUtils.isValid(relyingPartyId)
                && ProxyUtils.isValid(relyingPartyProxyId)
                && ProxyUtils.isValid(subjectIdentity)
                && ProxyUtils.isValid(newOwnerProxyId)
                && ProxyUtils.isValid(subjectDetails);
    }

    @Override
    public ProxyId address() {
        return getIdentityProviderProxyId();
    }

    @JsonIgnore
    public ProxyId getOriginalOwnerProxyId() {
        return subjectIdentity.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyId getIdentityProviderProxyId() {
        return subjectIdentity.getMessage().getIdentityProviderProxyId();
    }

    @Override
    public String requestId() {
        return requestId;
    }
}
