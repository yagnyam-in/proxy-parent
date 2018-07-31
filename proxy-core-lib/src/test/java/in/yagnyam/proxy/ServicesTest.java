package in.yagnyam.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServicesTest {

  @Test
  public void testIsValidService() {
    assertFalse(Services.isValidService("gambling"));
    assertTrue(Services.isValidService("banking"));
  }
}
