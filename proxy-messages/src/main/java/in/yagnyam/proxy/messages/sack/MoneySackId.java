package in.yagnyam.proxy.messages.sack;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.ProxyBaseObject;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MoneySackId implements ProxyBaseObject {

  @NonNull
  private String proxyUniverse;

  @NonNull
  private ProxyId sackProviderProxyId;

  @NonNull
  private String id;

  @JsonIgnore
  @Override
  public boolean isValid() {
    return StringUtils.isValid(proxyUniverse)
        && ProxyUtils.isValid(sackProviderProxyId)
        && StringUtils
        .isValid(id);
  }
}
