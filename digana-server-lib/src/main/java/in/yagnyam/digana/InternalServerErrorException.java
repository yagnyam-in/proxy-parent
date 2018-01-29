package in.yagnyam.digana;

import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends ServiceException {

  protected InternalServerErrorException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  protected InternalServerErrorException(String message, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
  }
}
