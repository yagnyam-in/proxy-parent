package in.yagnyam.digana.payments;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import lombok.*;

import java.util.Date;

/**
 * Class to Represent a Business Transaction
 */

@Data
@EqualsAndHashCode(of="transactionNumber")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class Transaction {

    private String transactionNumber;

    private String merchantTransactionNumber;

    private String shortDescription;

    private String transactionURL;

    private String fullDetails;

    @NonNull
    private Amount amount;

    private String merchantNumber;

    private String merchantName;

    private AccountNumber merchantAccount;

    private String customerNumber;

    private String customerName;

    private Date transactionDate;
}
