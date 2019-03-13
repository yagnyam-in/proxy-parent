package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
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
 * Payment Authorization Encashment
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = {"paymentAuthorization"})
public class PaymentAuthorizationEncashment implements SignableRequestMessage, AddressableMessage {

  @NonNull
  public SignedMessage<PaymentAuthorization> paymentAuthorization;

  @NonNull
  private String requestId;

  private Amount amount;

  private ProxyAccountId receivingProxyAccountId;

  private NonProxyAccount receivingNonProxyAccount;

  @Override
  public ProxyId signer() {
    return paymentAuthorization.getMessage().getPayeeId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && paymentAuthorization != null
        && paymentAuthorization.isValid()
        && amount != null
        && amount.isValid()
        && (receivingProxyAccountId != null && receivingProxyAccountId.isValid()
        || receivingNonProxyAccount != null && receivingNonProxyAccount.isValid());
  }

  @Override
  public ProxyId address() {
    return paymentAuthorization.getMessage().getProxyAccount().getSignedBy();
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @JsonIgnore
  public String getCurrency() {
    return paymentAuthorization != null && paymentAuthorization.getMessage() != null ? paymentAuthorization.getMessage().getCurrency() : null;
  }
}
