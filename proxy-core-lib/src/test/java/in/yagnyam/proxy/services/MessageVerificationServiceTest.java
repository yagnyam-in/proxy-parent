package in.yagnyam.proxy.services;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.Security;

@RunWith(MockitoJUnitRunner.class)
public class MessageVerificationServiceTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }
}
