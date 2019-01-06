package in.yagnyam.proxy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProxyTest {

  @Test
  public void testGetUniqueId() {
    ProxyId proxyId = ProxyId.of("abc", "123");
    Proxy proxy = Proxy.builder()
        .id(proxyId)
        .certificateSerialNumber("123")
        .certificate(mock(Certificate.class)).build();
    assertEquals("abc#123", proxy.getUniqueId());
  }

  @Test
  public void testOf() {
    Certificate certificate = mock(Certificate.class);
    when(certificate.getId()).thenReturn("abc");
    when(certificate.getSha256Thumbprint()).thenReturn("123");
    when(certificate.getSerialNumber()).thenReturn("456");
    when(certificate.getOwner()).thenReturn("owner");

    ProxyId proxyId = ProxyId.of("abc", "123");
    Proxy proxy = Proxy.of(certificate);
    assertEquals(proxyId, proxy.getId());
    assertEquals("456", proxy.getCertificateSerialNumber());
    assertEquals("owner", proxy.getName());
  }
}
