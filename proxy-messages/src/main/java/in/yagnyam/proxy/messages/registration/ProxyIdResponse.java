package in.yagnyam.proxy.messages.registration;

import lombok.*;

/**
 * Proxy Id response from Server
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProxyIdResponse {

    /**
     * Request Number
     */
    @NonNull
    private String requestNumber;

    /**
     * Unique Proxy Id
     */
    @NonNull
    private String proxyId;

}
