package in.yagnyam.digana.authorization;


import in.yagnyam.digana.Account;
import in.yagnyam.digana.AccountNumber;
import in.yagnyam.digana.Amount;
import in.yagnyam.digana.cheque.ChequeBook;
import in.yagnyam.digana.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j
public class AccountAuthorizationResponseTest {

    @Test
    public void testCostruction() {
        assertNotNull(AccountAuthorizationResponse.success("", mock(Account.class), Collections.<ChequeBook>emptyList()));
    }

    @Test
    public void testJson() {
        Account account = Account.of(AccountNumber.of("BN", "AN"), Amount.of("INR", 100));
        AccountAuthorizationResponse response = AccountAuthorizationResponse.success("", account, Collections.<ChequeBook>emptyList());
        log.info("Json: " + JsonUtils.toJson(response));
    }
}
