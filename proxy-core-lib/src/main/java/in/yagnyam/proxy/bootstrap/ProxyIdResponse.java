package in.yagnyam.proxy.bootstrap;

import lombok.*;

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
