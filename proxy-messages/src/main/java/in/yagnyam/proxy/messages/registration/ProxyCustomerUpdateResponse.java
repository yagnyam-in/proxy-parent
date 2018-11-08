package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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

/**
 * Acknowledge by Proxy Central to update Customer details for given Proxy Id
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProxyCustomerUpdateResponse implements SignableMessage, AddressableMessage {

  @NonNull
  public SignedMessage<ProxyCustomerUpdateRequest> request;

  @Override
  public ProxyId signer() {
    return request.getMessage().address();
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
  public ProxyId address() {
    return request.getMessage().getProxyId();
  }
}
