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
 * Confirmation that Payment process is failed
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentFailed implements SignableMessage {

  enum FailedReason {
    InsufficientFunds,
    CancelledByPayer,
    CancleedByPayee,
    Expired
  }

  @NonNull
  public SignedMessage<Payment> payment;

  @NonNull
  private FailedReason failedReason;

  @Override
  public ProxyId signer() {
    return payment.getMessage().proxyAccount.getMessage().getProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return payment != null && payment.isValid()
        && failedReason != null;
  }

}
