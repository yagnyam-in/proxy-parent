package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import java.util.Date;

import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Payment Authorization that is protected by a Secret
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentAuthorizationId", "proxyAccount"})
public class SecretProtectedPaymentAuthorization implements SignableMessage {

  @NonNull
  public SignedMessage<ProxyAccount> proxyAccount;

  @NonNull
  private String paymentAuthorizationId;

  @NonNull
  private Date validFrom;

  @NonNull
  private Date validTill;

  @NonNull
  private Date issueDate;

  @NonNull
  private Amount amount;

  @NonNull
  private String ivPrefixedSecretHash;

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
    return false;
  }

  @JsonIgnore
  public ProxyAccountId getPayerAccountId() {
    return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getProxyAccountId() : null;
  }

  @JsonIgnore
  public String getCurrency() {
    return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getCurrency() : null;
  }
}
