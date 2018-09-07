package in.yagnyam.proxy.server.banking.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import in.yagnyam.proxy.Proxy;
import in.yagnyam.proxy.server.banking.model.BankConfigurationEntity;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BankConfigurationRepositoryTest extends RepositoryTestHelper {

  private BankConfigurationRepository repository = BankConfigurationRepository.builder().build();

  @Test
  public void test_FetchAllBankConfigurations_Empty() {
    List<BankConfigurationEntity> results = repository.fetchAllBankConfigurations();
    assertTrue(results.isEmpty());
  }

  @Test
  public void test_FetchAllBankConfigurations_Many() {
    List<BankConfigurationEntity> results = repository.fetchAllBankConfigurations();
    assertTrue(results.isEmpty());
    BankConfigurationEntity one = bankConfigurationEntity("one");
    BankConfigurationEntity two = bankConfigurationEntity("two");
    repository.saveBankConfiguration(one);
    repository.saveBankConfiguration(two);
    repository.saveBankConfiguration(one);
    repository.saveBankConfiguration(two);
    results = repository.fetchAllBankConfigurations();
    assertEquals(2, results.size());
    assertTrue(results.contains(one));
    assertTrue(results.contains(two));
  }


  @Test
  public void test_SaveBankConfiguration() {
    BankConfigurationEntity expected = bankConfigurationEntity("bank-id");
    repository.saveBankConfiguration(expected);
    Optional<BankConfigurationEntity> result = repository.getBankConfiguration("bank-id");
    assertTrue(result.isPresent());
    assertEquals(expected, result.get());
    assertEquals(1, repository.fetchAllBankConfigurations().size());
    assertEquals(expected, repository.fetchAllBankConfigurations().get(0));
  }

  private BankConfigurationEntity bankConfigurationEntity(String bankId) {
    BankConfigurationEntity entity = new BankConfigurationEntity();
    entity.setBankId(bankId);
    entity.setBankName("Bank");
    entity.setPrivateKeyId("pk");
    entity.setProxy(mock(Proxy.class));
    return entity;
  }

}
