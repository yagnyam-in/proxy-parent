package in.yagnyam.proxy.server.banking.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.messages.banking.wallet.ProxyWalletCreationRequest;
import in.yagnyam.proxy.server.BadRequestException;
import org.junit.Test;

public class ProxyAccountServiceTest {

  @Test(expected = BadRequestException.class)
  public void testAssertValidRequest() {
    ProxyWalletCreationRequest request = mock(ProxyWalletCreationRequest.class);
    when(request.getCurrency()).thenReturn("BTC");
    ProxyAccountService.assertValidRequest(request);
  }
}
