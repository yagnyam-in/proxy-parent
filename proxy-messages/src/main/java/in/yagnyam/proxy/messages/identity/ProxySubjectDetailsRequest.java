package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxySubjectDetailsRequest implements SignableMessage, AddressableMessage {

    @NonNull
    public SignedMessage<ProxySubject> proxySubject;

    @Override
    public ProxyId signer() {
        return getOwnerProxyId();
    }

    @Override
    public Set<ProxyId> validSigners() {
        return new HashSet<>(Arrays.asList(getOwnerProxyId(), getRelyingPartyProxyId()));
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(proxySubject);
    }

    @Override
    public ProxyId address() {
        return getIdentityProviderProxyId();
    }

    @JsonIgnore
    public ProxyId getRelyingPartyProxyId() {
        return proxySubject.getMessage().getRelyingPartyProxyId();
    }

    @JsonIgnore
    public ProxyId getOwnerProxyId() {
        return proxySubject.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyId getIdentityProviderProxyId() {
        return proxySubject.getMessage().getIdentityProviderProxyId();
    }

    @JsonIgnore
    public ProxySubjectId getProxySubjectId() {
        return proxySubject.getMessage().getProxySubjectId();
    }
}
