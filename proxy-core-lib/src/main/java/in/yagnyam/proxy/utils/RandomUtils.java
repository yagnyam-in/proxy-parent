package in.yagnyam.proxy.utils;

import java.security.SecureRandom;
import java.util.UUID;

public class RandomUtils {

  public static long randomNumber(int length) {
    if (length == 0) {
      throw new IllegalArgumentException("Length must be at least 1");
    } else if (length > 18) {
      throw new IllegalArgumentException("Length must be less than 19");
    }
    return (new SecureRandom().nextLong()) % (long) Math.pow(10, length);
  }

  /**
   * Simple Random Id generator
   *
   * @return Random Id
   */
  public static String randomId() {
    String id;
    do {
      id = UUID.randomUUID().toString().substring(19);
    } while (!Character.isAlphabetic(id.charAt(0)));
    return id;
  }

}
