package in.yagnyam.proxy.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.CertificateChain;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class CacheBackedCertificateService implements CertificateService {

  private final CertificateService certificateService;

  private final Cache<String, Certificate> certificateCacheBySerialNumber;

  private final Cache<String, List<Certificate>> certificateCacheById;

  private CacheBackedCertificateService(CertificateService certificateService,
      long cacheSize, long cacheTimeout, TimeUnit cacheTimeoutUnit) {
    this.certificateService = certificateService;
    this.certificateCacheBySerialNumber = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheTimeout, cacheTimeoutUnit)
        .build();

    this.certificateCacheById = CacheBuilder.newBuilder()
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
   * @param serialNumber Certificate Serial Number
   * @return Certificate associated with given Serial Number
   */
  @Override
  public Optional<Certificate> getCertificateBySerialNumber(@NonNull String serialNumber) {
    try {
      @Nullable Certificate cached = certificateCacheBySerialNumber.getIfPresent(serialNumber);
      if (cached != null) {
        return Optional.of(cached);
      }
      Optional<Certificate> result = certificateService.getCertificateBySerialNumber(serialNumber);
      result.ifPresent((c) -> certificateCacheBySerialNumber.put(serialNumber, c));
      return result;
    } catch (Exception e) {
      log.error("Unable to fetch certificate for Serial number " + serialNumber, e);
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
  public List<Certificate> getCertificatesById(@NonNull String certificateId) {
    try {
      @Nullable List<Certificate> result = certificateCacheById.getIfPresent(certificateId);
      if (result == null || result.isEmpty()) {
        result = certificateService.getCertificatesById(certificateId);
        if (!result.isEmpty()) {
          certificateCacheById.put(certificateId, result);
        }
      }
      return result;
    } catch (Exception e) {
      log.error("Unable to fetch certificate for Id " + certificateId, e);
      return Collections.emptyList();
    }
  }

  @Override
  public Optional<CertificateChain> getCertificateChain(String certificateId) {
    // TODO: Implement Cache
    return certificateService.getCertificateChain(certificateId);
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
