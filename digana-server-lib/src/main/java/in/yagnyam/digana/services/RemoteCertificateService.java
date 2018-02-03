package in.yagnyam.digana.services;

import in.yagnyam.digana.model.Certificate;
import in.yagnyam.digana.model.CertificateChain;
import in.yagnyam.digana.utils.CertificateUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Optional;

@Builder
@Slf4j
public class RemoteCertificateService implements CertificateService {

    @NonNull
    private final PemService pemService;

    @NonNull
    private final NetworkService networkService;

    @NonNull
    private final String certificateDownloadUrlTemplate;

    /**
     * Fetches Certificate for given Serial Number from CA
     *
     * @param serialNumber Certificate Serial Number
     * @return Certificate associated with given Serial Number
     */
    public Optional<Certificate> getCertificate(@NonNull String serialNumber) {
        String certificateUrl = certificateUrl(serialNumber);
        try {
            return Optional.ofNullable(networkService.getValue(certificateUrl, CertificateChain.class))
                    .flatMap(cc -> cc.getCertificates().stream()
                            .filter(c -> c.getSerialNumber().equals(serialNumber))
                            .map(c -> CertificateUtils.enrichCertificate(c, pemService))
                            .findFirst()
                    );
        } catch (Exception e) {
            log.error("Unable to fetch certificate " + certificateUrl, e);
            return Optional.empty();
        }
    }

    private String certificateUrl(String serialNumber) {
        return MessageFormat.format(certificateDownloadUrlTemplate, serialNumber);
    }

}
