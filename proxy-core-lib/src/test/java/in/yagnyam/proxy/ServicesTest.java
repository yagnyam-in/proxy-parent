package in.yagnyam.proxy;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServicesTest {

    @Test
    public void testIsValidService() {
        assertFalse(Services.isValidService("gambling"));
        assertTrue(Services.isValidService("banking"));
    }
}
