package in.yagnyam.digana.cheque;

import in.yagnyam.digana.Amount;
import in.yagnyam.digana.Currency;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class ChequeBookTest {

    @Test
    public void testConstruction() {
        ChequeBook chequeBook = ChequeBookBuilder.builder().bank(Bank.of("BANK", "Bank Name"))
                .chequeBookNumber(1)
                .initialChequeNumber(1)
                .maximumAmount(Amount.of(Currency.INR, 100.0))
                .numberOfCheques(10)
                .payer(Payer.of("Payer"))
                .validityInMonths(10)
                .build();
        log.info("Cheque Book: " + chequeBook);
    }
}
