package in.yagnyam.proxy.server.services;

import static in.yagnyam.proxy.server.TestUtils.sampleCertificate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.server.db.DataStoreCertificateRepository;
import in.yagnyam.proxy.services.PemService;
import in.yagnyam.proxy.services.RemoteCertificateService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DbBackedCertificateServiceTest {

  @Test
  public void testGetCertificate_None() {
    RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);
    when(remoteCertificateService.getCertificate("1", "sha256"))
        .thenReturn(Optional.empty());

    DataStoreCertificateRepository certificateRepository = mock(
        DataStoreCertificateRepository.class);
    when(certificateRepository.getCertificateBySerialNumber("1", "sha256"))
        .thenReturn(Optional.empty());
    when(certificateRepository.getCertificatesById("1", "sha256"))
        .thenReturn(Collections.emptyList());

    DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
        .remoteCertificateService(remoteCertificateService)
        .certificateRepository(certificateRepository)
        .pemService(mock(PemService.class))
        .build();

    assertFalse(dbBackedCertificateService.getCertificate("1", "sha256").isPresent());
  }


  @Test
  public void testGetCertificate_FromRemote() throws GeneralSecurityException, IOException {
    Certificate certificate = sampleCertificate("1");
    RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);
    when(remoteCertificateService.getCertificate("1", "sha256"))
        .thenReturn(Optional.of(certificate));

    DataStoreCertificateRepository certificateRepository = mock(
        DataStoreCertificateRepository.class);
    when(certificateRepository.getCertificateBySerialNumber("1", "sha256"))
        .thenReturn(Optional.empty());
    when(certificateRepository.getCertificatesById("1", "sha256"))
        .thenReturn(Collections.emptyList());

    PemService pemService = mock(PemService.class);
    when(pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));

    DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
        .remoteCertificateService(remoteCertificateService)
        .certificateRepository(certificateRepository)
        .pemService(pemService)
        .build();

    Optional<Certificate> result = dbBackedCertificateService.getCertificate("1", "sha256");
    assertTrue(result.isPresent());
    assertNotNull(result.get().getCertificate());
  }

  @Test
  public void testGetCertificate_FromDb() throws GeneralSecurityException, IOException {
    Certificate certificate = sampleCertificate("2");

    RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);

    DataStoreCertificateRepository certificateRepository = mock(
        DataStoreCertificateRepository.class);
    when(certificateRepository.getCertificateBySerialNumber("1", "sha256"))
        .thenReturn(Optional.of(certificate));

    PemService pemService = mock(PemService.class);
    when(pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));

    DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
        .remoteCertificateService(remoteCertificateService)
        .certificateRepository(certificateRepository)
        .pemService(pemService)
        .build();

    // First Call
    Optional<Certificate> result = dbBackedCertificateService.getCertificate("1", "sha256");
    assertTrue(result.isPresent());
    assertNotNull(result.get().getCertificate());

    // Remote service should never called
    verify(remoteCertificateService, never()).getCertificate("1", "sha256");
  }

}
