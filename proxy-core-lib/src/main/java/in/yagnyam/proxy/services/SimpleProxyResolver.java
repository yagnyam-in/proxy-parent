package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import lombok.Builder;
import lombok.NonNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Certificate Service to fetch certificates
 */
@Builder
public class SimpleProxyResolver implements ProxyResolver {

    @NonNull
    private CertificateService certificateService;

    /**
     * Fetch proxy for given Proxy Id
     * @param pid Proxy Id
     * @return Proxy if available, otherwise None
     */
    public List<Proxy> resolveProxy(@NonNull String pid) {
        return certificateService.getCertificatesById(pid).stream().map(Proxy::of).collect(Collectors.toList());
    }
}
