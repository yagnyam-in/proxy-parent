package in.yagnyam.proxy.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.proxy.Constants;
import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableMessage;
import lombok.*;

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
  @JsonIgnore
  public boolean isValid() {
    return request != null && request.isValid()
        && proxy != null && proxy.isValid();
  }

}
