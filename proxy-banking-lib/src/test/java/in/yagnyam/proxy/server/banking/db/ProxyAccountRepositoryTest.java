package in.yagnyam.proxy.server.banking.db;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.server.banking.model.OriginalAccountEntity;
import in.yagnyam.proxy.server.banking.model.ProxyAccountEntity;
import in.yagnyam.proxy.utils.DateUtils;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProxyAccountRepositoryTest extends RepositoryTestHelper {


  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ProxyAccountRepository repository = ProxyAccountRepository.builder().build();

  @Test
  public void test_SaveProxyAccountWithLinkedAccount() {
    String accountId = UUID.randomUUID().toString();

    OriginalAccountEntity underlyingAccount = OriginalAccountEntity.builder()
        .accountId(accountId)
        .bank("bank")
        .accountHolder("hello")
        .currency("EUR")
        .balance(Amount.of("EUR", 0))
        .build();

    ProxyAccountEntity proxyAccountEntity = ProxyAccountEntity.builder()
        .proxyId(ProxyId.of("hello"))
        .proxyAccountId(accountId)
        .bankId("bank")
        .currency("EUR")
        .originalAccountEntity(underlyingAccount)
        .creationDate(DateUtils.now())
        .expiryDate(DateUtils.afterYears(1))
        .maximumAmountPerTransaction(Amount.of("EUR", 100))
        .build();

    repository.saveProxyAccountWithLinkedAccount(proxyAccountEntity, underlyingAccount);
  }

}
