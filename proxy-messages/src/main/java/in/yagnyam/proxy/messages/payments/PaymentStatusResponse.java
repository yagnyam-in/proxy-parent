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
 * Payment Status Response
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusResponse implements SignableMessage {

  public enum PaymentStatus {
    Registered,
    InsufficientFunds,
    CancelledByPayer,
    CancleedByPayee,
    Expired,
    Processed
  }

  @NonNull
  public SignedMessage<Payment> payment;

  @NonNull
  private String requestId;

  @NonNull
  private PaymentStatus status;

  @Override
  public ProxyId signer() {
    return payment.getMessage().getProxyAccount().getSignedBy();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return payment != null && payment.isValid()
        && requestId != null
        && status != null;
  }

}
