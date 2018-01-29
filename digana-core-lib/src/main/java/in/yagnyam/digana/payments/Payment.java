package in.yagnyam.digana.payments;

import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import in.yagnyam.digana.cheque.Cheque;
import lombok.*;

import java.util.Date;

@Data
@EqualsAndHashCode(of={"paymentNumber", "payerNumber"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class Payment {

    @NonNull
    private String paymentNumber;

    @NonNull
    private String payerNumber;

    private String payerName;

    // @NonNull
    private Cheque cheque;

    private String chequeContent;

    private Date paymentDate;

    @NonNull
    private Amount amount;

    private String paymentRequestNumber;

    @NonNull
    private String payeeNumber;

    private String payeeName;

    private AccountNumber payeeAccount;

    private String transactionNumber;

    private String merchantTransactionNumber;

    private String shortDescription;

    private String transactionURL;

    private Date requestDate;

    private String status;

    public static Payment from(PaymentRequest paymentRequest, String paymentNumber) {
        return builder().paymentNumber(paymentNumber)
                .paymentRequestNumber(paymentRequest.getPaymentRequestNumber())
                .transactionNumber(paymentRequest.getTransactionNumber())
                .merchantTransactionNumber(paymentRequest.getMerchantTransactionNumber())
                .payeeNumber(paymentRequest.getRequesterNumber())
                .payeeName(paymentRequest.getRequesterName())
                .payeeAccount(paymentRequest.getRequesterAccount())
                .shortDescription(paymentRequest.getShortDescription())
                .transactionURL(paymentRequest.getTransactionURL())
                .amount(paymentRequest.getAmount())
                .payerNumber(paymentRequest.getPayerNumber())
                .payerName(paymentRequest.getPayerName())
                .requestDate(paymentRequest.getRequestDate())
                .build();
    }

}
