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
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentEncashmentStatusResponse implements SignableMessage {

  @NonNull
  public SignedMessage<PaymentEncashmentStatusRequest> request;

  @NonNull
  private PaymentStatusEnum paymentStatus;

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
        && paymentStatus != null;
  }

  @JsonIgnore
  public String getPaymentId() {
    return request.getMessage().getPaymentId();
  }

}
