package in.yagnyam.digana.payments;

import in.yagnyam.digana.Account;
import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.cheque.Cheque;
import lombok.*;

@Data
@EqualsAndHashCode(of="requestNumber")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
@Builder
public class EncashRequest {

    @NonNull
    private String requestNumber;

    private Cheque cheque;

    private String chequeContent;

    private AccountNumber defaultAccount;

    /*
    public static EncashRequest of(String requestNumber, Cheque cheque) {
        return builder().requestNumber(requestNumber).cheque(cheque).build();
    }

    public static EncashRequest of(String requestNumber, Cheque cheque, AccountNumber defaultAccount) {
        return builder().requestNumber(requestNumber).cheque(cheque).defaultAccount(defaultAccount).build();
    }
    */

}
