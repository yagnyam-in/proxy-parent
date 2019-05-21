package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableSet;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import java.util.Set;

import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * PaymentAuthorization Status Request
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentAuthorizationStatusRequest implements SignableRequestMessage, AddressableMessage {

  @NonNull
  private String requestId;

  @NonNull
  public SignedMessage<PaymentAuthorization> paymentAuthorization;

  @Override
  public ProxyId signer() {
    throw new RuntimeException("PaymentAuthorizationStatusRequest.signer should never be invoked when PaymentAuthorizationStatusRequest.validSigners is implemented");
  }

  @Override
  public Set<ProxyId> validSigners() {
    ProxyId payerId = paymentAuthorization.getMessage().getPayerId();
    ProxyId payeeId = paymentAuthorization.getMessage().getPayeeId();
    if (payeeId == null) {
      return ImmutableSet.of(payerId);
    } else {
      return ImmutableSet.of(payerId, payeeId);
    }
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId) && paymentAuthorization != null && paymentAuthorization.isValid();
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId address() {
    return paymentAuthorization.getMessage().getProxyAccount().getSignedBy();
  }

  @JsonIgnore
  public String getPaymentId() {
    return paymentAuthorization != null && paymentAuthorization.getMessage() != null ? paymentAuthorization.getMessage().getPaymentId() : null;
  }

  @JsonIgnore
  public ProxyAccountId getPayerAccountId() {
    return paymentAuthorization != null && paymentAuthorization.getMessage() != null ? paymentAuthorization.getMessage().getPayerAccountId() : null;
  }

  @JsonIgnore
  public String getCurrency() {
    return paymentAuthorization != null && paymentAuthorization.getMessage() != null ? paymentAuthorization.getMessage().getCurrency() : null;
  }
}
