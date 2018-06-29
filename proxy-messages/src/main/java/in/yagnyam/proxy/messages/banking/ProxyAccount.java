package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Date;

/**
 * Proxy Account
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"proxyAccountId", "proxyId"})
public class ProxyAccount implements SignableMessage, AddressableMessage {

    @NonNull
    private ProxyAccountId proxyAccountId;

    @NonNull
    private String proxyId;

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
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return proxyAccountId != null
                && proxyAccountId.isValid()
                && StringUtils.isEmpty(proxyId)
                && DateUtils.isValid(creationDate)
                && DateUtils.isValid(expiryDate)
                && maximumAmountPerTransaction != null
                && maximumAmountPerTransaction.isValid();
    }

    @Override
    public String address() {
        return proxyId;
    }
}
