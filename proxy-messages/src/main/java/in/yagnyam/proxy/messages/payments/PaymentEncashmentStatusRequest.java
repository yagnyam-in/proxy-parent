package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Set;

/**
 * PaymentAuthorization Status Request
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentEncashmentStatusRequest implements SignableRequestMessage, AddressableMessage {

  @NonNull
  private String requestId;

  @NonNull
  public SignedMessage<PaymentEncashment> paymentEncashment;

  @Override
  public ProxyId signer() {
    throw new RuntimeException("PaymentEncashmentStatusRequest.signer should never be invoked when PaymentEncashmentStatusRequest.validSigners is implemented");
  }

  @Override
  public Set<ProxyId> validSigners() {
      return ImmutableSet.of(paymentEncashment.getMessage().getPayerId(), paymentEncashment.getMessage().getPayerId());
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId) && paymentEncashment != null && paymentEncashment.isValid();
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId address() {
    return paymentEncashment.getMessage().getPaymentAuthorization().getMessage().getProxyAccount().getSignedBy();
  }

  @JsonIgnore
  public String getPaymentId() {
    return paymentEncashment.getMessage().getPaymentId();
  }

}