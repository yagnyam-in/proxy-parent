package in.yagnyam.proxy.messages.payments.alerts;

import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
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
 * Alert used by GCM for sending Payment Update Event
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"eventId"})
public class PaymentUpdatedAlert implements SignableMessage, AddressableMessage {

  @NonNull
  private String eventId;

  @NonNull
  private String paymentId;

  @NonNull
  private ProxyAccountId payerAccountId;

  @NonNull
  private ProxyId receiverId;

  @Override
  public ProxyId address() {
    return receiverId;
  }

  @Override
  public ProxyId signer() {
    return ProxyId.of(payerAccountId.getBankId());
  }

  @Override
  public String toReadableString() {
    return String.format("Payment %s is updated.", paymentId);
  }

  @Override
  public boolean isValid() {
    return StringUtils.isValid(eventId)
        && payerAccountId != null && payerAccountId.isValid()
        && receiverId != null && receiverId.isValid();
  }
}
