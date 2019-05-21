package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

/**
 * PaymentAuthorization Status Response
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentAuthorizationStatusResponse implements SignableMessage {

  @NonNull
  public SignedMessage<PaymentAuthorizationStatusRequest> request;

  @NonNull
  private PaymentStatusEnum paymentStatus;

  @Override
  public ProxyId signer() {
    return request.getMessage().getPaymentAuthorization().getMessage().getProxyAccount().getSignedBy();
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
