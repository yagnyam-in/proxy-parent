package in.yagnyam.proxy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CertificateChainTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetCertificate_NoCertificates() {
        CertificateChain chain = CertificateChain.builder().certificateSerial("123").build();
        thrown.expect(IllegalStateException.class);
        assertNotNull(chain.getCertificate());
    }

    @Test
    public void testGetCertificate_NoMatchingCertificate() {
        Certificate certificate = mock(Certificate.class);
        when (certificate.getSerialNumber()).thenReturn("456");
        CertificateChain chain = CertificateChain.builder().certificateSerial("123").certificate(certificate).build();
        thrown.expect(IllegalStateException.class);
        assertNotNull(chain.getCertificate());
    }

    @Test
    public void testGetCertificate_MatchingCertificate() {
        Certificate certificate = mock(Certificate.class);
        when (certificate.getSerialNumber()).thenReturn("123");
        CertificateChain chain = CertificateChain.builder().certificateSerial("123").certificate(certificate).build();
        assertEquals("123", chain.getCertificate().getSerialNumber());
    }
}
