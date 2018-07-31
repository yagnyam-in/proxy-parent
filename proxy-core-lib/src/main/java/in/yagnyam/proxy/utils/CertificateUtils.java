package in.yagnyam.proxy.utils;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.services.PemService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CertificateUtils {

  /**
   * Populate the X509 Certificate from encoded certificate
   *
   * @param certificate Certificate to Enrich
   * @return Enriched Certificate
   * @throws IllegalArgumentException if any errors while decoding certificate
   */
  public static Certificate enrichCertificate(Certificate certificate, PemService pemService) {
    log.debug("enrichCertificate({})", certificate);
    try {
      if (certificate.getCertificate() == null) {
        certificate
            .setCertificate(pemService.decodeCertificate(certificate.getCertificateEncoded()));
      }
      return certificate;
    } catch (GeneralSecurityException | IOException e) {
      log.error("Unable to encrich Certificate", e);
      throw new IllegalArgumentException("invalid certificate", e);
    }
  }

}
