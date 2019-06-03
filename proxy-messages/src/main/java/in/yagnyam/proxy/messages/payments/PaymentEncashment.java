package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * PaymentAuthorization Encashment by Payee
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEncashment implements SignableMessage, AddressableMessage {

  @NonNull
  public SignedMessage<PaymentAuthorization> paymentAuthorization;

  @NonNull
  public SignedMessage<ProxyAccount> payeeAccount;

  private String secret;

  @Override
  public ProxyId signer() {
    return payeeAccount.getMessage().getOwnerProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    boolean valid = paymentAuthorization != null && paymentAuthorization.isValid()
        && payeeAccount != null && payeeAccount.isValid();
    if (!valid) return false;

    switch (paymentAuthorization.getMessage().getPayee().getPayeeType()) {
      case ProxyAccountId:
        return paymentAuthorization.getMessage().getPayeeAccountId().equals(payeeAccount.getMessage().getProxyAccountId())
                && paymentAuthorization.getMessage().getPayeeId().equals(payeeAccount.getMessage().getOwnerProxyId());
      case ProxyId:
        return paymentAuthorization.getMessage().getPayeeId().equals(payeeAccount.getMessage().getOwnerProxyId());
      case Email:
      case Phone:
      case AnyoneWithSecret:
        return StringUtils.isValid(secret);
      default:
        return false;
    }
  }

  @Override
  public ProxyId address() {
    return paymentAuthorization.getMessage().getProxyAccount().getSignedBy();
  }


  @JsonIgnore
  public String getPaymentId() {
    return paymentAuthorization.getMessage().getPaymentId();
  }

  @JsonIgnore
  public Amount getAmount() {
    return paymentAuthorization.getMessage().getAmount();
  }

  @JsonIgnore
  public ProxyId getPayerId() {
    return paymentAuthorization.getMessage().getPayerId();
  }

  @JsonIgnore
  public ProxyAccountId getPayerAccountId() {
    return paymentAuthorization.getMessage().getPayerAccountId();
  }

  @JsonIgnore
  public String getCurrency() {
    return paymentAuthorization.getMessage().getCurrency();
  }

  @JsonIgnore
  public ProxyId getPayeeId() {
        return payeeAccount.getMessage().getOwnerProxyId();
    }

  @JsonIgnore
  public ProxyAccountId getPayeeAccountId() {
        return payeeAccount.getMessage().getProxyAccountId();
    }

  @JsonIgnore
  public ProxyId getPayerBankProxyId() {
      return paymentAuthorization.getMessage().getPayerBankProxyId();
    }

  @JsonIgnore
  public ProxyId getPayeeBankProxyId() {
        return payeeAccount.getSignedBy();
    }

  @JsonIgnore
  public String getProxyUniverse() {
    return paymentAuthorization.getMessage().getProxyUniverse();
  }

}
