package in.yagnyam.proxy.messages.payments;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CurrencyTest {

    @Test
    public void testIsValidCurrency() {
        assertFalse(Currency.isValidCurrency("BTC"));
        assertTrue(Currency.isValidCurrency("EUR"));
    }
}
