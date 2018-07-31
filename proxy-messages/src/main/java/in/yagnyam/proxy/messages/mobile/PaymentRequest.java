package in.yagnyam.proxy.messages.mobile;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
