package in.yagnyam.proxy.services;

import in.yagnyam.proxy.TestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class BcCertificateRequestServiceTest {

    @Test
    public void testCreateCertificateRequest() throws IOException, GeneralSecurityException {
        PemService pemService = PemService.builder().provider(BouncyCastleProvider.PROVIDER_NAME).build();
        CertificateRequestService service = BcCertificateRequestService.builder()
                .pemService(pemService)
                .build();
        KeyPair keyPair = TestUtils.generateKeyPair();
        X500Principal subject = new X500Principal("CN=Hello");
        String certificateRequest = service.createCertificateRequest("SHA256WithRSAEncryption", keyPair, subject);
        PKCS10CertificationRequest cr = pemService.decodeCertificateRequest(certificateRequest);
        assertEquals("CN=Hello", cr.getSubject().toString());
    }
}
