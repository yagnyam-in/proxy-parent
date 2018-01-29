package in.yagnyam.digana.cheque;


import in.yagnyam.digana.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Slf4j
public class PayerTest {

    @Test
    public void testConstruction() {
        Payer payer = Payer.of("P1");
        log.info("toString: " + payer);
    }


    @Test
    public void testJson() {
        Payer payer = Payer.of("P1");
        String json = JsonUtils.toJson(payer);
        log.info("json: " + json);
        Payer newAccount = JsonUtils.fromJson(json, Payer.class);
        assertEquals(payer, newAccount);
    }


    @Test
    public void testEquals() {
        Payer payer1 =Payer.of("P1");
        assertEquals(payer1, Payer.of("P1"));
        Payer payer1_ = Payer.of("P1");
        payer1_.setName("Dummy");
        assertEquals(payer1, payer1_);
        Payer payer2 = Payer.of("P2");
        assertNotEquals(payer1, payer2);
    }

}
