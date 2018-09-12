package in.yagnyam.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ProxyIdTest {


  @Test
  public void testEquals() {
    assertProxyIdEquals(ProxyId.of("p1"), ProxyId.of("p1"));
    assertProxyIdEquals(ProxyId.of("p1"), ProxyId.of("p1", "sha256"));
    assertProxyIdEquals(ProxyId.of("p1", "sha256"), ProxyId.of("p1", "sha256"));
    assertProxyIdNotEquals(ProxyId.of("p1", "sha256"), ProxyId.of("p1", "SHA256"));
    assertProxyIdNotEquals(ProxyId.of("p1", "sha256"), ProxyId.of("P1", "sha256"));
    assertProxyIdNotEquals(ProxyId.of("p1"), ProxyId.of("P1"));
    assertProxyIdNotEquals(ProxyId.of("p1"), ProxyId.of("p2"));
  }

  private void assertProxyIdEquals(ProxyId a, ProxyId b) {
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  private void assertProxyIdNotEquals(ProxyId a, ProxyId b) {
    assertNotEquals(a, b);
  }

}
