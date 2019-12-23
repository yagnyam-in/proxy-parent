package in.yagnyam.proxy.messages.payments.offline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OfflineMoney implements SignableMessage {

    @NonNull
    private String moneyId;

    @NonNull
    private ProxyId bankProxyId;

    @NonNull
    private ProxyId ownerProxyId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @NonNull
    private Amount maxAmount;

    @Override
    public ProxyId signer() {
        return bankProxyId;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(moneyId)
                && ProxyUtils.isValid(bankProxyId)
                && ProxyUtils.isValid(ownerProxyId)
                && DateUtils.isValid(creationDate)
                && DateUtils.isValid(expiryDate)
                && ProxyUtils.isValid(maxAmount);
    }
}
