package in.yagnyam.proxy.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EncodingUtilsTest {


  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testToBase56_Negative() {
    thrown.expect(IllegalArgumentException.class);
    EncodingUtils.asBase56(-1);
  }


  @Test
  public void testToBase56_Zero() {
    thrown.expect(IllegalArgumentException.class);
    EncodingUtils.asBase56(0);
  }


  @Test
  public void testToBase56() {
    assertEquals("b4", EncodingUtils.asBase56(145));
    assertEquals("jtC", EncodingUtils.asBase56(34145));
  }
}
