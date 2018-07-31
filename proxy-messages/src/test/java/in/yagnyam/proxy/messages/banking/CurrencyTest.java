package in.yagnyam.proxy.messages.banking;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CurrencyTest {

  @Test
  public void testIsValidCurrency() {
    assertFalse(Currency.isValidCurrency("BTC"));
    assertTrue(Currency.isValidCurrency("EUR"));
  }
}
