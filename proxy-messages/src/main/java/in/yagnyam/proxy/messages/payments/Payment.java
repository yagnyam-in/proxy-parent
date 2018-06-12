package in.yagnyam.proxy.messages.payments;

import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

/**
 * Direct Payment to the payee
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class Payment implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<ProxyAccount> proxyAccount;

    private Amount amount;

    private ProxyAccountId receivingProxyAccountId;

    private NonProxyAccount receivingNonProxyAccount;

    @Override
    public String signer() {
        return proxyAccount.getMessage().getProxyId();
    }

    @Override
    public String address() {
        return proxyAccount.signer();
    }

    @Override
    public String requestId() {
        return requestId;
    }
}
