package in.yagnyam.proxy.server;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ServiceException {

  private static final long serialVersionUID = 1L;

  protected ForbiddenException(String message) {
    super(HttpStatus.FORBIDDEN, message);
  }

  protected ForbiddenException(String message, Throwable cause) {
    super(HttpStatus.FORBIDDEN, message, cause);
  }
}
