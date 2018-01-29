package in.yagnyam.digana.utils;

import in.yagnyam.digana.InternalServerErrorException;
import in.yagnyam.digana.ServiceException;
import in.yagnyam.digana.model.Certificate;
import in.yagnyam.digana.services.PemService;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface CertificateUtils {

    /**
     * Populate the X509 Certificate from encoded certificate
     *
     * @param certificate Certificate to Enrich
     * @return Enriched Certificate
     * @throws InternalServerErrorException if any errors while decoding certificate
     */
    static Certificate enrichCertificate(Certificate certificate, PemService pemService) throws InternalServerErrorException {
        try {
            if (certificate.getCertificate() == null) {
                certificate.setCertificate(pemService.decodeCertificate(certificate.getCertificateEncoded()));
            }
            return certificate;
        } catch (GeneralSecurityException | IOException e) {
            throw ServiceException.internalServerError("failed to parse certificate", e);
        }
    }

}
