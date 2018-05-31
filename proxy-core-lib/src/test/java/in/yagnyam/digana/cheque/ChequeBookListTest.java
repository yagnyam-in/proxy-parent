package in.yagnyam.digana.cheque;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.fail;

@Slf4j
public class ChequeBookListTest {


    @Test
    public void testConstruction() {

        try {
            ChequeBookList chequeBookList = ChequeBookList.of(null);
            fail("Null arguments shouldn't be supported");
        } catch (NullPointerException | IllegalArgumentException e) {
            log.info("Caught as expected", e);
        }
    }

}
