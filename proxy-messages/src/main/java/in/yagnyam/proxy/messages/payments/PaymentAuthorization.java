package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.utils.DateUtils;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.Date;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Payment Authorization
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentAuthorizationId", "proxyAccount"})
public class PaymentAuthorization implements SignableRequestMessage, AddressableMessage {

  @NonNull
  public SignedMessage<ProxyAccount> proxyAccount;
  @NonNull
  private String requestId;
  @NonNull
  private String paymentAuthorizationId;

  /**
   * Payee is mandatory (Anonymous Cheques are not supported to prevent Fraud)
   */
  @NonNull
  private ProxyId payeeId;

  @NonNull
  private Date validFrom;

  @NonNull
  private Date validTill;

  @NonNull
  private Date issueDate;

  @NonNull
  private Amount amount;

  @NonNull
  private String transactionId;

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
    return StringUtils.isValid(requestId)
        && proxyAccount != null
        && proxyAccount.isValid()
        && payeeId != null && payeeId.isValid()
        && DateUtils.isValid(validFrom)
        && DateUtils.isValid(validTill)
        && DateUtils.isValid(issueDate)
        && amount != null
        && amount.isValid()
        && StringUtils.isValid(transactionId);
  }

  @Override
  public ProxyId address() {
    return payeeId;
  }

  @Override
  public String requestId() {
    return requestId;
  }
}
