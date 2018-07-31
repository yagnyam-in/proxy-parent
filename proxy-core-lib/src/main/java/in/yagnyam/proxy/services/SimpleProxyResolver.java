package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;

/**
 * Certificate Service to fetch certificates
 */
@Builder
public class SimpleProxyResolver implements ProxyResolver {

  @NonNull
  private CertificateService certificateService;

  /**
   * Fetch proxy for given Proxy Id
   *
   * @param proxyId Proxy Id
   * @return Proxy if available, otherwise None
   */
  public List<Proxy> resolveProxy(@NonNull ProxyId proxyId) {
    return certificateService.getCertificatesById(proxyId.getUniqueId()).stream()
        .map(Proxy::of)
        .collect(Collectors.toList());
  }
}
