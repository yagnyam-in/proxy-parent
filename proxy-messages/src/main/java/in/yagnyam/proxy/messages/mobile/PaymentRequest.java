package in.yagnyam.proxy.messages.mobile;

import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Getter
public class PaymentRequest {

    private String requestId;

    private String currency;

    private double amount;

    private String payeeId;

    private String payeeName;
}
