package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class WithdrawalStatusRequest implements SignableRequestMessage, AddressableMessage {

  @NonNull
  private String requestId;

  @NonNull
  public SignedMessage<Withdrawal> request;

  @Override
  public ProxyId address() {
    return request.getMessage().address();
  }

  @Override
  public ProxyId signer() {
    return request.getSignedBy();
  }@Override
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid();
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @JsonIgnore
  public String getWithdrawalId() {
    return request != null && request.getMessage() != null ? request.getMessage().getWithdrawalId()
        : null;
  }

  @JsonIgnore
  public ProxyAccountId getProxyAccountId() {
    return request != null && request.getMessage() != null ? request.getMessage()
        .getProxyAccountId() : null;
  }

}
