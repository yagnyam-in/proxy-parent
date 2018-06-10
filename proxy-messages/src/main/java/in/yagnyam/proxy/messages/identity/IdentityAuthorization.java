package in.yagnyam.proxy.messages.identity;

import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
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
    public String signer() {
        return proxyIdentity.getMessage().getProxyId();
    }
}
