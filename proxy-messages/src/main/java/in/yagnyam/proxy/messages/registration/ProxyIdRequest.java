package in.yagnyam.proxy.messages.registration;

import lombok.*;

/**
 * Request to get PID
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "requestNumber")
public class ProxyIdRequest {

    /**
     * Unique Request Number. No two requests shall have same request number
     */
    @NonNull
    private String requestNumber;

    /**
     * Valid Certificate Request for Subject requestNumber.
     * This is to prevent misusing un-protected endpoint to get new PID
     */
    @NonNull
    private String certificationRequestEncoded;

}
