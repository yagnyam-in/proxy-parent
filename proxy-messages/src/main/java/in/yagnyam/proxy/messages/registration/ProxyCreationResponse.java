package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.CertificateChain;
import in.yagnyam.proxy.Constants;
import in.yagnyam.proxy.SignableMessage;
import lombok.*;

import java.util.Map;

/**
 * Response for Proxy Creation
 */
@Getter
@ToString
@EqualsAndHashCode(of = "requestNumber", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ProxyCreationResponse implements SignableMessage {

    @NonNull
    private String requestNumber;

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
}
