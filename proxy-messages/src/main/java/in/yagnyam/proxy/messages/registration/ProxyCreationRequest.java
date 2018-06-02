package in.yagnyam.proxy.messages.registration;

import lombok.*;

/**
 * Request to create new Proxy Certificate
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "requestId")
public class ProxyCreationRequest {

    /**
     * Unique Request Id. No two requests shall have same request number
     */
    @NonNull
    private String requestId;

    /**
     * Unique Proxy Id
     */
    @NonNull
    private String proxyId;

    /**
     * Valid Certificate Request for Subject requestId.
     * This is to prevent misusing un-protected endpoint to get new PID
     */
    @NonNull
    private String certificationRequestEncoded;

}
