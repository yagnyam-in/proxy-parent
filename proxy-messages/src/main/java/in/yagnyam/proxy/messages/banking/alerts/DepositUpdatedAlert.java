package in.yagnyam.proxy.messages.banking.alerts;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableAlertMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Alert used by GCM for sending Deposit Update Event
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"alertId"})
public class DepositUpdatedAlert implements SignableAlertMessage {

  public static final String DEPOSIT_ID = "depositId";
  public static final String ACCOUNT_ID = "accountId";
  public static final String BANK_ID = "bankId";

  @NonNull
  private String alertId;

  @NonNull
  private ProxyAccountId proxyAccountId;

  @Override
  public String proxyUniverse() {
    return proxyAccountId.getProxyUniverse();
  }

  @NonNull
  private String depositId;

  @NonNull
  private ProxyId receiverId;

  @Override
  public ProxyId signer() {
    return ProxyId.of(proxyAccountId.getBankId());
  }

  @Override
  public String toReadableString() {
    return String.format("Deposit request %s is updated.", depositId);
  }

  @Override
  public boolean isValid() {
    return StringUtils.isValid(alertId)
        && StringUtils.isValid(depositId)
        && receiverId != null && receiverId.isValid();
  }

  @Override
  public String alertId() {
    return alertId;
  }

  @Override
  public List<ProxyId> receivers() {
    return Collections.singletonList(receiverId);
  }

  @Override
  public Map<String, String> toFcmMap() {
    Map<String, String> map = SignableAlertMessage.super.toFcmMap();
    map.put(DEPOSIT_ID, depositId);
    map.put(ACCOUNT_ID, proxyAccountId.getAccountId());
    map.put(BANK_ID, proxyAccountId.getBankId());
    return map;
  }

}