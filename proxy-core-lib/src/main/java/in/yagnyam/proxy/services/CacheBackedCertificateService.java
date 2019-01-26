package in.yagnyam.proxy.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.CertificateChain;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class CacheBackedCertificateService implements CertificateService {

  private final CertificateService certificateService;

  private final Cache<String, Set<Certificate>> certificateCache;

  private CacheBackedCertificateService(CertificateService certificateService,
      long cacheSize, long cacheTimeout, TimeUnit cacheTimeoutUnit) {
    this.certificateService = certificateService;
    this.certificateCache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheTimeout, cacheTimeoutUnit)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Get Certificate for given Serial Number
   *
   * @param certificateId Certificate Serial Number
   * @return Certificate associated with given Serial Number
   */
  @Override
  public Optional<Certificate> getCertificate(@NonNull String certificateId, String sha256Thumbprint) {
    try {
      @Nullable Set<Certificate> cached = certificateCache.getIfPresent(certificateId);
      if (cached != null) {
        List<Certificate> result = cached.stream().
            filter(CertificateService.sha256MatcherForCertificate(sha256Thumbprint))
            .collect(Collectors.toList());
        if (result.size() == 1) {
          return Optional.of(result.get(0));
        } else if (result.size() > 1) {
          throw new RuntimeException("Serial Number " + certificateId + " isn't Unique");
        }
      }
      Optional<Certificate> result = certificateService.getCertificate(certificateId, sha256Thumbprint);
      result.ifPresent(this::addToCache);
      return result;
    } catch (Exception e) {
      log.error("Unable to fetch certificate for Serial number " + certificateId, e);
      return Optional.empty();
    }
  }

  /**
   * Get Certificate for given Certificate Id
   *
   * @param certificateId Certificate Id
   * @return Certificate associated with given Certificate Id
   */
  @Override
  public List<Certificate> getCertificates(@NonNull String certificateId, String sha256Thumbprint) {
    try {
      @Nullable Set<Certificate> cached = certificateCache.getIfPresent(certificateId);
      if (cached != null) {
        List<Certificate> result = cached.stream().
            filter(CertificateService.sha256MatcherForCertificate(sha256Thumbprint))
            .collect(Collectors.toList());
        if (!result.isEmpty()) {
          return result;
        }
      }
      List<Certificate> result = certificateService.getCertificates(certificateId, sha256Thumbprint);
      result.forEach(this::addToCache);
      return result;
    } catch (Exception e) {
      log.error("Unable to fetch certificate for Id " + certificateId, e);
      return Collections.emptyList();
    }
  }

  @Override
  public Optional<CertificateChain> getCertificateChain(String certificateId, String sha256Thumbprint) {
    // TODO: Implement Cache
    return certificateService.getCertificateChain(certificateId, sha256Thumbprint);
  }

  private void addToCache(Certificate certificate) {
    addToCacheForKey(certificate.getId(), certificate);
    addToCacheForKey(certificate.getSerialNumber(), certificate);
  }

  private void addToCacheForKey(String key, Certificate certificate) {
    Set<Certificate> existing = certificateCache.getIfPresent(key);
    if (existing == null) {
      existing = new HashSet<>();
      existing.add(certificate);
      certificateCache.put(key, existing);
    } else {
      existing.add(certificate);
    }
  }

  public static class Builder {

    private int cacheSize = 10;
    private long cacheTimeout = 1;
    private TimeUnit cacheTimeoutUnit = TimeUnit.HOURS;
    private CertificateService certificateService;

    private Builder() {
    }

    public Builder cacheSize(int cacheSize) {
      this.cacheSize = cacheSize;
      return this;
    }

    public Builder certificateService(CertificateService certificateService) {
      this.certificateService = certificateService;
      return this;
    }

    public Builder cacheTimeout(long cacheTimeout, TimeUnit unit) {
      this.cacheTimeout = cacheTimeout;
      this.cacheTimeoutUnit = unit;
      return this;
    }


    public CacheBackedCertificateService build() {
      return new CacheBackedCertificateService(certificateService, cacheSize, cacheTimeout,
          cacheTimeoutUnit);
    }
  }


}
