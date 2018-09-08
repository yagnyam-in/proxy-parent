package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class DepositLinkResponse implements SignableMessage, AddressableMessage {

  @NonNull
  private String requestId;

  @NonNull
  public SignedMessage<ProxyAccount> proxyAccount;

  @NonNull
  private String accountName;

  @NonNull
  private Amount amount;

  @NonNull
  private String depositLink;

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
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && proxyAccount != null && proxyAccount.isValid()
        && StringUtils.isValid(accountName)
        && amount != null && amount.isValid()
        && StringUtils.isValid(depositLink);
  }
}
