package in.yagnyam.proxy.messages.banking.wallet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.messages.banking.Currency;
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
 * Message to create new Proxy Wallet
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class ProxyWalletCreationRequest implements SignableRequestMessage, AddressableMessage {

  @NonNull
  private String requestId;

  @NonNull
  private String proxyUniverse;

  @NonNull
  private ProxyId proxyId;

  @NonNull
  private ProxyId bankId;

  @NonNull
  private String currency;

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId signer() {
    return proxyId;
  }

  @Override
  public String toReadableString() {
    return String.format(
        "With request %s, create new %s wallet for proxy id %s ", requestId, currency, proxyId);
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && StringUtils.isValid(proxyUniverse)
        && proxyId != null && proxyId.isValid()
        && bankId != null && bankId.isValid()
        && Currency.isValidCurrency(currency);
  }

  @Override
  public ProxyId address() {
    return bankId;
  }
}
