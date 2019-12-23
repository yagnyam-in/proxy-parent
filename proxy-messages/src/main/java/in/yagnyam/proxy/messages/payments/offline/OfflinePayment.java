package in.yagnyam.proxy.messages.payments.offline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.payments.PaymentRequest;
import in.yagnyam.proxy.utils.CollectionUtils;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OfflinePayment implements SignableMessage {

    @NonNull
    private String paymentId;

    @NonNull
    public SignedMessage<PaymentRequest> paymentRequest;

    @NonNull
    private ProxyId payerProxyId;

    @NonNull
    private List<SignedMessage<OfflineMoney>> offlineMoney;

    @NonNull
    private Amount amount;

    private String description;

    @Override
    public ProxyId signer() {
        return payerProxyId;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        boolean valid = StringUtils.isValid(paymentId)
                && ProxyUtils.isValid(paymentRequest)
                && ProxyUtils.isValid(payerProxyId)
                && CollectionUtils.isNotEmpty(offlineMoney)
                && offlineMoney.stream().allMatch(m -> m.getMessage().getOwnerProxyId().equals(payerProxyId))
                && ProxyUtils.isValid(amount);
        valid = valid && paymentRequest.getMessage().getAmount().equals(amount);
        Amount maxAmount = offlineMoney.stream().map(m -> m.getMessage().getMaxAmount()).reduce(Amount.of(amount.getCurrency(), 0), Amount::add);
        valid = valid && amount.isLessThanOrEquals(maxAmount);
        return valid;
    }
}
