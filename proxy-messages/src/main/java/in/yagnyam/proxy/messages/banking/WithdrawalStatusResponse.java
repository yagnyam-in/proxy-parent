package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.AddressableMessage;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
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
public class WithdrawalStatusResponse implements SignableMessage, AddressableMessage {

  @NonNull
  public SignedMessage<WithdrawalStatusRequest> request;

  @NonNull
  private WithdrawalStatusEnum status;

  @Override
  public ProxyId address() {
    return request.getSignedBy();
  }

  @Override
  public ProxyId signer() {
    Withdrawal withdrawal = request.getMessage().getRequest().getMessage();
    return withdrawal.address();
  }

  @JsonIgnore
  public String getRequestId() {
    return request != null && request.getMessage() != null ? request.getMessage().getRequestId()
        : null;
  }@Override
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid()
        && status != null;
  }
}
