package in.yagnyam.digana.server.services;

import in.yagnyam.digana.server.model.Certificate;
import lombok.NonNull;

import java.util.Optional;

/**
 * Certificate Service to fetch certificates
 */
public interface CertificateService {

    /**
     * Fetch certificate for given Certificate Serial Number
     * @param serialNumber Certificate Serial Number
     * @return Certificate if available otherwise None
     */
    Optional<Certificate> getCertificate(@NonNull String serialNumber);
}
