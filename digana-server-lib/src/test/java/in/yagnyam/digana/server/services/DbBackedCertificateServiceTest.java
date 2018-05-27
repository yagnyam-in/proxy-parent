package in.yagnyam.digana.server.services;

import in.yagnyam.digana.server.db.DataStoreCertificateRepository;
import in.yagnyam.digana.services.PemService;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.services.RemoteCertificateService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static in.yagnyam.digana.server.TestUtils.sampleCertificate;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DbBackedCertificateServiceTest {

    @Test
    public void testGetCertificate_None() {
        RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);
        when (remoteCertificateService.getCertificateBySerialNumber(anyString())).thenReturn(Optional.empty());

        DataStoreCertificateRepository certificateRepository = mock(DataStoreCertificateRepository.class);
        when (certificateRepository.getCertificateBySerialNumber(anyString())).thenReturn(Optional.empty());

        DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
                .remoteCertificateService(remoteCertificateService)
                .certificateRepository(certificateRepository)
                .pemService(mock(PemService.class))
                .build();

        assertFalse(dbBackedCertificateService.getCertificateBySerialNumber("1").isPresent());
    }


    @Test
    public void testGetCertificate_FromRemote() throws GeneralSecurityException, IOException {
        Certificate certificate = sampleCertificate("1");
        RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);
        when (remoteCertificateService.getCertificateBySerialNumber(anyString())).thenReturn(Optional.of(certificate));

        DataStoreCertificateRepository certificateRepository = mock(DataStoreCertificateRepository.class);
        when (certificateRepository.getCertificateBySerialNumber(anyString())).thenReturn(Optional.empty());

        PemService pemService = mock(PemService.class);
        when (pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));

        DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
                .remoteCertificateService(remoteCertificateService)
                .certificateRepository(certificateRepository)
                .pemService(pemService)
                .build();

        assertTrue(dbBackedCertificateService.getCertificateBySerialNumber("1").isPresent());
        assertNotNull(dbBackedCertificateService.getCertificateBySerialNumber("1").get().getCertificate());
    }

    @Test
    public void testGetCertificate_FromDb() throws GeneralSecurityException, IOException {
        Certificate certificate = sampleCertificate("2");

        RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);
        when (remoteCertificateService.getCertificateBySerialNumber(anyString())).thenReturn(Optional.empty());

        DataStoreCertificateRepository certificateRepository = mock(DataStoreCertificateRepository.class);
        when (certificateRepository.getCertificateBySerialNumber(anyString())).thenReturn(Optional.of(certificate));

        PemService pemService = mock(PemService.class);
        when (pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));

        DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
                .remoteCertificateService(remoteCertificateService)
                .certificateRepository(certificateRepository)
                .pemService(pemService)
                .build();

        // First Call
        assertTrue(dbBackedCertificateService.getCertificateBySerialNumber("1").isPresent());
        assertNotNull(dbBackedCertificateService.getCertificateBySerialNumber("1").get().getCertificate());
        // Remote service should never called
        verify(remoteCertificateService, never()).getCertificateBySerialNumber(anyString());
    }

}
