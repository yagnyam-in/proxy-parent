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
 * PaymentAuthorization Registered confirmation by bank
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentAuthorizationRegistered implements SignableMessage {

  @NonNull
  public SignedMessage<PaymentAuthorization> paymentAuthorization;

  @NonNull
  private PaymentStatusEnum paymentStatus;

  @Override
  public ProxyId signer() {
    return paymentAuthorization.getMessage().getProxyAccount().getSignedBy();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return paymentAuthorization != null && paymentAuthorization.isValid() && paymentStatus != null;
  }

}
