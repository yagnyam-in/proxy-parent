package in.yagnyam.proxy.server.services;

import in.yagnyam.proxy.server.TestUtils;
import in.yagnyam.proxy.server.db.PrivateKeyRepository;
import in.yagnyam.proxy.server.model.PrivateKeyEntity;
import in.yagnyam.proxy.services.PemService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PrivateKeyServiceTest {

    @Mock
    private PrivateKeyRepository privateKeyRepository;

    @Mock
    private PemService pemService;

    @InjectMocks
    private PrivateKeyService privateKeyService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void testGetIssuer_InvalidArguments() {
        expectedException.expect(NullPointerException.class);
        privateKeyService.getPrivateKey(null);
    }

    @Test
    public void testGetIssuer_None() {
        when(privateKeyRepository.getPrivateKeyEntity(anyString())).thenReturn(Optional.empty());
        assertFalse(privateKeyService.getPrivateKey("1").isPresent());
    }

    @Test
    public void testGetIssuer_Valid() {
        PrivateKeyEntity privateKeyEntity = TestUtils.samplePrivateKeyEntity("1");
        when(privateKeyRepository.getPrivateKeyEntity(eq("1"))).thenReturn(Optional.of(privateKeyEntity));
        assertTrue(privateKeyService.getPrivateKey("1").isPresent());
        assertEquals(privateKeyEntity, privateKeyService.getPrivateKey("1").get());
    }

    @Test
    public void testGetIssuer_Enrich() throws GeneralSecurityException, IOException {
        PrivateKeyEntity privateKeyEntity = TestUtils.samplePrivateKeyEntity("1");
        privateKeyEntity.setPrivateKeyEncoded("PKE");
        privateKeyEntity.setCertificateEncoded("CE");

        when(privateKeyRepository.getPrivateKeyEntity(eq("1"))).thenReturn(Optional.of(privateKeyEntity));
        when(pemService.decodePrivateKey(eq("PKE"))).thenReturn(mock(PrivateKey.class));
        when(pemService.decodeCertificate(eq("CE"))).thenReturn(mock(X509Certificate.class));

        Optional<PrivateKeyEntity> actual = privateKeyService.getPrivateKey("1");
        assertTrue(actual.isPresent());
        assertEquals(privateKeyEntity, actual.get());
        assertNotNull(actual.get().getPrivateKey());
        assertNotNull(actual.get().getCertificate());
    }


}