package in.yagnyam.digana.payments;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import lombok.*;

import java.util.Date;

@Data
@EqualsAndHashCode(of={"paymentRequestNumber", "requesterNumber"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NonNull
    private String paymentRequestNumber;

    @NonNull
    private Amount amount;

    @NonNull
    private String requesterNumber;

    private String requesterName;

    private AccountNumber requesterAccount;

    private String payerNumber;

    private String payerName;

    private String transactionNumber;

    private String merchantTransactionNumber;

    private String shortDescription;

    private String transactionURL;

    private Date requestDate;

    private String modeOfPayment;
	
	private String status;

    public static PaymentRequest from(Transaction transaction, String paymentRequestNumber) {
        return builder().paymentRequestNumber(paymentRequestNumber)
                .transactionNumber(transaction.getTransactionNumber())
                .merchantTransactionNumber(transaction.getMerchantTransactionNumber())
                .requesterNumber(transaction.getMerchantNumber())
                .requesterName(transaction.getMerchantName())
                .requesterAccount(transaction.getMerchantAccount())
                .shortDescription(transaction.getShortDescription())
                .transactionURL(transaction.getTransactionURL())
                .amount(transaction.getAmount())
                .payerNumber(transaction.getCustomerNumber())
                .payerName(transaction.getCustomerName())
                .requestDate(transaction.getTransactionDate())
                .build();
    }
}
