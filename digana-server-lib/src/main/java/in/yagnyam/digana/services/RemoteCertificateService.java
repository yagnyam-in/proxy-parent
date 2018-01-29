package in.yagnyam.digana.services;

import in.yagnyam.digana.model.Certificate;
import in.yagnyam.digana.utils.CertificateUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Optional;

import static in.yagnyam.digana.AppConstants.CERTIFICATE_DOWNLOAD_URL_TEMPLATE;

@Builder
@Slf4j
public class RemoteCertificateService implements CertificateService {

    @NonNull
    private final PemService pemService;

    @NonNull
    private final NetworkService networkService;

    /**
     * Fetches Certificate for given Serial Number from CA
     *
     * @param serialNumber Certificate Serial Number
     * @return Certificate associated with given Serial Number
     */
    public Optional<Certificate> getCertificate(@NonNull String serialNumber) {
        String certificateUrl = certificateUrl(serialNumber);
        try {
            return Optional.of(networkService.getValue(certificateUrl, Certificate.class))
                    .map(c -> CertificateUtils.enrichCertificate(c, pemService));
        } catch (Exception e) {
            log.error("Unable to fetch certificate " + certificateUrl, e);
            return Optional.empty();
        }
    }

    private String certificateUrl(String serialNumber) {
        return MessageFormat.format(CERTIFICATE_DOWNLOAD_URL_TEMPLATE, serialNumber);
    }

}
