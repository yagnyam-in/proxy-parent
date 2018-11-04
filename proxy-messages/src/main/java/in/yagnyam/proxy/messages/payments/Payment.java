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

  private ProxyAccountId payeeAccountId;

  private ProxyId payeeId;

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
    return StringUtils.isValid(paymentId)
        && proxyAccount != null && proxyAccount.isValid()
        && (payeeId != null || payeeAccountId != null)
        && (payeeId == null || payeeId.isValid())
        && (payeeAccountId == null || payeeAccountId.isValid());
  }

  @Override
  public ProxyId address() {
    if (payeeAccountId == null) {
      return payeeId;
    } else {
      return proxyAccount.getSignedBy();
    }
  }

  @Override
  public String requestId() {
    return paymentId;
  }

  @JsonIgnore
  public String bankId() {
    return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().bankId() : null;
  }

}
