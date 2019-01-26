package in.yagnyam.proxy.services;

import static in.yagnyam.proxy.TestUtils.sampleCertificateForSerialNumber;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.Certificate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheBackedCertificateServiceTest {

  @Test
  public void testGetCertificate_None() {
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificate("1", null))
        .thenReturn(Optional.empty());

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertFalse(certificateService.getCertificate("1", null).isPresent());
  }


  @Test
  public void testGetCertificate_NotInCache() {
    Certificate certificate = sampleCertificateForSerialNumber("1", "sha256");
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificate("1", null))
        .thenReturn(Optional.of(certificate));

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertTrue(certificateService.getCertificate("1", null).isPresent());
    assertEquals(certificate, certificateService.getCertificate("1", null).get());
  }

  @Test
  public void testGetCertificate_InCache() {
    Certificate certificate = sampleCertificateForSerialNumber("2", "sha256");
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificate("2", null))
        .thenReturn(Optional.of(certificate));

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertTrue(certificateService.getCertificate("2", null).isPresent());
    assertTrue(certificateService.getCertificate("2", null).isPresent());
    verify(underlyingCertificateService, times(1)).getCertificate("2", null);
  }




}
