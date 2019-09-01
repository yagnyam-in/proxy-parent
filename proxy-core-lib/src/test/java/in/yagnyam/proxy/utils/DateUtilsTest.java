package in.yagnyam.proxy.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DateUtilsTest {

  @Test
  public void testToday() {
    assertEquals(DateUtils.now().getDay(), DateUtils.today().getDay());
    assertEquals(DateUtils.now().getMonth(), DateUtils.today().getMonth());
    assertEquals(DateUtils.now().getYear(), DateUtils.today().getYear());
  }
}
