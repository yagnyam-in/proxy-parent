package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.Constants;
import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Response for Proxy Creation
 */
@Getter
@ToString
@EqualsAndHashCode(of = {"request", "proxy"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ProxyCreationResponse implements SignableMessage {

  @NonNull
  private ProxyCreationRequest request;

  @NonNull
  private Proxy proxy;

  @Override
  public ProxyId signer() {
    return ProxyId.of(Constants.PROXY_CENTRAL);
  }

  @Override
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid()
        && proxy != null && proxy.isValid();
  }

}
