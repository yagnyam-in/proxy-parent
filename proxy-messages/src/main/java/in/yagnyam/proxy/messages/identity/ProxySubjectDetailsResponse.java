package in.yagnyam.proxy.messages.identity;

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
public class ProxySubjectDetailsResponse implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<ProxySubjectDetailsRequest> request;

    @NonNull
    private SubjectDetails subjectDetails;

    @Override
    public ProxyId signer() {
        return getIdentityProviderProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(request) && ProxyUtils.isValid(subjectDetails);
    }

    @Override
    public ProxyId address() {
        return request.getSignedBy();
    }

    @JsonIgnore
    public ProxyId getOwnerProxyId() {
        return request.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyId getIdentityProviderProxyId() {
        return request.getMessage().getIdentityProviderProxyId();
    }

    @JsonIgnore
    public ProxySubjectId getProxySubjectId() {
        return request.getMessage().getProxySubjectId();
    }
}
