package in.yagnyam.proxy.messages.xxx;

import in.yagnyam.proxy.SignableRequestMessage;
import lombok.*;


/**
 * Identity authorization message
 */
@Builder
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"requestId"})
public class IdentityAuthorization implements SignableRequestMessage {

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
