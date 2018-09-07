package in.yagnyam.proxy.server.banking.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import in.yagnyam.proxy.ProxyId;
import in.yagnyam.proxy.messages.banking.Amount;
import in.yagnyam.proxy.server.banking.model.AccountCredentialsEntity;
import in.yagnyam.proxy.server.banking.model.AccountCredentialsEntity.AccountCredentialsEntityBuilder;
import in.yagnyam.proxy.server.banking.model.OriginalAccountEntity;
import in.yagnyam.proxy.server.banking.model.ProxyAccountEntity;
import in.yagnyam.proxy.utils.DateUtils;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProxyAccountRepositoryTest extends RepositoryTestHelper {

  private ProxyAccountRepository repository = ProxyAccountRepository.builder().build();

  @Test
  public void testSaveProxyAccountWithLinkedAccount() {
    String accountId = UUID.randomUUID().toString();

    OriginalAccountEntity underlyingAccount = originalAccountEntity(accountId, null);

    ProxyAccountEntity proxyAccountEntity = proxyAccountEntity(accountId, underlyingAccount);

    repository.saveProxyAccountWithLinkedAccount(proxyAccountEntity, underlyingAccount);

    Optional<ProxyAccountEntity> result = repository.fetchProxyAccount(accountId);
    assertTrue(result.isPresent());
    assertNotNull(result.get().getOriginalAccountEntity());
    assertNull(result.get().getOriginalAccountEntity().getCredentialsEntity());
  }

  @Test
  public void testSaveProxyAccountWithLinkedAccountAndCredentials() {
    String accountId = UUID.randomUUID().toString();

    AccountCredentialsEntity credentialsEntity = AccountCredentialsEntity.accountCredentialsEntityBuilder()
        .credentialId(accountId)
        .build();

    OriginalAccountEntity underlyingAccount = originalAccountEntity(accountId, credentialsEntity);

    ProxyAccountEntity proxyAccountEntity = proxyAccountEntity(accountId, underlyingAccount);

    repository
        .saveProxyAccountWithLinkedAccountAndCredentials(proxyAccountEntity, underlyingAccount,
            credentialsEntity);

    Optional<ProxyAccountEntity> result = repository.fetchProxyAccount(accountId);
    assertTrue(result.isPresent());
    assertNotNull(result.get().getOriginalAccountEntity());
    assertNotNull(result.get().getOriginalAccountEntity().getCredentialsEntity());
  }

  private OriginalAccountEntity originalAccountEntity(String accountId,
      AccountCredentialsEntity credentialsEntity) {
    return OriginalAccountEntity.builder()
        .accountId(accountId)
        .accountNumber(accountId)
        .bank("bank")
        .accountHolder("hello")
        .currency("EUR")
        .balance(Amount.of("EUR", 0))
        .credentialsEntity(credentialsEntity)
        .build();
  }

  private ProxyAccountEntity proxyAccountEntity(String accountId,
      OriginalAccountEntity underlyingAccount) {
    return ProxyAccountEntity.builder()
        .proxyId(ProxyId.of("hello"))
        .proxyAccountId(accountId)
        .bankId("bank")
        .currency("EUR")
        .creationDate(DateUtils.now())
        .expiryDate(DateUtils.afterYears(1))
        .maximumAmountPerTransaction(Amount.of("EUR", 100))
        .originalAccountEntity(underlyingAccount)
        .build();
  }
}
