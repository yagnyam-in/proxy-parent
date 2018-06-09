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

    private boolean revealNationality;

    private boolean revealName;

    private boolean revealAge;

    private boolean revealIs18Plus;

    private boolean revealDateOfBirth;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public String signer() {
        return proxyId;
    }
}
