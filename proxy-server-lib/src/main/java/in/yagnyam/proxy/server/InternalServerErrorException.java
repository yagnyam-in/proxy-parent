package in.yagnyam.proxy.server;

import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends ServiceException {

  private static final long serialVersionUID = 1L;

  protected InternalServerErrorException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  protected InternalServerErrorException(String message, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
  }
}
