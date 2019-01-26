package in.yagnyam.proxy.server.services;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.CertificateChain;
import in.yagnyam.proxy.server.db.DataStoreCertificateRepository;
import in.yagnyam.proxy.services.CertificateService;
import in.yagnyam.proxy.services.PemService;
import in.yagnyam.proxy.services.RemoteCertificateService;
import in.yagnyam.proxy.utils.CertificateUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
  public Optional<Certificate> getCertificate(@NonNull String certificateId,
      String sha256Thumbprint) {
    Optional<Certificate> certificate = certificateRepository
        .getCertificatesById(certificateId, sha256Thumbprint).stream()
        .findFirst();
    if (!certificate.isPresent()) {
      certificate = certificateRepository
          .getCertificateBySerialNumber(certificateId, sha256Thumbprint);
    }
    if (!certificate.isPresent()) {
      certificate = remoteCertificateService.getCertificate(certificateId, sha256Thumbprint);
      certificate.ifPresent(certificateRepository::saveCertificate);
    }
    return certificate.map(c -> CertificateUtils.enrichCertificate(c, pemService));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Certificate> getCertificates(@NonNull String certificateId, String sha256Thumbprint) {
    List<Certificate> certificates = certificateRepository
        .getCertificatesById(certificateId, sha256Thumbprint);
    if (certificates.isEmpty()) {
      certificates = certificateRepository
          .getCertificateBySerialNumber(certificateId, sha256Thumbprint)
          .map(Collections::singletonList)
          .orElse(Collections.emptyList());
    }
    if (certificates.isEmpty()) {
      certificates = remoteCertificateService.getCertificates(certificateId, sha256Thumbprint);
      certificates.forEach(certificateRepository::saveCertificate);
    }
    return certificates.stream().map(c -> CertificateUtils.enrichCertificate(c, pemService))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<CertificateChain> getCertificateChain(@NonNull String certificateId,
      String sha256Thumbprint) {
    // TODO: Use DB as Cache
    return remoteCertificateService.getCertificateChain(certificateId, sha256Thumbprint);
  }


}
