package in.yagnyam.proxy.messages.payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Payment Status Request
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusRequest implements SignableRequestMessage, AddressableMessage {

  @NonNull
  public SignedMessage<Payment> payment;

  @NonNull
  private String requestId;

  @Override
  public ProxyId signer() {
    throw new RuntimeException("SignableMessage.signer should never be invoked when SignableMessage.validSigners is implemented");
  }

  @Override
  public Set<ProxyId> validSigners() {
    if (payment.getMessage().getPayeeId() == null) {
      return ImmutableSet.of(payment.getSignedBy());
    } else {
      return ImmutableSet.of(payment.getSignedBy(), payment.getMessage().getPayeeId());
    }
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return payment != null && payment.isValid()
        && requestId != null;
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId address() {
    return payment.getMessage().getProxyAccount().getSignedBy();
  }
}
