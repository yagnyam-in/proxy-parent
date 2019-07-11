package in.yagnyam.proxy.messages.payments.alerts;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableAlertMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;
import java.util.Map;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"alertId"})
public class PaymentAuthorizationUpdatedAlert implements SignableAlertMessage {

  public static final String PAYMENT_AUTHORIZATION_ID = "paymentAuthorizationId";
  public static final String PAYER_ACCOUNT_ID = "payerAccountId";
  public static final String PAYER_BANK_ID = "payerBankId";

  @NonNull
  private String alertId;

  @NonNull
  private String paymentAuthorizationId;

  @NonNull
  private ProxyAccountId payerAccountId;

  @Override
  public String proxyUniverse() {
    return payerAccountId.getProxyUniverse();
  }

  @NonNull
  @Singular
  private List<ProxyId> receivers;

  @Override
  public ProxyId signer() {
    return ProxyId.of(payerAccountId.getBankId());
  }

  @Override
  public String toReadableString() {
    return String.format("PaymentAuthorization %s is updated.", paymentAuthorizationId);
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

  @Override
  public Map<String, String> toFcmMap() {
    Map<String, String> map = SignableAlertMessage.super.toFcmMap();
    map.put(PAYMENT_AUTHORIZATION_ID, paymentAuthorizationId);
    map.put(PAYER_ACCOUNT_ID, payerAccountId.getAccountId());
    map.put(PAYER_BANK_ID, payerAccountId.getBankId());
    return map;
  }

}