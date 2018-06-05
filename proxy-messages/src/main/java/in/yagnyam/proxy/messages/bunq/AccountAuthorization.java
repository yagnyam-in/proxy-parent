package in.yagnyam.proxy.messages.bunq;

import in.yagnyam.proxy.SignableRequestMessage;
import lombok.*;


/**
 * Bunq account authorization message
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class AccountAuthorization implements SignableRequestMessage {

    @NonNull
    private String requestId;

    @NonNull
    private String apiToken;

    @NonNull
    private String proxyId;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public String signer() {
        return proxyId;
    }
}
