package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Date;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccount implements SignableMessage {

    @NonNull
    private EscrowAccountId escrowAccountId;

    @NonNull
    private ProxyId payerProxyId;

    @NonNull
    private ProxyId payeeProxyId;

    @NonNull
    private ProxyId escrowProxyId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Amount balance;

    @NonNull
    private String title;

    private String description;

    @Override
    public ProxyId signer() {
        return escrowAccountId.getBankProxyId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(payerProxyId)
                && ProxyUtils.isValid(payeeProxyId)
                && ProxyUtils.isValid(payeeProxyId)
                && ProxyUtils.isValid(escrowAccountId)
                && DateUtils.isValid(creationDate)
                && ProxyUtils.isValid(balance)
                && StringUtils.isValid(title);
    }

    @JsonIgnore
    public ProxyId getBankProxyId() {
        return escrowAccountId.getBankProxyId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return escrowAccountId.getProxyUniverse();
    }
}
