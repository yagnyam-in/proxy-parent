package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import lombok.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Proxy Account
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxyAccount implements SignableMessage {

    @NonNull
    private ProxyAccountId proxyAccountId;

    @NonNull
    private ProxyId ownerProxyId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @NonNull
    private String currency;

    // TODO: Remove default list and make it non null
    @Builder.Default
    private List<ProxyAccountPermissionEnum> permissions = Collections.emptyList();

    /**
     * Maximum amount for which *each* PaymentAuthorization can be made
     */
    @NonNull
    private Amount maximumAmountPerTransaction;

    @Override
    public ProxyId signer() {
        return proxyAccountId.getBankProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return proxyAccountId != null
                && proxyAccountId.isValid()
                && ownerProxyId != null && ownerProxyId.isValid()
                && DateUtils.isValid(creationDate)
                && DateUtils.isValid(expiryDate)
                && currency != null
                && maximumAmountPerTransaction != null && maximumAmountPerTransaction.isValid();
    }

    @JsonIgnore
    public ProxyId getBankProxyId() {
        return proxyAccountId.getBankProxyId();
    }
}
