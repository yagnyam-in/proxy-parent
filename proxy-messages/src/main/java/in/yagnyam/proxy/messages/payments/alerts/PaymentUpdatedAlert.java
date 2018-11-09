package in.yagnyam.proxy.messages.payments.alerts;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableAlertMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.List;
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
@EqualsAndHashCode(of = {"alertId"})
public class PaymentUpdatedAlert implements SignableAlertMessage {

  public enum DataFields {
    Type,
    AlertId,
    PaymentId,
    PayerAccountId,
    PayerBankId
  }

  @NonNull
  private String alertId;

  @NonNull
  private String paymentId;

  @NonNull
  private ProxyAccountId payerAccountId;

  @NonNull
  private List<ProxyId> receivers;

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
    return StringUtils.isValid(alertId)
        && payerAccountId != null && payerAccountId.isValid()
        && receivers != null && receivers.stream().allMatch(ProxyId::isValid);
  }

  @Override
  public String alertId() {
    return alertId;
  }

  @Override
  public List<ProxyId> receivers() {
    return receivers;
  }
}
