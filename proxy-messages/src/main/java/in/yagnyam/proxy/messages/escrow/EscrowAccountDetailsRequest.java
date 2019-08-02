package in.yagnyam.proxy.messages.escrow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    throw new RuntimeException("EscrowAccountDetailsRequest.signer should never be invoked when EscrowAccountDetailsRequest.validSigners is implemented");
  }

  @Override
  public Set<ProxyId> validSigners() {
    EscrowAccount account = escrowAccount.getMessage();
    return new HashSet<>(Arrays.asList(account.getPayerProxyId(), account.getPayeeProxyId(), account.getEscrowProxyId()));
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && escrowAccount != null && escrowAccount.isValid();
  }

  @JsonIgnore
  public EscrowAccountId getProxyAccountId() {
    return escrowAccount != null && escrowAccount.getMessage() != null
        ? escrowAccount.getMessage().getEscrowAccountId() : null;
  }

}
