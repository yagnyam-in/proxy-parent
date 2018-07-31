package in.yagnyam.proxy.messages.registration;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class CustomerRegistrationRequestTest {

  @Test
  public void testConstruction() {
    try {
      CustomerRegistrationRequest.builder().build();
      fail("CustomerRegistrationRequest must fail without any fields");
    } catch (NullPointerException e) {
      log.info("Caught", e);
    }
    try {
      CustomerRegistrationRequest.builder().requestId("rid").build();
      fail("CustomerRegistrationRequest must fail without customer number and CSR");
    } catch (NullPointerException e) {
      log.info("Caught", e);
    }
    try {
      CustomerRegistrationRequest.builder().proxyId("pid").build();
      fail("CustomerRegistrationRequest must fail without registration number and CSR");
    } catch (NullPointerException e) {
      log.info("Caught", e);
    }
    assertNotNull(CustomerRegistrationRequest.builder().requestId("rid").proxyId("pid")
        .certificateRequestEncoded("CSR").build());
  }
}
