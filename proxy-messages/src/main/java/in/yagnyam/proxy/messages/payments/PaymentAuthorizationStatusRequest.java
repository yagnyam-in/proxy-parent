package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentAuthorizationStatusRequest implements SignableRequestMessage, AddressableMessage {

    @NonNull
    private String requestId;

    @NonNull
    public SignedMessage<PaymentAuthorization> paymentAuthorization;

    @Override
    public ProxyId signer() {
        return getPayerId();
    }

    @Override
    public String toReadableString() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isValid(requestId) && paymentAuthorization != null && paymentAuthorization.isValid();
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public ProxyId address() {
        return paymentAuthorization.getMessage().getProxyAccount().getSignedBy();
    }

    @JsonIgnore
    public String getPaymentAuthorizationId() {
        return paymentAuthorization.getMessage().getPaymentAuthorizationId();
    }

    @JsonIgnore
    public ProxyId getPayerId() {
        return paymentAuthorization.getMessage().getPayerId();
    }

    @JsonIgnore
    public ProxyAccountId getPayerAccountId() {
        return paymentAuthorization.getMessage().getPayerAccountId();
    }

    @JsonIgnore
    public String getCurrency() {
        return paymentAuthorization.getMessage().getCurrency();
    }

    @JsonIgnore
    public String getProxyUniverse() {
        return paymentAuthorization.getMessage().getProxyUniverse();
    }

}
