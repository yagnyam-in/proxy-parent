package in.yagnyam.proxy.services;

import javax.security.auth.x500.X500Principal;
import java.security.GeneralSecurityException;
import java.security.KeyPair;


/**
 * Service for creating Certificate Requests
 */
public interface CertificateRequestService {

    /**
     * Create Certificate Request for given
     *
     * @param signatureAlgorithm Signature Algorithm to sign the CSR
     * @param keyPair            Cryptographic Key Pair used for creating CSR
     * @param subject            Subject for which the CSR need to be created
     * @return CSR in PEM format
     * @throws GeneralSecurityException if any error while creating CSR
     */
    String createCertificateRequest(String signatureAlgorithm, KeyPair keyPair, X500Principal subject)
            throws GeneralSecurityException;

}
