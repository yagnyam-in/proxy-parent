package in.yagnyam.proxy.services;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;

/**
 * Certificate Request Service using Bouncy Castle
 */
@Slf4j
@Builder
public class BcCertificateRequestService implements CertificateRequestService {

    private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();

    static {
        Security.addProvider(BC_PROVIDER);
    }

    @NonNull
    private PemService pemService;

    @Override
    public String createCertificateRequest(String signatureAlgorithm, KeyPair keyPair, X500Principal subject)
            throws GeneralSecurityException {
        try {
            ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
                    .setProvider(BC_PROVIDER)
                    .build(keyPair.getPrivate());
            PKCS10CertificationRequest certificationRequest = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic())
                    .build(signer);
            return pemService.encodeCertificateRequest(certificationRequest);
        } catch (OperatorCreationException | IOException e) {
            log.error("Error while creating certificate request for subject " + subject, e);
            throw new GeneralSecurityException("Error while creating certificate request", e);
        }
    }


}
