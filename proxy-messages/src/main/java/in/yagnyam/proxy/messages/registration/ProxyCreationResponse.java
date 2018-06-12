package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.CertificateChain;
import in.yagnyam.proxy.Constants;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Map;

/**
 * Response for Proxy Creation
 */
@Getter
@ToString
@EqualsAndHashCode(of = "requestId", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ProxyCreationResponse implements SignableMessage {

    @NonNull
    private String requestId;

    @NonNull
    private String proxyId;

    @NonNull
    private String certificateSerial;

    @NonNull
    private CertificateChain certificateChain;

    @Override
    public String signer() {
        return Constants.PROXY_CENTRAL;
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId)
                && StringUtils.isValid(proxyId)
                && StringUtils.isValid(certificateSerial)
                && certificateChain != null
                && certificateChain.isValid();
    }

}
