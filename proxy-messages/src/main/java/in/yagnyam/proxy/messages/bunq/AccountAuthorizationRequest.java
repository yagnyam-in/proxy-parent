package in.yagnyam.proxy.messages.bunq;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;


/**
 * Bunq account authorization message
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class AccountAuthorizationRequest implements SignableRequestMessage {

    @NonNull
    private String requestId;

    @NonNull
    private String apiToken;

    @NonNull
    private ProxyId proxyId;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId signer() {
        return proxyId;
    }

    @Override
    public String toReadableString() {
        return String.format("With request %s, authorize account with Api Token %s to Proxy Id %s ", requestId, apiToken, proxyId);
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && StringUtils.isValid(apiToken)
                && proxyId != null && proxyId.isValid();
    }
}
