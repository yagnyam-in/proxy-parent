package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"proxyIdentity", "requestId"})
public class IdentityAuthorization implements SignableRequestMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<ProxyIdentity> proxyIdentity;

    private boolean consentToMail;

    private boolean consentToRequestPayments;

    private boolean consentToVoiceCall;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId signer() {
        return proxyIdentity.getMessage().getProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && proxyIdentity != null
                && proxyIdentity.isValid();
    }
}
