package in.yagnyam.proxy.messages.escrow.alerts;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableAlertMessage;
import in.yagnyam.proxy.messages.escrow.EscrowAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountUpdatedAlert implements SignableAlertMessage {

  public static final String ESCROW_ACCOUNT_ID = "escrowAccountId";
  public static final String ESCROW_BANK_ID = "escrowAccountBankId";

  @NonNull
  private String alertId;

  @NonNull
  private EscrowAccountId escrowAccountId;

  @Override
  public String proxyUniverse() {
    return escrowAccountId.getProxyUniverse();
  }

  @NonNull
  @Singular
  private List<ProxyId> receivers;

  @Override
  public ProxyId signer() {
    return escrowAccountId.getBankProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isValid(alertId)
        && escrowAccountId != null && escrowAccountId.isValid()
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
    map.put(ESCROW_ACCOUNT_ID, escrowAccountId.getAccountId());
    map.put(ESCROW_BANK_ID, escrowAccountId.getBankProxyId().uniqueId());
    return map;
  }

}
