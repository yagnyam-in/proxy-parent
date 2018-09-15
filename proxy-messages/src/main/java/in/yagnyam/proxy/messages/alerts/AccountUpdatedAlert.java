package in.yagnyam.proxy.messages.alerts;

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
 * Alert used by GCM for sending Account Update Event
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"eventId"})
public class AccountUpdatedAlert implements SignableMessage, AddressableMessage {

  @NonNull
  private String eventId;

  @NonNull
  private ProxyAccountId proxyAccountId;

  @NonNull
  private ProxyId proxyId;

  @Override
  public ProxyId address() {
    return proxyId;
  }

  @Override
  public ProxyId signer() {
    return ProxyId.of(proxyAccountId.getBankId());
  }

  @Override
  public String toReadableString() {
    return String.format("Account %s is updated.", proxyAccountId.getAccountId());
  }

  @Override
  public boolean isValid() {
    return StringUtils.isValid(eventId)
        && proxyAccountId != null && proxyAccountId.isValid()
        && proxyId != null && proxyId.isValid();
  }
}
