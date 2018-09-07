package in.yagnyam.proxy.server.banking.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.yagnyam.proxy.messages.banking.ProxyAccountCreationRequest;
import in.yagnyam.proxy.server.BadRequestException;
import org.junit.Test;

public class ProxyAccountServiceTest {

  @Test(expected = BadRequestException.class)
  public void testAssertValidRequest() {
    ProxyAccountCreationRequest request = mock(ProxyAccountCreationRequest.class);
    when(request.getCurrency()).thenReturn("BTC");
    ProxyAccountService.assertValidRequest(request);
  }
}
