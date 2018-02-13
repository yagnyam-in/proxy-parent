package in.yagnyam.digana.server;

import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {

  private final HttpStatus statusCode;

  public HttpStatus getStatusCode() {
    return statusCode;
  }

  protected ServiceException(HttpStatus statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  protected ServiceException(HttpStatus statusCode, String message, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public static BadRequestException badRequest(String message) {
    return new BadRequestException(message);
  }

  public static BadRequestException badRequest(String message, Throwable cause) {
    return new BadRequestException(message, cause);
  }

  public static NotFoundException notFound(String message) {
    return new NotFoundException(message);
  }

  public static UnauthorizedException unauthorized(String message) {
    return new UnauthorizedException(message);
  }

  public static UnauthorizedException unauthorized(String message, Throwable cause) {
    return new UnauthorizedException(message, cause);
  }

  public static InternalServerErrorException internalServerError(String message) {
    return new InternalServerErrorException(message);
  }

  public static InternalServerErrorException internalServerError(String message, Throwable cause) {
    return new InternalServerErrorException(message, cause);
  }
}
