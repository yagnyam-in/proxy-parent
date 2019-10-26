package in.yagnyam.proxy.messages.identity.self;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IdentityVerificationRequest implements SignableRequestMessage {

  @NonNull
  private String requestId;

  @NonNull
  private ProxyId ownerProxyId;

  @NonNull
  private String subjectId;

  @NonNull
  private Boolean verified;

  @Override
  public String requestId() {
    return requestId;
  }

  @Override
  public ProxyId signer() {
    return ownerProxyId;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && ProxyUtils.isValid(ownerProxyId)
        && StringUtils.isValid(subjectId);
  }
}
