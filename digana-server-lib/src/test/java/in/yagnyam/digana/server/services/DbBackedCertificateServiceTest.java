package in.yagnyam.digana.server.services;

import in.yagnyam.digana.server.db.DataStoreCertificateRepository;
import in.yagnyam.digana.server.model.Certificate;
import in.yagnyam.digana.services.PemService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static in.yagnyam.digana.server.TestUtils.sampleCertificate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DbBackedCertificateServiceTest {

    @Test
    public void testGetCertificate_None() {
        RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);
        when (remoteCertificateService.getCertificate(anyString())).thenReturn(Optional.empty());

        DataStoreCertificateRepository certificateRepository = mock(DataStoreCertificateRepository.class);
        when (certificateRepository.getCertificate(anyString())).thenReturn(Optional.empty());

        DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
                .remoteCertificateService(remoteCertificateService)
                .certificateRepository(certificateRepository)
                .pemService(mock(PemService.class))
                .build();

        assertFalse(dbBackedCertificateService.getCertificate("1").isPresent());
    }


    @Test
    public void testGetCertificate_FromRemote() throws GeneralSecurityException, IOException {
        Certificate certificate = sampleCertificate("1");
        RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);
        when (remoteCertificateService.getCertificate(anyString())).thenReturn(Optional.of(certificate));

        DataStoreCertificateRepository certificateRepository = mock(DataStoreCertificateRepository.class);
        when (certificateRepository.getCertificate(anyString())).thenReturn(Optional.empty());

        PemService pemService = mock(PemService.class);
        when (pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));

        DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
                .remoteCertificateService(remoteCertificateService)
                .certificateRepository(certificateRepository)
                .pemService(pemService)
                .build();

        assertTrue(dbBackedCertificateService.getCertificate("1").isPresent());
        assertNotNull(dbBackedCertificateService.getCertificate("1").get().getCertificate());
    }

    @Test
    public void testGetCertificate_FromDb() throws GeneralSecurityException, IOException {
        Certificate certificate = sampleCertificate("2");

        RemoteCertificateService remoteCertificateService = mock(RemoteCertificateService.class);
        when (remoteCertificateService.getCertificate(anyString())).thenReturn(Optional.empty());

        DataStoreCertificateRepository certificateRepository = mock(DataStoreCertificateRepository.class);
        when (certificateRepository.getCertificate(anyString())).thenReturn(Optional.of(certificate));

        PemService pemService = mock(PemService.class);
        when (pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));

        DbBackedCertificateService dbBackedCertificateService = DbBackedCertificateService.builder()
                .remoteCertificateService(remoteCertificateService)
                .certificateRepository(certificateRepository)
                .pemService(pemService)
                .build();

        // First Call
        assertTrue(dbBackedCertificateService.getCertificate("1").isPresent());
        assertNotNull(dbBackedCertificateService.getCertificate("1").get().getCertificate());
        // Remote service should never called
        verify(remoteCertificateService, never()).getCertificate(anyString());
    }

}
