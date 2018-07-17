package in.yagnyam.proxy.server;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ServiceException {

  private static final long serialVersionUID = 1L;

  protected UnauthorizedException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }

  protected UnauthorizedException(String message, Throwable cause) {
    super(HttpStatus.UNAUTHORIZED, message, cause);
  }
}
