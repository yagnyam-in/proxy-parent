package in.yagnyam.proxy.services;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.CertificateChain;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RemoteCertificateServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetCertificateBySerialNumber_InvalidArguments() {
        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(mock(NetworkService.class))
                .pemService(mock(PemService.class))
                .certificateByIdUrlTemplate("dummy")
                .certificateBySerialNumberUrlTemplate("dummy")
                .build();
        thrown.expect(NullPointerException.class);
        certificateService.getCertificateBySerialNumber(null);
    }

    @Test
    public void testCertificateBySerialNumber_NetworkError() throws IOException {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.get(anyString())).thenThrow(new IOException("Error"));

        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(networkService)
                .pemService(mock(PemService.class))
                .certificateByIdUrlTemplate("dummy")
                .certificateBySerialNumberUrlTemplate("dummy")
                .build();
        assertFalse(certificateService.getCertificateBySerialNumber("1").isPresent());
    }

    @Test
    public void testCertificateBySerialNumber_InvalidCertificate() throws IOException, GeneralSecurityException {
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
                .certificateByIdUrlTemplate("dummy")
                .certificateBySerialNumberUrlTemplate("dummy")
                .build();
        assertFalse(certificateService.getCertificateBySerialNumber("1").isPresent());
    }


    @Test
    public void testCertificateBySerialNumber_ValidCertificate() throws IOException, GeneralSecurityException {
        Certificate certificate = mock(Certificate.class);
        when(certificate.getSerialNumber()).thenReturn("1");
        when(certificate.getCertificateEncoded()).thenReturn("Certificate");

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getValue(anyString(), any()))
                .thenReturn(CertificateChain.builder().certificateSerial("1").certificate(certificate).build());

        PemService pemService = mock(PemService.class);
        when(pemService.decodeCertificate(eq("Certificate"))).thenReturn(mock(X509Certificate.class));

        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(networkService)
                .pemService(pemService)
                .certificateByIdUrlTemplate("dummy")
                .certificateBySerialNumberUrlTemplate("dummy")
                .build();
        assertTrue(certificateService.getCertificateBySerialNumber("1").isPresent());
    }




    @Test
    public void testGetCertificateById_InvalidArguments() {
        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(mock(NetworkService.class))
                .pemService(mock(PemService.class))
                .certificateByIdUrlTemplate("dummy")
                .certificateBySerialNumberUrlTemplate("dummy")
                .build();
        thrown.expect(NullPointerException.class);
        certificateService.getCertificatesById(null);
    }

    @Test
    public void testCertificateById_NetworkError() throws IOException {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.get(anyString())).thenThrow(new IOException("Error"));

        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(networkService)
                .pemService(mock(PemService.class))
                .certificateByIdUrlTemplate("dummy")
                .certificateBySerialNumberUrlTemplate("dummy")
                .build();
        assertTrue(certificateService.getCertificatesById("1").isEmpty());
    }

    @Test
    public void testCertificateById_InvalidCertificate() throws IOException, GeneralSecurityException {
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
                .certificateByIdUrlTemplate("dummy")
                .certificateBySerialNumberUrlTemplate("dummy")
                .build();
        assertTrue(certificateService.getCertificatesById("1").isEmpty());
    }


    @Test
    public void testCertificateById_ValidCertificate() throws IOException, GeneralSecurityException {
        Certificate certificate = mock(Certificate.class);
        when(certificate.getSerialNumber()).thenReturn("1");
        when(certificate.getCertificateEncoded()).thenReturn("Certificate");
        when(certificate.matchesId(anyString())).thenReturn(true);

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getValue(anyString(), any()))
                .thenReturn(CertificateChain.builder().certificate(certificate).build());

        PemService pemService = mock(PemService.class);
        when(pemService.decodeCertificate(eq("Certificate"))).thenReturn(mock(X509Certificate.class));

        CertificateService certificateService = RemoteCertificateService.builder()
                .networkService(networkService)
                .pemService(pemService)
                .certificateByIdUrlTemplate("dummy")
                .certificateBySerialNumberUrlTemplate("dummy")
                .build();
        assertEquals(1, certificateService.getCertificatesById("1").size());
    }


}
