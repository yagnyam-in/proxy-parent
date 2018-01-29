package in.yagnyam.digana.payments;

import in.yagnyam.digana.cheque.Cheque;
import lombok.*;

@Data
@EqualsAndHashCode(of={"paymentNumber", "payerNumber"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class PaymentResponse {

    @NonNull
    private String paymentNumber;

    @NonNull
    private String payerNumber;

    private String paymentRequestNumber;

    private String requesterNumber;

    private boolean success;

    private String errorCode;

    private String errorMessage;

    public static PaymentResponse success(String paymentNumber) {
        return builder().success(true).paymentNumber(paymentNumber).build();
    }

    public static PaymentResponse failure(String paymentNumber, String errorMessage) {
        return builder().success(false).paymentNumber(paymentNumber).errorCode("UNKNOWN").errorMessage(errorMessage).build();
    }

}
