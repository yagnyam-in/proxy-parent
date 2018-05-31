package in.yagnyam.proxy.server.db;

import in.yagnyam.proxy.Certificate;
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
    Optional<Certificate> getCertificateBySerialNumber(@NonNull String serialNumber);

    /**
     * Fetch the certificate from Database using SHA256 Thumbprint
     *
     * @param sha256Thumbprint Certificate SHA256 Thumbprint
     * @return Certificates matching Thumbprint
     */
    List<Certificate> getCertificatesBySha256Thumbprint(@NonNull String sha256Thumbprint);

    /**
     * Fetch the certificate from Database using Certificate Id
     *
     * @param certificateId Certificate Id
     * @return Certificates matching Id
     */
    List<Certificate> getCertificatesById(@NonNull String certificateId);

    /**
     * Save the Certificate along with Certificate Request
     *
     * @param certificate Certificate
     */
    void saveCertificate(@NonNull Certificate certificate);
}
