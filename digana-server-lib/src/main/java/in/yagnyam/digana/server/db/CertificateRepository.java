package in.yagnyam.digana.server.db;

import in.yagnyam.digana.server.model.Certificate;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Interface to persist Certificate Locally for faster retrieval
 */
public interface CertificateRepository {

    /**
     * Fetch the certificate from Database
     *
     * @param serialNumber Certificate Serial Number
     * @return Certificate with given Serial Number
     */
    Optional<Certificate> getCertificate(@NonNull String serialNumber);

    /**
     * Fetch the certificate from Database using SHA256 Thumbprint
     *
     * @param sha256Thumbprint Certificate SHA256 Thumbprint
     * @return Certificates matching Thumbprint
     */
    List<Certificate> getCertificatesBySha256Thumbprint(@NonNull String sha256Thumbprint);

    /**
     * Save the Certificate along with Certificate Request
     *
     * @param certificate Certificate
     */
    void saveCertificate(@NonNull Certificate certificate);
}
