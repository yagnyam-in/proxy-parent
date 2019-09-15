package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment Authorization
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentAuthorization implements SignableRequestMessage {

    @NonNull
    private String paymentAuthorizationId;

    @NonNull
    public SignedMessage<ProxyAccount> proxyAccount;

    @NonNull
    private Amount amount;

    @NonNull
    private List<Payee> payees;

    @Override
    public ProxyId signer() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(paymentAuthorizationId)
                && proxyAccount != null && proxyAccount.isValid()
                && amount != null && amount.isValid()
                && payees != null && !payees.isEmpty() && payees.stream().allMatch(Payee::isValid)
                // Ensure payment encashement ids are unique
                && payees.stream().map(Payee::getPaymentEncashmentId).collect(Collectors.toSet()).size() == payees.size();
    }

    @Override
    public String requestId() {
        return paymentAuthorizationId;
    }

    @JsonIgnore
    public ProxyId getPayerId() {
        return proxyAccount.getMessage().getOwnerProxyId();
    }

    @JsonIgnore
    public ProxyAccountId getPayerAccountId() {
        return proxyAccount.getMessage().getProxyAccountId();
    }

    @JsonIgnore
    public String getCurrency() {
        return proxyAccount.getMessage().getCurrency();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return proxyAccount.getMessage().getProxyUniverse();
    }

    @JsonIgnore
    public ProxyId getPayerBankProxyId() {
        return proxyAccount.getSignedBy();
    }

}
