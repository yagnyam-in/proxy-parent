package in.yagnyam.digana.services;

import in.yagnyam.digana.model.Certificate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static in.yagnyam.digana.TestUtils.sampleCertificate;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CacheBackedCertificateServiceTest {


    @Test
    public void testGetCertificate_None() {
        CertificateService underlyingCertificateService = mock(CertificateService.class);
        when(underlyingCertificateService.getCertificate(anyString())).thenReturn(Optional.empty());

        CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
                .certificateService(underlyingCertificateService)
                .cacheSize(10)
                .cacheTimeout(10, TimeUnit.SECONDS)
                .build();
        assertFalse(certificateService.getCertificate("1").isPresent());
    }



    @Test
    public void testGetCertificate_NotInCache() {
        Certificate certificate = sampleCertificate("1");
        CertificateService underlyingCertificateService = mock(CertificateService.class);
        when(underlyingCertificateService.getCertificate(anyString())).thenReturn(Optional.of(certificate));

        CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
                .certificateService(underlyingCertificateService)
                .cacheSize(10)
                .cacheTimeout(10, TimeUnit.SECONDS)
                .build();
        assertTrue(certificateService.getCertificate("1").isPresent());
        assertEquals(certificate, certificateService.getCertificate("1").get());
    }

    @Test
    public void testGetCertificate_InCache() {
        Certificate certificate = sampleCertificate("1");
        CertificateService underlyingCertificateService = mock(CertificateService.class);
        when(underlyingCertificateService.getCertificate(anyString())).thenReturn(Optional.of(certificate));

        CacheBackedCertificateService certificateService = CacheBackedCertificateService.builder()
                .certificateService(underlyingCertificateService)
                .cacheSize(10)
                .cacheTimeout(10, TimeUnit.SECONDS)
                .build();
        assertTrue(certificateService.getCertificate("1").isPresent());
        assertTrue(certificateService.getCertificate("1").isPresent());
        verify(underlyingCertificateService, times(1)).getCertificate(anyString());
    }



}
