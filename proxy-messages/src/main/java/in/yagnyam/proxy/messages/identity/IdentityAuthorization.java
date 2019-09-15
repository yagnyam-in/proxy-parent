package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
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
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(of = {"proxyIdentity", "requestId"})
@JsonRootName("in.yagnyam.proxy.messages.identity.IdentityAuthorization")
public class IdentityAuthorization implements SignableRequestMessage {

  @NonNull
  public SignedMessage<ProxyIdentity> proxyIdentity;

  @NonNull
  private String requestId;

  private boolean consentToMail;

  private boolean consentToRequestPayments;

  private boolean consentToVoiceCall;

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId signer() {
    return proxyIdentity.getMessage().getProxyId();
  }@Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && proxyIdentity != null
        && proxyIdentity.isValid();
  }
}
