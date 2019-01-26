package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import java.util.Optional;
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
  public Optional<Proxy> resolveProxy(@NonNull ProxyId proxyId) {
    return certificateService.getCertificate(proxyId.getId(), proxyId.getSha256Thumbprint())
        .map(Proxy::of);
  }
}
