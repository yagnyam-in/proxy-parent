package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import java.util.List;
import lombok.NonNull;

/**
 * Certificate Service to fetch certificates
 */
public interface ProxyResolver {

  /**
   * Fetch proxies for given Proxy Id
   *
   * @param proxyId Proxy Id
   * @return Proxies if available, otherwise Empty List
   */
  List<Proxy> resolveProxy(@NonNull ProxyId proxyId);
}
