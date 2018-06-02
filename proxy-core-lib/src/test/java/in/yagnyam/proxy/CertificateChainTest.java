package in.yagnyam.proxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CertificateChainTest {

    @Test
    public void testGetCertificate_NoCertificates() {
        CertificateChain chain = CertificateChain.builder().build();
        assertFalse(chain.getCertificate("dummy").isPresent());
    }

    @Test
    public void testGetCertificate_NoMatchingCertificate() {
        Certificate certificate = mock(Certificate.class);
        when (certificate.matchesId(anyString())).thenReturn(false);
        CertificateChain chain = CertificateChain.builder().certificate(certificate).build();
        assertFalse(chain.getCertificate("dummy").isPresent());
    }

    @Test
    public void testGetCertificate_MatchingCertificate() {
        Certificate certificate = mock(Certificate.class);
        when (certificate.matchesId(anyString())).thenReturn(true);
        CertificateChain chain = CertificateChain.builder().certificate(certificate).build();
        assertTrue(chain.getCertificate("dummy").isPresent());
    }
}
