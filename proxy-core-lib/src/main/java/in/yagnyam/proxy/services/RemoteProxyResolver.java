package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;


@Builder
@Slf4j
public class RemoteProxyResolver implements ProxyResolver {

  @NonNull
  private final PemService pemService;

  @NonNull
  private final NetworkService networkService;

  @NonNull
  private final String proxyFetchUrlRoot;

  @Override
  public Optional<Proxy> resolveProxy(ProxyId proxyId) {
    String certificateUrl = proxyUrl(proxyId);
    try {
      return Optional.ofNullable(networkService.getValue(certificateUrl, Proxy.class))
          .map(p -> ProxyUtils.enrichCertificate(p, pemService));
    } catch (Exception e) {
      log.error("Unable to fetch certificate " + certificateUrl, e);
      return Optional.empty();
    }
  }

  private String sha256ThumbprintQueryParam(String value) {
    if (StringUtils.isValid(value)) {
      return "?sha256Thumbprint=" + value;
    } else {
      return "";
    }
  }

  private String proxyUrl(@NonNull ProxyId proxyId) {
    return proxyFetchUrlRoot + "/" + proxyId.getId()
        + sha256ThumbprintQueryParam(proxyId.getSha256Thumbprint());
  }

}
