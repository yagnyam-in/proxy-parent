package in.yagnyam.digana.utils;

import in.yagnyam.digana.InternalServerErrorException;
import in.yagnyam.digana.ServiceException;
import in.yagnyam.digana.model.Certificate;
import in.yagnyam.digana.services.PemService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
public final class CertificateUtils {

    /**
     * Populate the X509 Certificate from encoded certificate
     *
     * @param certificate Certificate to Enrich
     * @return Enriched Certificate
     * @throws InternalServerErrorException if any errors while decoding certificate
     */
    public static Certificate enrichCertificate(Certificate certificate, PemService pemService) throws InternalServerErrorException {
        log.debug("enrichCertificate({})", certificate);
        try {
            if (certificate.getCertificate() == null) {
                certificate.setCertificate(pemService.decodeCertificate(certificate.getCertificateEncoded()));
            }
            return certificate;
        } catch (GeneralSecurityException | IOException e) {
            log.error("Unable to encrich Certificate", e);
            throw ServiceException.internalServerError("failed to parse certificate", e);
        }
    }

}
