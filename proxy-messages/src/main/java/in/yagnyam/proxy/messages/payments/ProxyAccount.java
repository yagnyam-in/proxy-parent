package in.yagnyam.proxy.messages.payments;

import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableMessage;
import lombok.*;

import java.util.Date;

/**
 * Proxy Account
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"proxyAccountId", "customerId"})
public class ProxyAccount implements SignableMessage, AddressableMessage {

    @NonNull
    private ProxyAccountId proxyAccountId;

    @NonNull
    private String customerId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    /**
     * Maximum amount for which *each* Payment can be made
     */
    @NonNull
    private Amount maximumAmountPerTransaction;

    @Override
    public String signer() {
        return proxyAccountId.getBankId();
    }

    @Override
    public String address() {
        return customerId;
    }
}