package in.yagnyam.proxy.services;

import static in.yagnyam.proxy.TestUtils.sampleCertificateForId;
import static in.yagnyam.proxy.TestUtils.sampleCertificateForSerialNumber;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.Certificates;
import in.yagnyam.proxy.services.NetworkService.HttpException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemoteCertificateServiceTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGetCertificate_InvalidArguments() {
    CertificateService certificateService = RemoteCertificateService.builder()
        .networkService(mock(NetworkService.class))
        .pemService(mock(PemService.class))
        .caCertificateRoot("dummy")
        .build();
    thrown.expect(NullPointerException.class);
    certificateService.getCertificate(null, null);
  }

  @Test
  public void testCertificate_NetworkError() throws IOException, HttpException {
    NetworkService networkService = mock(NetworkService.class);
    when(networkService.get(anyString())).thenThrow(new IOException("Error"));

    CertificateService certificateService = RemoteCertificateService.builder()
        .networkService(networkService)
        .pemService(mock(PemService.class))
        .caCertificateRoot("dummy")
        .build();
    assertFalse(certificateService.getCertificate("1", null).isPresent());
  }

  @Test
  public void testCertificate_InvalidCertificate()
      throws IOException, GeneralSecurityException, HttpException {
    Certificate certificate = sampleCertificateForSerialNumber("1", "sha256");

    NetworkService networkService = mock(NetworkService.class);
    when(networkService.getValue(anyString(), any())).thenReturn(certificate);

    PemService pemService = mock(PemService.class);
    when(pemService.decodeCertificate(anyString()))
        .thenThrow(new GeneralSecurityException("Error"));

    CertificateService certificateService = RemoteCertificateService.builder()
        .networkService(networkService)
        .pemService(pemService)
        .caCertificateRoot("dummy")
        .build();
    assertFalse(certificateService.getCertificate("1", null).isPresent());
  }


  @Test
  public void testCertificate_ValidCertificate()
      throws IOException, GeneralSecurityException, HttpException {
    Certificate certificate = sampleCertificateForSerialNumber("1", "sha256");

    NetworkService networkService = mock(NetworkService.class);
    when(networkService.getValue(anyString(), any()))
        .thenReturn(certificate);

    PemService pemService = mock(PemService.class);
    when(pemService.decodeCertificate(eq("Certificate"))).thenReturn(mock(X509Certificate.class));

    CertificateService certificateService = RemoteCertificateService.builder()
        .networkService(networkService)
        .pemService(pemService)
        .caCertificateRoot("dummy")
        .build();
    assertTrue(certificateService.getCertificate("1", null).isPresent());
  }


  @Test
  public void testGetCertificates_InvalidArguments() {
    CertificateService certificateService = RemoteCertificateService.builder()
        .networkService(mock(NetworkService.class))
        .pemService(mock(PemService.class))
        .caCertificateRoot("dummy")
        .build();
    thrown.expect(NullPointerException.class);
    certificateService.getCertificates(null, null);
  }

  @Test
  public void testCertificates_NetworkError() throws IOException, HttpException {
    NetworkService networkService = mock(NetworkService.class);
    when(networkService.get(anyString())).thenThrow(new IOException("Error"));

    CertificateService certificateService = RemoteCertificateService.builder()
        .networkService(networkService)
        .pemService(mock(PemService.class))
        .caCertificateRoot("dummy")
        .build();
    assertTrue(certificateService.getCertificates("1", null).isEmpty());
  }

  @Test
  public void testCertificates_InvalidCertificate()
      throws IOException, GeneralSecurityException, HttpException {
    Certificate certificate = sampleCertificateForId("1", "sha256");

    NetworkService networkService = mock(NetworkService.class);
    when(networkService.getValue(anyString(), any())).thenReturn(certificate);

    PemService pemService = mock(PemService.class);
    when(pemService.decodeCertificate(anyString()))
        .thenThrow(new GeneralSecurityException("Error"));

    CertificateService certificateService = RemoteCertificateService.builder()
        .networkService(networkService)
        .pemService(pemService)
        .caCertificateRoot("dummy")
        .build();
    assertTrue(certificateService.getCertificates("1", null).isEmpty());
  }


  @Test
  public void testCertificates_ValidCertificate()
      throws IOException, GeneralSecurityException, HttpException {
    Certificate certificate = sampleCertificateForSerialNumber("1", "sha256");

    NetworkService networkService = mock(NetworkService.class);
    when(networkService.getValue(anyString(), any()))
        .thenReturn(Certificates.builder().certificate(certificate).build());

    PemService pemService = mock(PemService.class);
    when(pemService.decodeCertificate(eq("Certificate"))).thenReturn(mock(X509Certificate.class));

    CertificateService certificateService = RemoteCertificateService.builder()
        .networkService(networkService)
        .pemService(pemService)
        .caCertificateRoot("dummy")
        .build();
    assertEquals(1, certificateService.getCertificates("1", null).size());
  }


}
