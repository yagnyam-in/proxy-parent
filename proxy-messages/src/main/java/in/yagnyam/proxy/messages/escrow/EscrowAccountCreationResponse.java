package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountCreationResponse implements SignableMessage {

  @NonNull
  private SignedMessage<EscrowAccountCreationRequest> request;

  @NonNull
  public SignedMessage<EscrowAccount> escrowAccount;

  @Override
  public ProxyId signer() {
    return request.getMessage().getBankProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    if (!ProxyUtils.isValid(request) || !ProxyUtils.isValid(escrowAccount)) {
      return false;
    }
    EscrowAccountCreationRequest request = this.request.getMessage();
    EscrowAccount account = this.escrowAccount.getMessage();
    return request.getAmount().equals(account.getBalance())
        && request.getPayerProxyId().equals(account.getPayerProxyId())
        && request.getPayeeProxyId().equals(account.getPayeeProxyId())
        && request.getEscrowProxyId().equals(account.getEscrowProxyId())
        && request.getDebitProxyAccount().getMessage().getBankProxyId().equals(account.getBankProxyId());
  }

}
