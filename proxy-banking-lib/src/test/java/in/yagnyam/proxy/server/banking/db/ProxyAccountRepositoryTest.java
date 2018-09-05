package in.yagnyam.proxy.server.banking.db;

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
  public void test() {

  }

}
