package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

/**
 * Payment Authorization
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentAuthorization implements SignableRequestMessage {

  @NonNull
  public SignedMessage<ProxyAccount> proxyAccount;

  @NonNull
  private String paymentId;

  @NonNull
  private Amount amount;

  @NonNull
  private Payee payee;

  @Override
  public ProxyId signer() {
    return proxyAccount.getMessage().getOwnerProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(paymentId)
        && proxyAccount != null && proxyAccount.isValid()
        && amount != null && amount.isValid()
        && payee != null && payee.isValid();
  }

  @Override
  public String requestId() {
    return paymentId;
  }

  @JsonIgnore
  public ProxyId getPayerId() {
    return proxyAccount.getMessage().getOwnerProxyId();
  }

  @JsonIgnore
  public ProxyAccountId getPayerAccountId() {
    return proxyAccount.getMessage().getProxyAccountId();
  }

  @JsonIgnore
  public String getCurrency() {
    return proxyAccount.getMessage().getCurrency();
  }

  @JsonIgnore
  public String getProxyUniverse() {
    return proxyAccount.getMessage().getProxyUniverse();
  }

  @JsonIgnore
  public ProxyAccountId getPayeeAccountId() {
    return payee.getProxyAccountId();
  }

  @JsonIgnore
  public ProxyId getPayeeId() {
    return payee.getProxyId();
  }

  @JsonIgnore
  public ProxyId getPayerBankProxyId() {
    return proxyAccount.getSignedBy();
  }

}
