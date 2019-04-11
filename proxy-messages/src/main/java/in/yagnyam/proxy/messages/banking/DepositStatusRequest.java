package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.messages.banking.ProxyAccountId;
import in.yagnyam.proxy.messages.banking.Withdrawal;
import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class DepositStatusRequest implements SignableRequestMessage, AddressableMessage {

  @NonNull
  private String requestId;

  @NonNull
  public SignedMessage<DepositRequest> request;

  @Override
  public ProxyId address() {
    return request.getMessage().address();
  }

  @Override
  public ProxyId signer() {
    return request.getSignedBy();
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid();
  }

  @Override
  public String requestId() {
    return requestId;
  }

  @JsonIgnore
  public String getDepositId() {
    return request != null && request.getMessage() != null ? request.getMessage().getDepositId()
        : null;
  }

  @JsonIgnore
  public ProxyAccountId getProxyAccountId() {
    return request != null && request.getMessage() != null ? request.getMessage()
        .getProxyAccountId() : null;
  }

}
