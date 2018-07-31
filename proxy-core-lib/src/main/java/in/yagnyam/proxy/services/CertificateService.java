package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.CertificateChain;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;

/**
 * Certificate Service to fetch certificates
 */
public interface CertificateService {

  /**
   * Fetch certificate for given Certificate Serial Number
   *
   * @param serialNumber Certificate Serial Number
   * @return Certificate if available otherwise None
   */
  Optional<Certificate> getCertificateBySerialNumber(@NonNull String serialNumber);


  /**
   * Fetch certificate for given Certificate Id
   *
   * @param certificateId Certificate Id
   * @return Certificates matching if available otherwise Empty list
   */
  List<Certificate> getCertificatesById(@NonNull String certificateId);


  /**
   * Fetch certificate chain for given Certificate Id
   *
   * @param certificateId Certificate Id (Serial Number or Certificate Id)
   * @return CertificateChain for given Certificate Id if available otherwise None
   */
  Optional<CertificateChain> getCertificateChain(@NonNull String certificateId);


}
