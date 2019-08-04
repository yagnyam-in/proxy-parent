package in.yagnyam.proxy.messages.escrow.negotiation;

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
public class EscrowAccountProposalPayeeAck implements SignableMessage {

  @NonNull
  private ProxyId payeeProxyId;

  @NonNull
  private SignedMessage<EscrowAccountProposal> escrowAccountProposal;

  @Override
  public ProxyId signer() {
    return payeeProxyId;
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  public boolean isValid() {
    return ProxyUtils.isValid(payeeProxyId) && ProxyUtils.isValid(escrowAccountProposal);
  }
}
