package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

import java.util.Set;

/**
 * PaymentAuthorization Status Response
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEncashmentStatusResponse implements SignableMessage {

  @NonNull
  public SignedMessage<PaymentEncashmentStatusRequest> request;

  @NonNull
  private PaymentEncashmentStatusEnum paymentEncashmentStatus;

  @Override
  public ProxyId signer() {
    throw new RuntimeException("PaymentEncashmentStatusResponse.signer should never be invoked when PaymentEncashmentStatusResponse.validSigners is implemented");
  }

  @Override
  public Set<ProxyId> validSigners() {
    PaymentEncashment paymentEncashment = request.getMessage().getPaymentEncashment().getMessage();
    return ImmutableSet.of(paymentEncashment.getPayeeBankProxyId(), paymentEncashment.getPayerBankProxyId());
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid()
        && paymentEncashmentStatus != null;
  }

  @JsonIgnore
  public String getPaymentAuthorizationId() {
    return request.getMessage().getPaymentAuthorizationId();
  }

  @JsonIgnore
  public String getPaymentEncashmentId() {
    return request.getMessage().getPaymentEncashmentId();
  }

}
