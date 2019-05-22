package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * PaymentEncashment Registered confirmation by bank
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentEncashmentRegistered implements SignableMessage {

  @NonNull
  public SignedMessage<PaymentEncashment> paymentEncashment;

  @NonNull
  private PaymentStatusEnum paymentStatus;

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
    return paymentEncashment != null && paymentEncashment.isValid() && paymentStatus != null;
  }

}
