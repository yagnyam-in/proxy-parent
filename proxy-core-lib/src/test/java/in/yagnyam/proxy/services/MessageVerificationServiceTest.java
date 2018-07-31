package in.yagnyam.proxy.services;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageVerificationServiceTest {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Test
  public void test() {

  }
}
