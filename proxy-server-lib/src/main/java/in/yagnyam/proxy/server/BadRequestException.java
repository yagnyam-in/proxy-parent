package in.yagnyam.proxy.server;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ServiceException {

  private static final long serialVersionUID = 1L;

  protected BadRequestException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }

  protected BadRequestException(String message, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, message, cause);
  }
}
