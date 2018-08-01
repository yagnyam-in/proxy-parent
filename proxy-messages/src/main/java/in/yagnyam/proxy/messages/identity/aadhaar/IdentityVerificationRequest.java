package in.yagnyam.proxy.messages.identity.aadhaar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Request Message to Verify Subject using Aadhaar verification
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@JsonRootName("in.yagnyam.proxy.messages.identity.aadhaar.IdentityVerificationRequest")
public class IdentityVerificationRequest implements SignableRequestMessage {

  /**
   * Unique Request Id
   */
  @NonNull
  private String requestId;

  /**
   * Owner of this Identity.
   * <p>
   * Only owner is allowed to Update the identity details
   */
  @NonNull
  private ProxyId ownerProxyId;

  /**
   * Subject being verified
   */
  @NonNull
  private String subjectId;

  /**
   * TODO: This is worst way of verification. Customer need to send OTP received on mobile for
   * verification.
   */
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
  public String toReadableString() {
    return null;
  }

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && ownerProxyId != null && ownerProxyId.isValid()
        && StringUtils.isValid(subjectId);
  }
}
