package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEncashment implements SignableRequestMessage, AddressableMessage {

  @NonNull
  private String paymentEncashmentId;

  @NonNull
  public SignedMessage<PaymentAuthorization> paymentAuthorization;

  @NonNull
  public SignedMessage<ProxyAccount> payeeAccount;

  private String secret;

  @Override
  public ProxyId signer() {
    return getPayeeId();
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    boolean valid = paymentAuthorization != null && paymentAuthorization.isValid()
        && payeeAccount != null && payeeAccount.isValid();
    if (!valid) {
      return false;
    }
    Payee payee = getPayee();
    switch (payee.getPayeeType()) {
      case ProxyId:
        return payee.getProxyId().equals(getPayeeId());
      case Email:
      case Phone:
      case AnyoneWithSecret:
        return StringUtils.isValid(secret);
      default:
        return false;
    }
  }

  @JsonIgnore
  public Payee getPayee() {
    return paymentAuthorization.getMessage().getPayees().stream()
        .filter(p -> p.getPaymentEncashmentId().equals(paymentEncashmentId))
        .findAny()
        .orElseThrow(
            () -> new IllegalStateException("Payment encashment doesn't have matching Payee"));
  }


  @Override
  public ProxyId address() {
    return paymentAuthorization.getMessage().getProxyAccount().getSignedBy();
  }

  @Override
  public String requestId() {
    return paymentEncashmentId;
  }

  @JsonIgnore
  public String getPaymentAuthorizationId() {
    return paymentAuthorization.getMessage().getPaymentAuthorizationId();
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

}
