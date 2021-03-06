package in.yagnyam.proxy.server;

import in.yagnyam.proxy.utils.StringUtils;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataValidation {

  public static void assertNotEmpty(String name, String value) throws BadRequestException {
    if (StringUtils.isEmpty(value)) {
      log.debug("{} can't be null/empty", name);
      badRequest(name + " can't be null/empty");
    }
  }

  public static void assertNotNull(String name, Object o) throws BadRequestException {
    if (o == null) {
      log.debug("{} can't be null/empty", name);
      badRequest(name + " can't be null/empty");
    }
  }

  public static void assertValidSerialNumber(String name, String value) {
    assertNotEmpty(name, value);
    try {
      if (BigInteger.ONE.compareTo(new BigInteger(value)) > 0) {
        log.debug("{} is not a valid certificate number", value);
        badRequest(value + " is not a valid certificate number");
      }
    } catch (NumberFormatException e) {
      log.debug("{} is not a valid certificate number", value);
      badRequest(value + " is not a valid certificate number", e);
    }
  }

  public static void badRequest(String error) throws BadRequestException {
    throw ServiceException.badRequest(error);
  }

  public static void badRequest(String error, Throwable cause) throws BadRequestException {
    throw ServiceException.badRequest(error, cause);
  }

}
