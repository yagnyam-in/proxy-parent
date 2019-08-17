package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.messages.banking.ProxyAccount;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountCreationRequest implements SignableRequestMessage, AddressableMessage {

  @NonNull
  private String escrowNegotiationId;

  @NonNull
  private ProxyId payeeProxyId;

  @NonNull
  private ProxyId escrowProxyId;

  @NonNull
  private SignedMessage<ProxyAccount> debitProxyAccount;

  @NonNull
  private Amount amount;

  @NonNull
  private String title;

  private String description;

  @Override
  public ProxyId signer() {
    return getPayerProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(escrowNegotiationId)
        && ProxyUtils.isValid(payeeProxyId)
        && ProxyUtils.isValid(payeeProxyId)
        && ProxyUtils.isValid(debitProxyAccount)
        && ProxyUtils.isValid(amount)
        && StringUtils.isValid(title);
  }

  @Override
  public ProxyId address() {
    return debitProxyAccount.getSignedBy();
  }

  @JsonIgnore
  public ProxyId getBankProxyId() {
    return debitProxyAccount.getMessage().getBankProxyId();
  }

  @JsonIgnore
  public ProxyId getPayerProxyId() {
    return debitProxyAccount.getMessage().getOwnerProxyId();
  }

  @Override
  public String requestId() {
    return escrowNegotiationId;
  }
}
