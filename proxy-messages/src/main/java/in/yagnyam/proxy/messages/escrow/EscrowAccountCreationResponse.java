package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.*;

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
    return request.getMessage().bankProxyId();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid()
        && request.getMessage().getProxyUniverse()
        .equals(escrowAccount.getMessage().getEscrowAccountId().getProxyUniverse())
        && escrowAccount != null && escrowAccount.isValid()
        && escrowAccount.getSignedBy().canSignOnBehalfOf(request.getMessage().bankProxyId());
  }

  @JsonIgnore
  public String getProxyUniverse() {
    String proxyUniverseFromRequest =
        request.getMessage().getProxyUniverse();
    String proxyUniverseFromAccount =
        escrowAccount.getMessage().getEscrowAccountId().getProxyUniverse();
    if (!proxyUniverseFromRequest.equals(proxyUniverseFromAccount)) {
      throw new IllegalArgumentException(
          "Proxy Universe from request [" + proxyUniverseFromRequest + "]"
              + " != Proxy Universe from account [" + proxyUniverseFromAccount + "]");
    }
    return proxyUniverseFromAccount;
  }


}
