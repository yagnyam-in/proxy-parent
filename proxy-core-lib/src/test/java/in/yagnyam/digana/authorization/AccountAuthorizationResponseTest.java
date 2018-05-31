package in.yagnyam.digana.authorization;


import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

@Slf4j
public class AccountAuthorizationResponseTest {

    @Test
    public void testCostruction() {
        assertNotNull(AccountAuthorizationResponse.builder().requestNumber("RN").build());
    }

}
