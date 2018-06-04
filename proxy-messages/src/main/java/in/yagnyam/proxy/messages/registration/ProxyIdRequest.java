package in.yagnyam.proxy.messages.registration;

import in.yagnyam.proxy.RequestMessage;
import lombok.*;

/**
 * Request to get PID
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "requestId")
public class ProxyIdRequest implements RequestMessage {

    /**
     * Unique Request Number. No two requests shall have same request number
     */
    @NonNull
    private String requestId;

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
