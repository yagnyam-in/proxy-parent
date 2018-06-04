package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.SignableMessage;
import lombok.*;

/**
 * Request to update Customer details for given Proxy Id
 */
@Getter
@ToString
@EqualsAndHashCode(of = "requestId", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProxyCustomerUpdateRequest implements SignableMessage {

    @NonNull
    private String requestId;

    @NonNull
    private String proxyId;

    private String gcmToken;

    private String name;

    private String emailAddress;

    private String phoneNumber;

    private boolean syncWithContacts;

    @Override
    public String signer() {
        return proxyId;
    }
}
