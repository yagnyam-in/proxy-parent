package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import lombok.NonNull;

import java.util.List;

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
