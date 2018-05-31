package in.yagnyam.digana.authorization;

import in.yagnyam.digana.Account;
import in.yagnyam.digana.cheque.ChequeBook;
import lombok.*;

import java.util.List;

/**
 * Account Authorization Request
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@Getter
@ToString
@EqualsAndHashCode(of = "requestNumber", callSuper = false)
public class AccountAuthorizationResponse {

    @NonNull
    private String requestNumber;

    @Singular
    private List<Account> accounts;

    @Singular
    private List<ChequeBook> chequeBooks;

}
