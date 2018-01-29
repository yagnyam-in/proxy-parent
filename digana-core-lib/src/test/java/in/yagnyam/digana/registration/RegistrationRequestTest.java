package in.yagnyam.digana.registration;


import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@Slf4j
public class RegistrationRequestTest {

    @Test
    public void testConstruction() {
        try {
            RegistrationRequest.builder().build();
            fail("RegistrationRequest must fail without any fields");
        } catch (NullPointerException e) {
            log.info("Caught", e);
        }
        try {
            RegistrationRequest.builder().requestNumber("RequestNumber").build();
            fail("RegistrationRequest must fail without customer number and CSR");
        } catch (NullPointerException e) {
            log.info("Caught", e);
        }
        try {
            RegistrationRequest.builder().customerNumber("CustomerNumber").build();
            fail("RegistrationRequest must fail without registration number and CSR");
        } catch (NullPointerException e) {
            log.info("Caught", e);
        }
        assertNotNull(RegistrationRequest.builder().requestNumber("RequestNumber").customerNumber("CustomerNumber").certificationRequestEncoded("CSR").build());
    }
}
