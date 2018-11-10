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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Payment to the payee or payee account
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentId"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payment implements SignableRequestMessage, AddressableMessage {

  @NonNull
  public SignedMessage<ProxyAccount> proxyAccount;

  @NonNull
  private String paymentId;

  @NonNull
  private Amount amount;

  private ProxyId payeeId;

  private ProxyAccountId payeeAccountId;

  private String ivPrefixedSecretHash;

  @Override
  public ProxyId signer() {
    return proxyAccount.getMessage().getProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    boolean valid = StringUtils.isValid(paymentId)
        && proxyAccount != null && proxyAccount.isValid()
        && amount != null && amount.isValid();

    if (StringUtils.isValid(ivPrefixedSecretHash)) {
      valid = valid && payeeId == null && payeeAccountId == null;
    } else {
      valid = valid && payeeId != null && payeeId.isValid()
          && (payeeAccountId == null || payeeAccountId.isValid());
    }
    return valid;
  }

  @Override
  public String requestId() {
    return paymentId;
  }

  @JsonIgnore
  public ProxyId getPayerId() {
    return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getProxyId() : null;
  }

  @JsonIgnore
  public ProxyAccountId getPayerAccountId() {
    return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getProxyAccountId() : null;
  }

  @Override
  public ProxyId address() {
    return payeeId != null ? payeeId : ProxyId.any();
  }
}
