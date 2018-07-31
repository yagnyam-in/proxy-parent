package in.yagnyam.proxy.utils;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.services.PemService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CertificateUtilsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

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

  @Test
  public void testEnrichCertificate_NoSecurityExceptions()
      throws GeneralSecurityException, IOException {
    PemService pemService = mock(PemService.class);
    when(pemService.decodeCertificate(anyString()))
        .thenThrow(new GeneralSecurityException("Error"));
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
  public void testEnrichCertificate_NoUnnecessaryDecoding()
      throws GeneralSecurityException, IOException {
    Certificate certificate = sampleCertificateWithUnderlying("2");
    certificate.setCertificate(mock(X509Certificate.class));

    PemService pemService = mock(PemService.class);
    CertificateUtils.enrichCertificate(certificate, pemService);
    verify(pemService, never()).decodeCertificate(anyString());

  }


}