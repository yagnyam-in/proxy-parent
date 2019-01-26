package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.CertificateChain;
import in.yagnyam.proxy.utils.StringUtils;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * Certificate Service to fetch certificates
 */
public interface CertificateService {

  /**
   * Fetch certificate for given Certificate Id or Serial Number
   *
   * @param certificateId Certificate Id or Certificate Serial Number
   * @param sha256Thumbprint Certificate SHA 256 thumbprint
   * @return Certificate if available otherwise None
   */
  Optional<Certificate> getCertificate(@NonNull String certificateId, String sha256Thumbprint);


  /**
   * Fetch certificate for given Certificate Id or Serial Number
   *
   * @param certificateId Certificate Id or Certificate Serial Number
   * @param sha256Thumbprint Certificate SHA 256 thumbprint
   * @return Certificates matching if available otherwise Empty list
   */
  List<Certificate> getCertificates(@NonNull String certificateId, String sha256Thumbprint);


  /**
   * Fetch certificate chain for given Certificate Id or Serial Number
   *
   * @param certificateId Certificate Id or Certificate Serial Number
   * @param sha256Thumbprint Certificate SHA 256 thumbprint
   * @return CertificateChain for given Certificate Id if available otherwise None
   */
  Optional<CertificateChain> getCertificateChain(@NonNull String certificateId, String sha256Thumbprint);

  static String displayId(String id, String sha256) {
    return StringUtils.isEmpty(sha256) ? id : id + "#" + sha256;
  }

  static Predicate<Certificate> sha256MatcherForCertificate(String sha256) {
    return c -> StringUtils.isEmpty(sha256) || sha256.equals(c.getSha256Thumbprint());
  }


}
