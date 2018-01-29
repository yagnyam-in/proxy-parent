package in.yagnyam.digana.services;

import in.yagnyam.digana.db.DataStoreCertificateRepository;
import in.yagnyam.digana.model.Certificate;
import in.yagnyam.digana.utils.CertificateUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class DbBackedCertificateService implements CertificateService {

    @NonNull
    private final RemoteCertificateService remoteCertificateService;

    @NonNull
    private final DataStoreCertificateRepository certificateRepository;

    @NonNull
    private final PemService pemService;

    @Builder
    private DbBackedCertificateService(@NonNull RemoteCertificateService remoteCertificateService,
                                       @NonNull DataStoreCertificateRepository certificateRepository,
                                       @NonNull PemService pemService) {
        this.remoteCertificateService = remoteCertificateService;
        this.certificateRepository = certificateRepository;
        this.pemService = pemService;
    }

    /**
     * Get Certificate for given Serial Number
     *
     * @param serialNumber Certificate Serial Number
     * @return Certificate associated with given Serial Number
     */
    public Optional<Certificate> getCertificate(@NonNull String serialNumber) {
        Optional<Certificate> certificate = certificateRepository.getCertificate(serialNumber);
        if (!certificate.isPresent()) {
            certificate = remoteCertificateService.getCertificate(serialNumber);
            certificate.ifPresent(certificateRepository::saveCertificate);
        }
        return certificate.map(c -> CertificateUtils.enrichCertificate(c, pemService));
    }



}
