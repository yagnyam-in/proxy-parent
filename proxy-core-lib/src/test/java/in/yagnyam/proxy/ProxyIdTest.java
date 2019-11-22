package in.yagnyam.proxy;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProxyIdTest {

    private void expectException(Runnable runnable, Class expected) {
        try {
            runnable.run();
            fail("Expecting an exception of type " + expected);
        } catch (Exception actual) {
            assertEquals(expected, actual.getClass());
        }
    }

    @Test
    public void testConstruction() {
        assertEquals(ProxyId.of("p1#sha256"), ProxyId.of("p1", "sha256"));
        expectException(() -> ProxyId.of((String) null), IllegalArgumentException.class);
        expectException(() -> ProxyId.of(""), IllegalArgumentException.class);
      expectException(() -> ProxyId.of("#"), IllegalArgumentException.class);
        expectException(() -> ProxyId.of("#sha256"), IllegalArgumentException.class);
    }

    @Test
    public void testUniqueId() {
        assertEquals(ProxyId.of("p1", "sha256").uniqueId(), "p1#sha256");
        assertEquals(ProxyId.of("p1").uniqueId(), "p1");
    }


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
