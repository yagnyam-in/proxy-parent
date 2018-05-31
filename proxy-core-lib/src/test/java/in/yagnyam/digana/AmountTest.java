package in.yagnyam.digana;

import in.yagnyam.proxy.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Slf4j
public class AmountTest {

  @Test
  public void testConstruction() {
    Amount amount = Amount.of(Currency.INR, 100.0);
    assertEquals("Currency must match", Currency.INR, amount.getCurrency());
    assertEquals("Amounts must match", 100.0, amount.getValue(), 0.000001);
    log.info("toString: " + amount);
  }

  @Test
  public void testJson() {
    Amount amount = Amount.of(Currency.INR, 100.0);
    String json = JsonUtils.toJson(amount);
    log.info("json: " + json);
    Amount newAmount = JsonUtils.fromJson(json, Amount.class);
    assertEquals(amount, newAmount);
  }

  @Test
  public void testEquals() {
    Amount amountInr100 = Amount.of(Currency.INR, 100.0);
    assertEquals(amountInr100, Amount.of(Currency.INR, 100.0));
    Amount amountInr200 = Amount.of(Currency.INR, 200.0);
    assertNotEquals(amountInr100, amountInr200);
    Amount amountEur200 = Amount.of(Currency.EUR, 200.0);
    assertNotEquals(amountEur200, amountInr200);
  }
}
