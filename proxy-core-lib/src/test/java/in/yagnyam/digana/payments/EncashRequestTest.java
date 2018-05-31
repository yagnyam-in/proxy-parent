package in.yagnyam.digana.payments;

import in.yagnyam.digana.cheque.Cheque;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@Slf4j
public class EncashRequestTest {

    @Test
    public void testOf() {
        try {
            EncashRequest request = EncashRequest.of(null);
            fail("Must throw nullpointer exception");
        } catch (NullPointerException e) {
            log.info("Caught", e);
        }
        assertNotNull(EncashRequest.of(UUID.randomUUID().toString()));
    }

    @Test
    public void testBuilder() {
        try {
            EncashRequest request = EncashRequest.builder().build();
            fail("Must throw nullpointer exception");
        } catch (NullPointerException e) {
            log.info("Caught", e);
        }
        assertNotNull(EncashRequest.builder().requestNumber("Random").cheque(mock(Cheque.class)));

    }
}
