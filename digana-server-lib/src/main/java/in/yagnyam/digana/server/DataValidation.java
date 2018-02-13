package in.yagnyam.digana.server;

import in.yagnyam.digana.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

@Slf4j
public class DataValidation {

  public static void assertNotEmpty(String name, String value) throws BadRequestException {
    if (StringUtils.isEmpty(value)) {
      log.debug("{} can't be null/empty", name);
      throw ServiceException.badRequest(name + " can't be null/empty");
    }
  }

  public static void assertNotNull(String name, Object o) throws BadRequestException {
    if (o == null) {
      log.debug("{} can't be null/empty", name);
      throw ServiceException.badRequest(name + " can't be null/empty");
    }
  }

  public static void assertValidSerialNumber(String name, String value) {
    assertNotEmpty(name, value);
    try {
      if (BigInteger.ONE.compareTo(new BigInteger(value)) > 0) {
        log.debug("{} is not a valid certificate number", value);
        throw ServiceException.badRequest(value + " is not a valid certificate number");
      }
    } catch (NumberFormatException e) {
      log.debug("{} is not a valid certificate number", value);
      throw ServiceException.badRequest(value + " is not a valid certificate number", e);
    }
  }
}
