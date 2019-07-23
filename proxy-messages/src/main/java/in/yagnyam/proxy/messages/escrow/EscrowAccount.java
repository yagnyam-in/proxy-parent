package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.utils.DateUtils;
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
    private ProxyId buyerProxyId;

    @NonNull
    private ProxyId sellerProxyId;

    @NonNull
    private ProxyId escrowProxyId;

    @NonNull
    private Date creationDate;

    @NonNull
    private Amount balance;

    @NonNull
    private String title;

    @NonNull
    private String description;

    @Override
    public ProxyId signer() {
        return ProxyId.of(escrowAccountId.getBankId());
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return escrowAccountId != null && escrowAccountId.isValid()
                && buyerProxyId != null && buyerProxyId.isValid()
                && sellerProxyId != null && sellerProxyId.isValid()
                && escrowAccountId != null && escrowAccountId.isValid()
                && DateUtils.isValid(creationDate)
                && balance != null && balance.isValid()
                && StringUtils.isValid(title)
                && StringUtils.isValid(description);
    }

    @JsonIgnore
    public String getBankId() {
        return escrowAccountId.getBankId();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return escrowAccountId.getProxyUniverse();
    }
}
