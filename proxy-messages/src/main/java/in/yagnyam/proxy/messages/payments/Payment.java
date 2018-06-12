package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.StringUtils;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payment implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private Amount amount;

    private ProxyAccountId receivingProxyAccountId;

    private NonProxyAccount receivingNonProxyAccount;

    @Override
    public String signer() {
        return proxyAccount.getMessage().getProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && proxyAccount != null && proxyAccount.isValid()
                && (receivingProxyAccountId != null && receivingProxyAccountId.isValid()
                || receivingNonProxyAccount != null && receivingNonProxyAccount.isValid());
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
