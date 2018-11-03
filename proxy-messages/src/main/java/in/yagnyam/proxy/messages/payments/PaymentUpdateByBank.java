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
 * Payment Status update by Bank
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentUpdateByBank implements SignableMessage {

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
  private PaymentStatus status;

  @Override
  public ProxyId signer() {
    return payment.getMessage().getProxyAccount().signer();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return payment != null && payment.isValid()
        && status != null;
  }

  @JsonIgnore
  public String bankId() {
    return payment != null && payment.getMessage() != null ? payment.getMessage().bankId() : null;
  }

}
