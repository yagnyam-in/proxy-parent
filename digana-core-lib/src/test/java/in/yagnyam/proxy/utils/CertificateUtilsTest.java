package in.yagnyam.proxy.utils;

import in.yagnyam.digana.services.PemService;
import in.yagnyam.proxy.Certificate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class CertificateUtilsTest {

    private Certificate sampleCertificateWithUnderlying(String encodedCertificate) {
        return Certificate.builder()
                .serialNumber("1234")
                .owner("Owner")
                .sha256Thumbprint("SHA256")
                .subject("SUB")
                .certificateEncoded(encodedCertificate)
                .validFrom(new Date())
                .validTill(new Date())
                .build();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEnrichCertificate_NoSecurityExceptions() throws GeneralSecurityException, IOException {
        PemService pemService = mock(PemService.class);
        when(pemService.decodeCertificate(anyString())).thenThrow(new GeneralSecurityException("Error"));
        thrown.expect(IllegalArgumentException.class);
        CertificateUtils.enrichCertificate(mock(Certificate.class), pemService);
    }

    @Test
    public void testEnrichCertificate() throws GeneralSecurityException, IOException {
        Certificate certificate = sampleCertificateWithUnderlying("1");
        PemService pemService = mock(PemService.class);
        when(pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));
        assertNotNull(CertificateUtils.enrichCertificate(certificate, pemService).getCertificate());
    }

    @Test
    public void testEnrichCertificate_NoUnnecessaryDecoding() throws GeneralSecurityException, IOException {
        Certificate certificate = sampleCertificateWithUnderlying("2");
        certificate.setCertificate(mock(X509Certificate.class));

        PemService pemService = mock(PemService.class);
        CertificateUtils.enrichCertificate(certificate, pemService);
        verify(pemService, never()).decodeCertificate(anyString());

    }


}