package in.yagnyam.proxy.authentication;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.yagnyam.proxy.services.CryptographyService;
import in.yagnyam.proxy.services.NetworkService;
import in.yagnyam.proxy.services.PemService;
import lombok.extern.slf4j.Slf4j;

import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RemoteCertificateResolver implements CertificateResolver {

    private static final String CERTIFICATE_DOWNLOAD_URL = "https://yagnyam-ca.appspot.com/download-certificate?serialNumber={0}";

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int cacheSize = 10;
        private String urlTemplate = CERTIFICATE_DOWNLOAD_URL;
        private long cacheTimeout = 1;
        private TimeUnit cacheTimeoutUnit = TimeUnit.HOURS;
        private NetworkService networkService = NetworkService.builder().build();
        private PemService pemService = PemService.builder().cryptographyService(CryptographyService.instance()).build();
        private Builder() {
        }

        public Builder cacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        public Builder urlTemplate(String urlTemplate) {
            this.urlTemplate = urlTemplate;
            return this;
        }

        public Builder cacheTimeout(long cacheTimeout, TimeUnit unit) {
            this.cacheTimeout = cacheTimeout;
            this.cacheTimeoutUnit = unit;
            return this;
        }

        public Builder networkService(NetworkService networkService) {
            this.networkService = networkService;
            return this;
        }

        public Builder pemService(PemService pemService) {
            this.pemService = pemService;
            return this;
        }

        public RemoteCertificateResolver build() {
            return new RemoteCertificateResolver(networkService,
                    pemService,
                    urlTemplate,
                    cacheSize,
                    cacheTimeout,
                    cacheTimeoutUnit);
        }
    }

    private final LoadingCache<String, X509Certificate> certificateCache;

    private RemoteCertificateResolver(NetworkService networkService,
                                      PemService pemService,
                                      String urlTemplate,
                                      int cacheSize,
                                      long cacheTimeout,
                                      TimeUnit cacheTimeoutUnit) {

        CacheLoader<String, X509Certificate> certificateLoader = new CacheLoader<String, X509Certificate>() {
            @Override
            public X509Certificate load(String serialNumber) throws Exception {
                return pemService.decodeCertificate(networkService.get(MessageFormat.format(urlTemplate, serialNumber)));
            }
        };

        this.certificateCache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterWrite(cacheTimeout, cacheTimeoutUnit)
                .build(certificateLoader);

    }

    @Override
    public Optional<X509Certificate> getCertificate(String serialNumber) {
        try {
            return Optional.of(certificateCache.get(serialNumber));
        } catch (ExecutionException e) {
            log.error("Failed to fetch certificate for " + serialNumber, e);
            return Optional.empty();
        }
    }
}
