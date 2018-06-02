package in.yagnyam.proxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProxyTest {

    @Test
    public void testGetUniqueId() {
        Proxy proxy = Proxy.builder().id("abc").sha256Thumbprint("123").certificate(mock(Certificate.class)).build();
        assertEquals("abc#123", proxy.getUniqueId());
    }

    @Test
    public void testO() {
        Certificate certificate = mock(Certificate.class);
        when (certificate.getId()).thenReturn("abc");
        when (certificate.getSha256Thumbprint()).thenReturn("123");
        when (certificate.getSerialNumber()).thenReturn("456");
        when (certificate.getOwner()).thenReturn("owner");
        Proxy proxy = Proxy.of(certificate);
        assertEquals("abc", proxy.getId());
        assertEquals("123", proxy.getSha256Thumbprint());
        assertEquals("456", proxy.getCertificateSerialNumber());
        assertEquals("owner", proxy.getName());
    }
}
