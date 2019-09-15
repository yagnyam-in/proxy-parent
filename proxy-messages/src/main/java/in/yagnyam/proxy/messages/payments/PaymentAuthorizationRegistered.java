package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

/**
 * PaymentAuthorization Registered confirmation by bank
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentAuthorizationRegistered implements SignableMessage {

  @NonNull
  public SignedMessage<PaymentAuthorization> paymentAuthorization;

  @NonNull
  private PaymentAuthorizationStatusEnum paymentAuthorizationStatus;

  @Override
  public ProxyId signer() {
    return paymentAuthorization.getMessage().getProxyAccount().getSignedBy();
  }@Override
  @JsonIgnore
  public boolean isValid() {
    return paymentAuthorization != null && paymentAuthorization.isValid() && paymentAuthorizationStatus != null;
  }

}
