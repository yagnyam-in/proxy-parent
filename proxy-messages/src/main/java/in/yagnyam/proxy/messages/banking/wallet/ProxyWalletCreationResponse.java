package in.yagnyam.proxy.messages.banking.wallet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Response message for Proxy Wallet Creation
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"request"})
public class ProxyWalletCreationResponse implements SignableMessage, AddressableMessage {

  @NonNull
  private SignedMessage<ProxyWalletCreationRequest> request;

  @NonNull
  public SignedMessage<ProxyAccount> proxyAccount;

  @Override
  public ProxyId address() {
    return proxyAccount.getMessage().getOwnerProxyId();
  }

  @Override
  public ProxyId signer() {
    return request.getMessage().getBankId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid()
        && proxyAccount != null && proxyAccount.isValid()
        && proxyAccount.getSignedBy().canSignOnBehalfOf(request.getMessage().getBankId());
  }
}
