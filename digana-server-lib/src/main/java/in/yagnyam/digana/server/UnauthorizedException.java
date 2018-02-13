package in.yagnyam.digana.server;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ServiceException {

  protected UnauthorizedException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }

  protected UnauthorizedException(String message, Throwable cause) {
    super(HttpStatus.UNAUTHORIZED, message, cause);
  }
}
