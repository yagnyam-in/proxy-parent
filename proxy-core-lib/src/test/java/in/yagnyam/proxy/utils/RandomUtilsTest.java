package in.yagnyam.proxy.utils;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class RandomUtilsTest {

  @Test
  public void testRandomString() {
    assertNotNull(RandomUtils.randomString());
    assertNotEquals(RandomUtils.randomString(), RandomUtils.randomString());
  }
}
