package in.yagnyam.proxy.messages.banking;

import in.yagnyam.proxy.services.MessageSerializerService;
import java.io.IOException;
import org.junit.Test;

public class SignedProxyAccountsTest {

  @Test
  public void testSerialization() throws IOException {
    SignedProxyAccounts accounts = SignedProxyAccounts.builder().build();
    MessageSerializerService serializerService = MessageSerializerService.builder().build();
    String json = serializerService.serializeMessage(accounts);
    serializerService.deserializeMessage(json, SignedProxyAccounts.class);
  }
}
