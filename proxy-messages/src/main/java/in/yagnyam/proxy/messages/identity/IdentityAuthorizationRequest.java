package in.yagnyam.proxy.messages.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
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
 * Identity authorization message
 */
@Builder
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"requestId"})
@JsonRootName("in.yagnyam.proxy.messages.identity.IdentityAuthorizationRequest")
public class IdentityAuthorizationRequest implements SignableRequestMessage {

  @NonNull
  private String requestId;

  @NonNull
  private ProxyId proxyId;

  @NonNull
  private ProxyId ownerProxyId;

  @NonNull
  private String subjectId;

  private boolean revealNationality;

  private boolean revealName;

  private boolean revealGender;

  private boolean revealDateOfBirth;

  private boolean revealAge;

  private boolean revealIs18Plus;

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId signer() {
    return ownerProxyId;
  }@Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && proxyId != null && proxyId.isValid()
        && ownerProxyId != null && ownerProxyId.isValid()
        && StringUtils.isValid(subjectId);
  }
}
