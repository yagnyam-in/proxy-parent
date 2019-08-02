package in.yagnyam.proxy.utils;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyBaseObject;
import in.yagnyam.proxy.services.PemService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ProxyUtils {

    /**
     * Populate the X509 Certificate from encoded certificate of the proxy
     *
     * @param proxy Proxy to Enrich
     * @return Enriched Proxy
     * @throws IllegalArgumentException if any errors while decoding certificate
     */
    public static Proxy enrichCertificate(@NonNull Proxy proxy, PemService pemService) {
        log.debug("enrichCertificate({})", proxy);
        if (proxy.getCertificate() != null) {
            CertificateUtils.enrichCertificate(proxy.getCertificate(), pemService);
        }
        return proxy;
    }

    public static boolean isValid(ProxyBaseObject proxyObject) {
        return proxyObject != null && proxyObject.isValid();
    }
}
