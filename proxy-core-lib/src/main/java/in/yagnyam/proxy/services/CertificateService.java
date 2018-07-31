package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Certificate;
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
   * @return Certificate if available otherwise None
   */
  List<Certificate> getCertificatesById(@NonNull String certificateId);


}
