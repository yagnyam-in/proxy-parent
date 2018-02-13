package in.yagnyam.digana.server.utils;

import in.yagnyam.digana.server.InternalServerErrorException;
import in.yagnyam.digana.server.model.Certificate;
import in.yagnyam.digana.services.PemService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import static in.yagnyam.digana.server.TestUtils.sampleCertificate;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class CertificateUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void enrichCertificate_NoSecurityExceptions() throws GeneralSecurityException, IOException {
        PemService pemService = mock(PemService.class);
        when(pemService.decodeCertificate(anyString())).thenThrow(new GeneralSecurityException("Error"));
        thrown.expect(InternalServerErrorException.class);
        CertificateUtils.enrichCertificate(mock(Certificate.class), pemService);
    }

    @Test
    public void enrichCertificate() throws GeneralSecurityException, IOException {
        Certificate certificate = sampleCertificate("1");
        PemService pemService = mock(PemService.class);
        when(pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));
        assertNotNull(CertificateUtils.enrichCertificate(certificate, pemService).getCertificate());
    }

    @Test
    public void enrichCertificate_NoUnnecessaryDecoding() throws GeneralSecurityException, IOException {
        Certificate certificate = sampleCertificate("1");
        certificate.setCertificate(mock(X509Certificate.class));

        PemService pemService = mock(PemService.class);
        CertificateUtils.enrichCertificate(certificate, pemService);
        verify(pemService, never()).decodeCertificate(anyString());

    }


}