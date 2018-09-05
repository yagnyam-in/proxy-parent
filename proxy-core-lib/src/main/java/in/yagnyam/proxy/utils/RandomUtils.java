package in.yagnyam.proxy.utils;

import java.math.BigInteger;
import java.util.Random;

public class RandomUtils {

  /**
   * Simple Random String generator
   * @return Random String
   */
  public static String randomString() {
    return new BigInteger(64, new Random()).toString(32);
  }

}
