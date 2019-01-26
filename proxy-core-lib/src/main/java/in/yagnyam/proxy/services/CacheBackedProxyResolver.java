package in.yagnyam.proxy.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.ProxyId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class CacheBackedProxyResolver implements ProxyResolver {

  private final ProxyResolver proxyResolver;

  private final Cache<ProxyId, Proxy> proxyCache;

  private CacheBackedProxyResolver(ProxyResolver proxyResolver,
      long cacheSize, long cacheTimeout, TimeUnit cacheTimeoutUnit) {
    this.proxyResolver = proxyResolver;
    this.proxyCache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheTimeout, cacheTimeoutUnit)
        .build();
  }

  @Override
  public Optional<Proxy> resolveProxy(@NonNull ProxyId proxyId) {
    @Nullable Proxy cached = proxyCache.getIfPresent(proxyId);
    if (cached != null) {
      return Optional.of(cached);
    }
    Optional<Proxy> proxyFromUnderlyingResolver = proxyResolver.resolveProxy(proxyId);
    proxyFromUnderlyingResolver.ifPresent(p -> proxyCache.put(proxyId, p));
    return proxyFromUnderlyingResolver;
  }


  public static class Builder {

    private int cacheSize = 10;
    private long cacheTimeout = 1;
    private TimeUnit cacheTimeoutUnit = TimeUnit.HOURS;
    private ProxyResolver proxyResolver;

    private Builder() {
    }

    public CacheBackedProxyResolver.Builder cacheSize(int cacheSize) {
      this.cacheSize = cacheSize;
      return this;
    }

    public CacheBackedProxyResolver.Builder certificateService(
        @NonNull ProxyResolver proxyResolver) {
      this.proxyResolver = proxyResolver;
      return this;
    }

    public CacheBackedProxyResolver.Builder cacheTimeout(long cacheTimeout,
        @NonNull TimeUnit unit) {
      this.cacheTimeout = cacheTimeout;
      this.cacheTimeoutUnit = unit;
      return this;
    }


    public CacheBackedProxyResolver build() {
      return new CacheBackedProxyResolver(proxyResolver, cacheSize, cacheTimeout,
          cacheTimeoutUnit);
    }
  }

}
