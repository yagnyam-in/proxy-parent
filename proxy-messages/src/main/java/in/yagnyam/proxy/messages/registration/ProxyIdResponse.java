package in.yagnyam.proxy.messages.registration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Proxy Id response from Server
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProxyIdResponse {

  /**
   * Request Number
   */
  @NonNull
  private String requestId;

  /**
   * Unique Proxy Id
   */
  @NonNull
  private String proxyId;

}
