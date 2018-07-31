package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.CertificateChain;
import in.yagnyam.proxy.utils.CertificateUtils;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class RemoteCertificateService implements CertificateService {

  @NonNull
  private final PemService pemService;

  @NonNull
  private final NetworkService networkService;

  @NonNull
  private final String certificateByIdUrlTemplate;

  @NonNull
  private final String certificateBySerialNumberUrlTemplate;

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Certificate> getCertificateBySerialNumber(@NonNull String serialNumber) {
    String certificateUrl = certificateUrlBySerialNumber(serialNumber);
    try {
      return Optional.ofNullable(networkService.getValue(certificateUrl, Certificate.class))
          .filter(c -> c.getSerialNumber().equals(serialNumber))
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
  public List<Certificate> getCertificatesById(@NonNull String certificateId) {
    String certificateUrl = certificateUrlById(certificateId);
    try {
      return networkService.getValue(certificateUrl, CertificateChain.class)
          .getCertificates().stream()
          .filter(c -> c.matchesId(certificateId))
          .map(c -> CertificateUtils.enrichCertificate(c, pemService))
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Unable to fetch certificate " + certificateUrl, e);
      return Collections.emptyList();
    }
  }


  private String certificateUrlBySerialNumber(String serialNumber) {
    return MessageFormat.format(certificateBySerialNumberUrlTemplate, serialNumber);
  }


  @SneakyThrows
  private String certificateUrlById(String certificateId) {
    return MessageFormat
        .format(certificateByIdUrlTemplate, URLEncoder.encode(certificateId, "UTF-8"));
  }

}
