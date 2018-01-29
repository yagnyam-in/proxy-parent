package in.yagnyam.digana;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@Slf4j
public class ChequeBookRequestTest {

  @Test
  public void testConstruction() {
    try {
      ChequeBookRequest.builder().build();
      fail("Must throw null pointer exception");
    } catch (NullPointerException e) {
      log.info("Caught Exception", e);
    }
    assertNotNull(ChequeBookRequest.builder().requestNumber("ABC").accountNumber("ABC").build());
  }
}
