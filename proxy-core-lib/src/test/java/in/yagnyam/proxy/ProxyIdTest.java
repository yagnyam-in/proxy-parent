package in.yagnyam.proxy;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProxyIdTest {

  @Test
  public void testEquals() {
    assertEquals(ProxyId.of("p1"), ProxyId.of("p1"));
    assertNotEquals(ProxyId.of("p1"), ProxyId.of("p1", "sha256"));
    assertEquals(ProxyId.of("p1", "sha256"), ProxyId.of("p1", "sha256"));
    assertNotEquals(ProxyId.of("p1", "sha256"), ProxyId.of("p1", "SHA256"));
    assertNotEquals(ProxyId.of("p1", "sha256"), ProxyId.of("P1", "sha256"));
    assertNotEquals(ProxyId.of("p1"), ProxyId.of("P1"));
    assertNotEquals(ProxyId.of("p1"), ProxyId.of("p2"));
  }


  @Test
  public void testIsParentOrEqualsOf() {
    assertTrue(ProxyId.of("p1").canSignOnBehalfOf(ProxyId.of("p1")));
    assertFalse(ProxyId.of("p1").canSignOnBehalfOf(ProxyId.of("p1", "sha256")));
    assertTrue(ProxyId.of("p1", "sha256").canSignOnBehalfOf(ProxyId.of("p1")));
    assertTrue(ProxyId.of("p1", "sha256").canSignOnBehalfOf(ProxyId.any()));
    assertTrue(ProxyId.of("p1", "sha256").canSignOnBehalfOf(ProxyId.of("p1", "sha256")));
    assertTrue(ProxyId.of("p1", "sha256").canSignOnBehalfOf(ProxyId.any()));
  }

}
