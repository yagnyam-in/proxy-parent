package in.yagnyam.proxy.utils;

import java.util.UUID;

public class RandomUtils {

  /**
   * Simple Random Id generator
   *
   * @return Random Id
   */
  public static String randomId() {
    String id;
    do {
      id = UUID.randomUUID().toString().substring(19);
    } while(!Character.isAlphabetic(id.charAt(0)));
    return id;
  }

}
