package in.yagnyam.proxy.messages.registration;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.RequestMessage;
import lombok.*;

/**
 * Request to create new Proxy Certificate
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "certificateRequestEncoded")
@EqualsAndHashCode(of = "requestId")
public class ProxyCreationRequest implements RequestMessage {

    /**
     * Unique Request Id. No two requests shall have same request number
     */
    @NonNull
    private String requestId;

    /**
     * Unique Proxy Id
     */
    @NonNull
    private ProxyId proxyId;

    /**
     * Valid Certificate Request for Subject requestId.
     * This is to prevent misusing un-protected endpoint to get new PID
     */
    @NonNull
    private String certificateRequestEncoded;

    @Override
    public String requestId() {
        return requestId;
    }
}
