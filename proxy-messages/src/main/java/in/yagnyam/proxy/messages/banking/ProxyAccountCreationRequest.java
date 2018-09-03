package in.yagnyam.proxy.messages.banking;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;


/**
 * Message to create new Proxy Account
 */
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"requestId"})
public class ProxyAccountCreationRequest implements SignableRequestMessage {

  @NonNull
  private String requestId;

  @NonNull
  private ProxyId proxyId;

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId signer() {
    return proxyId;
  }

  @Override
  public String toReadableString() {
    return String
        .format("With request %s, create new account for proxy id %s ", requestId, proxyId);
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && proxyId != null && proxyId.isValid();
  }
}
