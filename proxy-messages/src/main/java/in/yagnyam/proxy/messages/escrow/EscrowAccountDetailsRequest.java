package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
public class EscrowAccountDetailsRequest implements SignableRequestMessage, AddressableMessage {

  @NonNull
  private String requestId;

  @NonNull
  public SignedMessage<EscrowAccount> escrowAccount;

  @Override
  public ProxyId address() {
    return escrowAccount.getSignedBy();
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId signer() {
    throw new RuntimeException(
        "EscrowAccountDetailsRequest.signer should never be invoked when EscrowAccountDetailsRequest.validSigners is implemented");
  }

  @Override
  public Set<ProxyId> validSigners() {
    EscrowAccount account = escrowAccount.getMessage();
    return new HashSet<>(
        Arrays.asList(account.getPayerProxyId(), account.getPayeeProxyId(), account.getEscrowProxyId()));
  }@Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && ProxyUtils.isValid(escrowAccount);
  }

  @JsonIgnore
  public EscrowAccountId getEscrowAccountId() {
    return escrowAccount.getMessage().getEscrowAccountId();
  }

  @JsonIgnore
  public ProxyId getBankProxyId() {
    return escrowAccount.getMessage().getBankProxyId();
  }

}
