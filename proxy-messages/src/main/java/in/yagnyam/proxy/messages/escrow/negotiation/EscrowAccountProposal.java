package in.yagnyam.proxy.messages.escrow.negotiation;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.messages.banking.Amount;
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

@Builder
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EscrowAccountProposal implements SignableRequestMessage {

  @NonNull
  private String negotiationId;

  @NonNull
  private String proxyUniverse;

  @NonNull
  private ProxyId payerProxyId;

  @NonNull
  private ProxyId bankProxyId;

  @NonNull
  private Amount amount;

  @NonNull
  private String title;

  private String description;


  @Override
  public String requestId() {
    return negotiationId;
  }

  @Override
  public ProxyId signer() {
    return payerProxyId;
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isValid(negotiationId)
        && StringUtils.isValid(proxyUniverse)
        && ProxyUtils.isValid(payerProxyId)
        && ProxyUtils.isValid(bankProxyId)
        && ProxyUtils.isValid(amount)
        && StringUtils.isValid(title);
  }
}
