package in.yagnyam.proxy.messages.mobile;

import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Getter
public class PaymentRequest {

    public static final String REQUEST_PAYMENT_ACTION = "in.yagnyam.proxy.REQUEST_PAYMENT";

    private String requestId;

    private String currency;

    private double amount;

    private String payeeId;

    private String payeeName;
}
