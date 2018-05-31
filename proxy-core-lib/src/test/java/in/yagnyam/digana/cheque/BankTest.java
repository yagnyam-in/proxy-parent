package in.yagnyam.digana.cheque;

import in.yagnyam.proxy.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Bank Number class to represent an Bank
 */
@Slf4j
public class BankTest {


    @Test
    public void testConstruction() {
        Bank bank = Bank.of("BANK1", "Bank One");
        log.info("toString: " + bank);
    }

    @Test
    public void testJson() {
        Bank bank = Bank.of("B1", "Bank One");
        String json = JsonUtils.toJson(bank);
        log.info("json: " + json);
        Bank newBank = JsonUtils.fromJson(json, Bank.class);
        assertEquals(bank, newBank);
    }


    @Test
    public void testEquals() {
        Bank bankB1 = Bank.of("B1", "Bank One");
        assertEquals(bankB1, Bank.of("B1", "Bank One"));
        Bank bankB1_ = Bank.of("B1", "Bank Another");
        assertEquals(bankB1, bankB1_);
        Bank bankB2 = Bank.of("B2", "Bank Two");
        assertNotEquals(bankB2, bankB1);
        assertNotEquals(bankB2, bankB1_);
    }


}
