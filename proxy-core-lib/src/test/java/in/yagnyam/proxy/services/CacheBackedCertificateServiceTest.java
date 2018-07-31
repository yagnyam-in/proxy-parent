package in.yagnyam.proxy.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.Certificate;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheBackedCertificateServiceTest {

  private Certificate sampleCertificateForSerialNumber(String serialNumber) {
    Certificate certificate = mock(Certificate.class);
    when(certificate.getSerialNumber()).thenReturn(serialNumber);
    when(certificate.getOwner()).thenReturn("dummy");
    when(certificate.getUniqueId()).thenReturn("dummy");
    return certificate;
  }

  private Certificate sampleCertificateForId(String id) {
    Certificate certificate = mock(Certificate.class);
    when(certificate.getSerialNumber()).thenReturn("dummy");
    when(certificate.getOwner()).thenReturn("dummy");
    when(certificate.getUniqueId()).thenReturn(id);
    return certificate;
  }


  @Test
  public void testGetCertificateBySerialNumber_None() {
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificateBySerialNumber(anyString()))
        .thenReturn(Optional.empty());

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertFalse(certificateService.getCertificateBySerialNumber("1").isPresent());
  }


  @Test
  public void testGetCertificateBySerialNumber_NotInCache() {
    Certificate certificate = sampleCertificateForSerialNumber("1");
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificateBySerialNumber(anyString()))
        .thenReturn(Optional.of(certificate));

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertTrue(certificateService.getCertificateBySerialNumber("1").isPresent());
    assertEquals(certificate, certificateService.getCertificateBySerialNumber("1").get());
  }

  @Test
  public void testGetCertificateBySerialNumber_InCache() {
    Certificate certificate = sampleCertificateForSerialNumber("2");
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificateBySerialNumber(anyString()))
        .thenReturn(Optional.of(certificate));

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertTrue(certificateService.getCertificateBySerialNumber("2").isPresent());
    assertTrue(certificateService.getCertificateBySerialNumber("2").isPresent());
    verify(underlyingCertificateService, times(1)).getCertificateBySerialNumber(anyString());
  }


  @Test
  public void testGetCertificateById_None() {
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificatesById(anyString()))
        .thenReturn(Collections.emptyList());

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertTrue(certificateService.getCertificatesById("1").isEmpty());
  }


  @Test
  public void testGetCertificateById_NotInCache() {
    Certificate certificate = sampleCertificateForId("1");
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificatesById(anyString()))
        .thenReturn(Collections.singletonList(certificate));

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertEquals(1, certificateService.getCertificatesById("1").size());
    assertEquals(certificate, certificateService.getCertificatesById("1").get(0));
  }

  @Test
  public void testGetCertificateById_InCache() {
    Certificate certificate = sampleCertificateForId("2");
    CertificateService underlyingCertificateService = mock(CertificateService.class);
    when(underlyingCertificateService.getCertificatesById(anyString()))
        .thenReturn(Collections.singletonList(certificate));

    CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
        .certificateService(underlyingCertificateService)
        .cacheSize(10)
        .cacheTimeout(10, TimeUnit.SECONDS)
        .build();
    assertFalse(certificateService.getCertificatesById("2").isEmpty());
    assertFalse(certificateService.getCertificatesById("2").isEmpty());
    verify(underlyingCertificateService, times(1)).getCertificatesById(anyString());
  }


}
