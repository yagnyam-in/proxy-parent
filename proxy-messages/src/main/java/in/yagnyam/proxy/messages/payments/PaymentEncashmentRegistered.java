package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

/**
 * PaymentEncashment Registered confirmation by bank
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEncashmentRegistered implements SignableMessage {

  @NonNull
  public SignedMessage<PaymentEncashment> paymentEncashment;

  @NonNull
  private PaymentEncashmentStatusEnum paymentEncashmentStatus;

  @Override
  public ProxyId signer() {
    return paymentEncashment.getMessage().getPayerBankProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return paymentEncashment != null && paymentEncashment.isValid() && paymentEncashmentStatus != null;
  }

}
