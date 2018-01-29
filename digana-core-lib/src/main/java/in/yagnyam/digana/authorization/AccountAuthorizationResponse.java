package in.yagnyam.digana.authorization;

import in.yagnyam.digana.Account;
import in.yagnyam.digana.cheque.ChequeBook;
import lombok.*;

import java.util.List;

/** Account Authorization Request */
@Data
@EqualsAndHashCode(of = "requestNumber", callSuper = false)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class AccountAuthorizationResponse {

  @NonNull private String requestNumber;

  private boolean success;

  private String errorCode;

  private String errorMessage;

  private Account account;

  private List<ChequeBook> chequeBooks;

  public static AccountAuthorizationResponse success(
      @NonNull String requestNumber,
      @NonNull Account account,
      @NonNull List<ChequeBook> chequeBooks) {
    return AccountAuthorizationResponse.builder()
        .requestNumber(requestNumber)
        .success(true)
        .account(account)
        .chequeBooks(chequeBooks)
        .build();
  }

  public static AccountAuthorizationResponse failure(
      @NonNull String requestNumber, String errorMessage) {
    return AccountAuthorizationResponse.builder()
        .requestNumber(requestNumber)
        .success(false)
        .errorCode("UNKNOWN")
        .errorMessage(errorMessage)
        .build();
  }

  public static AccountAuthorizationResponse failure(
      @NonNull String requestNumber, String errorCode, String errorMessage) {
    return AccountAuthorizationResponse.builder()
        .requestNumber(requestNumber)
        .success(false)
        .errorCode(errorCode)
        .errorMessage(errorMessage)
        .build();
  }
}
