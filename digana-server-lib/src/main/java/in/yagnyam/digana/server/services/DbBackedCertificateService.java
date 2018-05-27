package in.yagnyam.digana.server.services;

import in.yagnyam.digana.server.db.DataStoreCertificateRepository;
import in.yagnyam.digana.services.PemService;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.services.CertificateService;
import in.yagnyam.proxy.services.RemoteCertificateService;
import in.yagnyam.proxy.utils.CertificateUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * {@inheritDoc}
     */
    public Optional<Certificate> getCertificateBySerialNumber(@NonNull String serialNumber) {
        Optional<Certificate> certificate = certificateRepository.getCertificateBySerialNumber(serialNumber);
        if (!certificate.isPresent()) {
            certificate = remoteCertificateService.getCertificateBySerialNumber(serialNumber);
            certificate.ifPresent(certificateRepository::saveCertificate);
        }
        return certificate.map(c -> CertificateUtils.enrichCertificate(c, pemService));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Certificate> getCertificatesById(String certificateId) {
        List<Certificate> certificates = certificateRepository.getCertificatesById(certificateId);
        if (certificates.isEmpty()) {
            certificates = remoteCertificateService.getCertificatesById(certificateId);
            certificates.forEach(certificateRepository::saveCertificate);
        }
        return certificates.stream().map(c -> CertificateUtils.enrichCertificate(c, pemService)).collect(Collectors.toList());
    }


}
