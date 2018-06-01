package in.yagnyam.proxy.server;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ServiceException {

  protected NotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
  }

  protected NotFoundException(String message, Throwable cause) {
    super(HttpStatus.NOT_FOUND, message, cause);
  }
}