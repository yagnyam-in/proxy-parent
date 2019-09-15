package in.yagnyam.proxy.messages.sack;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.*;

import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MoneySack implements SignableMessage {

    @NonNull
    private MoneySackId moneySackId;

    @NonNull
    private ProxyId ownerProxyId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Date expiryDate;

    @NonNull
    private Amount balance;

    @Override
    public ProxyId signer() {
        return moneySackId.getBankProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return moneySackId != null
                && moneySackId.isValid()
                && ownerProxyId != null && ownerProxyId.isValid()
                && DateUtils.isValid(creationDate)
                && DateUtils.isValid(expiryDate)
                && ProxyUtils.isValid(balance);
    }

    @JsonIgnore
    public ProxyId getBankProxyId() {
        return moneySackId.getBankProxyId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return moneySackId.getProxyUniverse();
    }
}
