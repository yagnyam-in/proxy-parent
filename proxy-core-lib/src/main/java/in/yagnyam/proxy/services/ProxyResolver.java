package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import java.util.Optional;
import lombok.NonNull;

/**
 * Service to fetch proxy for given proxy id
 */
public interface ProxyResolver {

  /**
   * Fetch proxy for given Proxy Id
   *
   * @param proxyId Proxy Id
   * @return Proxy if available, otherwise None
   */
  Optional<Proxy> resolveProxy(@NonNull ProxyId proxyId);
}
