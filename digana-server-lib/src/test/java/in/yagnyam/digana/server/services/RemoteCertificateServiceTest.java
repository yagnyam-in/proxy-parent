package in.yagnyam.digana.server.services;

import in.yagnyam.digana.server.model.Certificate;
import in.yagnyam.digana.server.model.CertificateChain;
import in.yagnyam.digana.services.NetworkService;
import in.yagnyam.digana.services.PemService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RemoteCertificateServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testGetCertificate_InvalidArguments() {
        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(mock(NetworkService.class))
                .pemService(mock(PemService.class))
                .certificateDownloadUrlTemplate("dummy")
                .build();
        thrown.expect(NullPointerException.class);
        certificateService.getCertificate(null);
    }

    @Test
    public void testGetCertificate_NetworkError() throws IOException {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.get(anyString())).thenThrow(new IOException("Error"));

        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(networkService)
                .pemService(mock(PemService.class))
                .certificateDownloadUrlTemplate("dummy")
                .build();
        assertFalse(certificateService.getCertificate("1").isPresent());
    }

    @Test
    public void testGetCertificate_InvalidCertificate() throws IOException, GeneralSecurityException {
        Certificate certificate = mock(Certificate.class);
        when(certificate.getSerialNumber()).thenReturn("1");
        when(certificate.getCertificateEncoded()).thenReturn("Certificate");

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getValue(anyString(), any())).thenReturn(certificate);

        PemService pemService = mock(PemService.class);
        when(pemService.decodeCertificate(anyString())).thenThrow(new GeneralSecurityException("Error"));

        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(networkService)
                .pemService(pemService)
                .certificateDownloadUrlTemplate("dummy")
                .build();
        assertFalse(certificateService.getCertificate("1").isPresent());
    }


    @Test
    public void testGetCertificate_ValidCertificate() throws IOException, GeneralSecurityException {
        Certificate certificate = mock(Certificate.class);
        when(certificate.getSerialNumber()).thenReturn("1");
        when(certificate.getCertificateEncoded()).thenReturn("Certificate");

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getValue(anyString(), any()))
                .thenReturn(CertificateChain.builder().serialNumber("1").certificate(certificate).build());

        PemService pemService = mock(PemService.class);
        when(pemService.decodeCertificate(eq("Certificate"))).thenReturn(mock(X509Certificate.class));

        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(networkService)
                .pemService(pemService)
                .certificateDownloadUrlTemplate("dummy")
                .build();
        assertTrue(certificateService.getCertificate("1").isPresent());
    }

}
