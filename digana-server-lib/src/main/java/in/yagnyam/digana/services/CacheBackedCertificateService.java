package in.yagnyam.digana.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import in.yagnyam.digana.model.Certificate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheBackedCertificateService implements CertificateService {

    private final CertificateService certificateService;

    private final Cache<String, Certificate> certificateCache;

    private CacheBackedCertificateService(CertificateService certificateService,
                                          long cacheSize, long cacheTimeout, TimeUnit cacheTimeoutUnit) {
        this.certificateService = certificateService;
        this.certificateCache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterWrite(cacheTimeout, cacheTimeoutUnit)
                .build();
    }

    /**
     * Get Certificate for given Serial Number
     *
     * @param serialNumber Certificate Serial Number
     * @return Certificate associated with given Serial Number
     */
    @Override
    public Optional<Certificate> getCertificate(@NonNull String serialNumber) {
        try {
            // Little ugly, but google Cache doesn't like null values
            Certificate result = certificateCache.getIfPresent(serialNumber);
            if (result == null) {
                result = loadCertificateToCache(serialNumber);
            }
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.error("Unable to fetch certificate for serial " + serialNumber, e);
            return Optional.empty();
        }
    }

    // Manually inserting to Cache, we might be under utilizing Cache functionality
    private Certificate loadCertificateToCache(String serialNumber) {
        Optional<Certificate> result = certificateService.getCertificate(serialNumber);
        result.ifPresent(c -> certificateCache.put(serialNumber, c));
        return result.orElse(null);
    }

    public static Builder builder() {
        return new Builder();
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
            return new CacheBackedCertificateService(certificateService, cacheSize, cacheTimeout, cacheTimeoutUnit);
        }
    }


}
