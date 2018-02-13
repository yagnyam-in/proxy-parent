package in.yagnyam.digana.payments;

import in.yagnyam.digana.Amount;
import in.yagnyam.digana.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class PaymentTest {

    @Test
    public void testBuilder() {
        Payment payment = Payment.builder()
                .paymentNumber("PN")
                .payerNumber("Hello")
                .payeeNumber("Payee")
                .amount(Amount.of("INR", 100))
                .build();
        log.info("Payment: " + JsonUtils.toJson(payment));
    }
}
