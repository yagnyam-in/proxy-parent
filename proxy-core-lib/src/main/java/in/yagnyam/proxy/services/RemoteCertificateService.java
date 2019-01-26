package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.CertificateChain;
import in.yagnyam.proxy.Certificates;
import in.yagnyam.proxy.utils.CertificateUtils;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class RemoteCertificateService implements CertificateService {

  @NonNull
  private final PemService pemService;

  @NonNull
  private final NetworkService networkService;

  @NonNull
  private final String caCertificateRoot;

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Certificate> getCertificate(@NonNull String certificateId, String sha256Thumbprint) {
    String certificateUrl = singleCertificateUrl(certificateId, sha256Thumbprint);
    try {
      return Optional.ofNullable(networkService.getValue(certificateUrl, Certificate.class))
          .filter(c -> c.getSerialNumber().equals(certificateId))
          .map(c -> CertificateUtils.enrichCertificate(c, pemService));
    } catch (Exception e) {
      log.error("Unable to fetch certificate " + certificateUrl, e);
      return Optional.empty();
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public List<Certificate> getCertificates(@NonNull String certificateId, String sha256Thumbprint) {
    String certificateUrl = multipleCertificatesUrl(certificateId, sha256Thumbprint);
    try {
      return networkService.getValue(certificateUrl, Certificates.class)
          .getCertificates().stream()
          .map(c -> CertificateUtils.enrichCertificate(c, pemService))
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Unable to fetch certificate " + certificateUrl, e);
      return Collections.emptyList();
    }
  }

  @Override
  public Optional<CertificateChain> getCertificateChain(@NonNull String certificateId, String sha256Thumbprint) {
    String certificateUrl = singleCertificateChainUrl(certificateId, sha256Thumbprint);
    try {
      CertificateChain certificateChain = networkService
          .getValue(certificateUrl, CertificateChain.class);
      certificateChain.getCertificates()
          .forEach(c -> CertificateUtils.enrichCertificate(c, pemService));
      return Optional.of(certificateChain);
    } catch (Exception e) {
      log.error("Unable to fetch certificate " + certificateUrl, e);
      return Optional.empty();
    }
  }

  private String sha256ThumbprintQueryParam(String value) {
    if (StringUtils.isValid(value)) {
      return "?sha256Thumbprint=" + value;
    } else {
      return "";
    }
  }

  private String singleCertificateUrl(@NonNull String certificateId, String sha256Thumbprint) {
    return caCertificateRoot + "/certificate/" + certificateId + sha256ThumbprintQueryParam(sha256Thumbprint);
  }

  private String multipleCertificatesUrl(@NonNull String certificateId, String sha256Thumbprint) {
    return caCertificateRoot + "/certificates/" + certificateId + sha256ThumbprintQueryParam(sha256Thumbprint);
  }

  private String singleCertificateChainUrl(@NonNull String certificateId, String sha256Thumbprint) {
    return caCertificateRoot + "/certificate-chain/" + certificateId + sha256ThumbprintQueryParam(sha256Thumbprint);
  }

}
