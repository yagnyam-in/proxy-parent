package in.yagnyam.digana.cheque;


import in.yagnyam.digana.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Slf4j
public class PayeeTest {

    @Test
    public void testConstruction() {
        Payee payee = Payee.of("P1");
        log.info("toString: " + payee);
    }


    @Test
    public void testJson() {
        Payee payee = Payee.of("P1");
        String json = JsonUtils.toJson(payee);
        log.info("json: " + json);
        Payee newAccount = JsonUtils.fromJson(json, Payee.class);
        assertEquals(payee, newAccount);
    }


    @Test
    public void testEquals() {
        Payee payee1 = Payee.of("P1");
        assertEquals(payee1, Payee.of("P1"));
        Payee payee1_ = Payee.of("P1");
        payee1_.setName("Dummy");
        assertEquals(payee1, payee1_);
        Payee payee2 = Payee.of("P2");
        assertNotEquals(payee1, payee2);
    }

}
