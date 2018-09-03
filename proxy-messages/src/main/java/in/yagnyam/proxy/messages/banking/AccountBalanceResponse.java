package in.yagnyam.proxy.messages.banking;

import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class AccountBalanceResponse implements SignableMessage, AddressableMessage {

  @NonNull
  private String requestId;

  @NonNull
  private SignedMessage<ProxyAccount> proxyAccount;

  @NonNull
  private Amount balance;

  @Override
  public ProxyId address() {
    return proxyAccount.signer();
  }

  @Override
  public ProxyId signer() {
    return proxyAccount.getMessage().getProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && proxyAccount != null && proxyAccount.isValid();
  }
}
