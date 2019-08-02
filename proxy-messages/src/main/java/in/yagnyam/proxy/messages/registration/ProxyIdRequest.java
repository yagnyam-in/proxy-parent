package in.yagnyam.proxy.messages.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Request to get PID
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "certificateRequestEncoded")
@EqualsAndHashCode(of = "requestId")
public class ProxyIdRequest implements RequestMessage {

  /**
   * Unique Request Number. No two requests shall have same request number
   */
  @NonNull
  private String requestId;

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

  @Override
  @JsonIgnore
  public boolean isValid() {
    return StringUtils.isValid(requestId) && StringUtils.isValid(certificateRequestEncoded);
  }
}
