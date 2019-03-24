package in.yagnyam.proxy.messages.registration;

import in.yagnyam.proxy.RequestMessage;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Request to create new Proxy Certificate
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "certificateRequestEncoded")
@EqualsAndHashCode(of = "requestId")
public class ProxyCreationRequest implements RequestMessage {

  /**
   * Unique Request Id. No two requests shall have same request number
   */
  @NonNull
  private String requestId;

  /**
   * Unique Proxy Id
   */
  @NonNull
  private String proxyId;

  /**
   * Pass phrase to de-activate a given Proxy (sha256(ownerProxyId#passPhrase))
   */
  @NonNull
  private String revocationPassPhraseSha256;

  /**
   * Valid Certificate Request for Subject requestId. This is to prevent misusing un-protected
   * endpoint to get new PID
   */
  @NonNull
  private String certificateRequestEncoded;

  @Override
  public String requestId() {
    return requestId;
  }

  public boolean isValid() {
    return StringUtils.isValid(requestId)
        && StringUtils.isValid(proxyId)
        && StringUtils.isValid(revocationPassPhraseSha256)
        && StringUtils.isValid(certificateRequestEncoded);
  }
}
