package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Proxy;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Certificate Service to fetch certificates
 */
public interface ProxyResolver {

    /**
     * Fetch proxies for given Proxy Id
     * @param pid Proxy Id
     * @return Proxies if available, otherwise Empty List
     */
    List<Proxy> resolveProxy(@NonNull String pid);
}
