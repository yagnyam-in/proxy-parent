package in.yagnyam.proxy.messages.mobile;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.messages.banking.Amount;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Getter
public class PaymentRequest {

  public static final String REQUEST_PAYMENT_ACTION = "in.yagnyam.proxy.REQUEST_PAYMENT";

  private String requestId;

  private ProxyId payeeProxyId;

  private String payeeName;

  @NonNull
  private Amount amount;
}
