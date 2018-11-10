package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Payment Encashment by Payee
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentEncashment implements SignableMessage, AddressableMessage {

  @NonNull
  public SignedMessage<Payment> payment;

  @NonNull
  private ProxyAccountId payeeAccountId;

  @NonNull
  private ProxyId payeeId;

  @Override
  public ProxyId signer() {
    return payment.getMessage().address();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    boolean valid = payment != null && payment.isValid()
        && payeeId != null && payeeId.isValid()
        && payeeAccountId != null && payeeAccountId.isValid();
    if (!valid) return false;
    if (payment.getMessage().getPayeeId() != null) {
      valid = payment.getMessage().getPayeeId().isParentOrEqualsOf(payeeId);
    }
    if (payment.getMessage().getPayeeAccountId() != null) {
      valid = valid && payment.getMessage().getPayeeAccountId().equals(payeeAccountId);
    }
    return valid;
  }

  @Override
  public ProxyId address() {
    return payment.getMessage().getProxyAccount().getSignedBy();
  }

  @JsonIgnore
  public String getPaymentId() {
    return payment != null && payment.getMessage() != null ? payment.getMessage().getPaymentId() : null;
  }

  @JsonIgnore
  public Amount getAmount() {
    return payment != null && payment.getMessage() != null ? payment.getMessage().getAmount() : null;
  }

  @JsonIgnore
  public ProxyId getPayerId() {
    return payment != null && payment.getMessage() != null ? payment.getMessage().getPayerId() : null;
  }

  @JsonIgnore
  public ProxyAccountId getPayerAccountId() {
    return payment != null && payment.getMessage() != null ? payment.getMessage().getPayerAccountId() : null;
  }

}
