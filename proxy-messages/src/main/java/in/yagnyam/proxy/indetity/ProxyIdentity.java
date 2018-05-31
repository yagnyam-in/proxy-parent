package in.yagnyam.proxy.indetity;

import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableMessage;
import lombok.*;

import java.util.Date;

/**
 * Proxy for a Person
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"issuerId", "customerId"})
public class ProxyIdentity implements SignableMessage, AddressableMessage {

    @NonNull
    private String issuerId;

    @NonNull
    private String customerId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @Override
    public String signer() {
        return issuerId;
    }

    @Override
    public String address() {
        return customerId;
    }
}
