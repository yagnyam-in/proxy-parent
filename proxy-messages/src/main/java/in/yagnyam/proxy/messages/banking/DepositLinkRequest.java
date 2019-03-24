package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
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


/**
 * Request to get URL that can be used to deposit to Underlying Proxy Account
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class DepositLinkRequest implements SignableRequestMessage, AddressableMessage {

  @Builder
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  @ToString
  public static class RequestingCustomer {

    private String name;

    private String phone;

    private String email;

  }

  @NonNull
  private String requestId;

  @NonNull
  public SignedMessage<ProxyAccount> proxyAccount;

  @NonNull
  private String accountName;

  @NonNull
  private Amount amount;

  private String message;

  private RequestingCustomer requestingCustomer;

  @Override
  public ProxyId address() {
    return proxyAccount.getSignedBy();
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId signer() {
    return proxyAccount.getMessage().getOwnerProxyId();
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
        && amount != null && amount.isValid();
  }

  @JsonIgnore
  public ProxyAccountId getProxyAccountId() {
    return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getProxyAccountId() : null;
  }

  @JsonIgnore
  public ProxyId getOwnerProxyId() {
    return proxyAccount != null && proxyAccount.getMessage() != null ? proxyAccount.getMessage().getOwnerProxyId() : null;
  }
}
