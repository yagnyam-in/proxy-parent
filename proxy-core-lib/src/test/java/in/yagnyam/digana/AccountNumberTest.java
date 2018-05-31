package in.yagnyam.digana;

import in.yagnyam.proxy.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/** AccountNumber Number class to represent an AccountNumber */
@Slf4j
public class AccountNumberTest {

  @Test
  public void testConstruction() {
    AccountNumber account = AccountNumber.of("BANK1", "ACCOUNT1");
    log.info("toString: " + account);
  }

  @Test
  public void testJson() {
    AccountNumber account = AccountNumber.of("B1", "A1");
    String json = JsonUtils.toJson(account);
    log.info("json: " + json);
    AccountNumber newAccount = JsonUtils.fromJson(json, AccountNumber.class);
    assertEquals(account, newAccount);
  }

  @Test
  public void testEquals() {
    AccountNumber accountB1A1 = AccountNumber.of("B1", "A1");
    assertEquals(accountB1A1, AccountNumber.of("B1", "A1"));
    AccountNumber accountB1A2 = AccountNumber.of("B1", "A2");
    assertNotEquals(accountB1A1, accountB1A2);
    AccountNumber accountB2A1 = AccountNumber.of("B2", "A1");
    assertNotEquals(accountB2A1, accountB1A2);
  }
}
