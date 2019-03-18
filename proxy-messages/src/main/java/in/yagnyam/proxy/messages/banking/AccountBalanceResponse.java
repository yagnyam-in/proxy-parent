package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
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

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class AccountBalanceResponse implements SignableMessage, AddressableMessage {

  @NonNull
  public SignedMessage<AccountBalanceRequest> request;

  @NonNull
  private Amount balance;

  @Override
  public ProxyId address() {
    return request.getSignedBy();
  }

  @Override
  public ProxyId signer() {
    return request.getMessage().getProxyAccount().getSignedBy();
  }

  @JsonIgnore
  public String getRequestId() {
    return request != null && request.getMessage() != null ? request.getMessage().getRequestId() : null;
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid()
        && balance != null && balance.isValid();
  }
}
